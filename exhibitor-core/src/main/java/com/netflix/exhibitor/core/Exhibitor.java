package com.netflix.exhibitor.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.ActivityQueue;
import com.netflix.exhibitor.core.backup.BackupManager;
import com.netflix.exhibitor.core.config.ConfigListener;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.index.IndexCache;
import com.netflix.exhibitor.core.state.CleanupManager;
import com.netflix.exhibitor.core.state.ControlPanelTypes;
import com.netflix.exhibitor.core.state.InstanceConfig;
import com.netflix.exhibitor.core.state.InstanceStateManager;
import com.netflix.exhibitor.core.state.MonitorRunningInstance;
import com.netflix.exhibitor.core.state.ProcessOperations;
import com.netflix.exhibitor.core.state.StandardProcessOperations;
import com.netflix.exhibitor.rest.UITab;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 *     The main application - this class serves as a container for the various
 *     objects needed by the application and as a lifecycle maintainer
 * </p>
 *
 * <p>
 *     Most users of the Exhibitor system will not need direct access to this class
 * </p>
 */
public class Exhibitor implements Closeable
{
    private final ActivityLog               log;
    private final ActivityQueue             activityQueue = new ActivityQueue();
    private final MonitorRunningInstance    monitorRunningInstance;
    private final InstanceStateManager      instanceStateManager;
    private final AtomicReference<InstanceConfig> instanceConfig = new AtomicReference<InstanceConfig>();
    private final Collection<UITab>         additionalUITabs;
    private final ProcessOperations         processOperations;
    private final CleanupManager            cleanupManager;
    private final AtomicReference<State>    state = new AtomicReference<State>(State.LATENT);
    private final ConfigProvider            configProvider;
    private final int                       connectionTimeOutMs;
    private final IndexCache                indexCache;
    private final Map<ControlPanelTypes, AtomicBoolean> controlPanelSettings;
    private final BackupManager             backupManager;
    private final Set<ConfigListener>       configListeners = Sets.newSetFromMap(Maps.<ConfigListener, Boolean>newConcurrentMap());

    private CuratorFramework    localConnection;    // protected by synchronization

    private enum State
    {
        LATENT,
        STARTED,
        STOPPED
    }

    /**
     * @param configProvider config source
     * @param additionalUITabs any additional tabs in the UI (can be null)
     * @param backupProvider backup provider or null
     * @throws IOException errors
     */
    public Exhibitor(ConfigProvider configProvider, Collection<UITab> additionalUITabs, BackupProvider backupProvider) throws Exception
    {
        this(configProvider, additionalUITabs, backupProvider, (int)TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS), 1000);
    }

    /**
     * @param configProvider config source
     * @param additionalUITabs any additional tabs in the UI (can be null)
     * @param backupProvider backup provider or null
     * @param connectionTimeOutMs general timeout for ZK connections
     * @param logWindowSizeLines max lines to keep in memory
     * @throws IOException errors
     */
    public Exhibitor(ConfigProvider configProvider, Collection<UITab> additionalUITabs, BackupProvider backupProvider, int connectionTimeOutMs, int logWindowSizeLines) throws Exception
    {
        log = new ActivityLog(logWindowSizeLines);
        this.configProvider = configProvider;
        this.connectionTimeOutMs = connectionTimeOutMs;
        this.instanceConfig.set(configProvider.loadConfig());
        this.additionalUITabs = (additionalUITabs != null) ? ImmutableList.copyOf(additionalUITabs) : ImmutableList.<UITab>of();
        this.processOperations = new StandardProcessOperations(this);
        instanceStateManager = new InstanceStateManager(this);
        monitorRunningInstance = new MonitorRunningInstance(this);
        cleanupManager = new CleanupManager(this);
        indexCache = new IndexCache(log);

        ImmutableMap.Builder<ControlPanelTypes, AtomicBoolean> builder = ImmutableMap.builder();
        for ( ControlPanelTypes type : ControlPanelTypes.values() )
        {
            builder.put(type, new AtomicBoolean(true));
        }
        controlPanelSettings = builder.build();

        this.backupManager = new BackupManager(this, backupProvider);
    }

    /**
     * @return logging manager
     */
    public ActivityLog getLog()
    {
        return log;
    }

    /**
     * @return cache of indexed log files
     */
    public IndexCache getIndexCache()
    {
        return indexCache;
    }

    /**
     * Start the app
     */
    public void start()
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTED));

        activityQueue.start();
        monitorRunningInstance.start();
        cleanupManager.start();
        backupManager.start();
    }

    @Override
    public void close() throws IOException
    {
        Preconditions.checkState(state.compareAndSet(State.STARTED, State.STOPPED));

        Closeables.closeQuietly(indexCache);
        Closeables.closeQuietly(backupManager);
        Closeables.closeQuietly(cleanupManager);
        Closeables.closeQuietly(monitorRunningInstance);
        Closeables.closeQuietly(activityQueue);
        closeLocalConnection();
    }

    /**
     * @return any additionally configured tabs
     */
    public Collection<UITab> getAdditionalUITabs()
    {
        return additionalUITabs;
    }

    /**
     * @return the config instance
     */
    public InstanceConfig getConfig()
    {
        return instanceConfig.get();
    }

    /**
     * Set new new config
     *
     * @param newConfig the new config
     * @throws Exception I/O errors
     */
    public synchronized void updateConfig(InstanceConfig newConfig) throws Exception
    {
        closeLocalConnection();
        instanceConfig.set(newConfig);
        configProvider.storeConfig(newConfig);

        for ( ConfigListener listener : configListeners )
        {
            listener.configUpdated();
        }
    }

    public InstanceStateManager getInstanceStateManager()
    {
        return instanceStateManager;
    }

    public ActivityQueue getActivityQueue()
    {
        return activityQueue;
    }

    public ProcessOperations getProcessOperations()
    {
        return processOperations;
    }

    public int getConnectionTimeOutMs()
    {
        return connectionTimeOutMs;
    }

    public synchronized void resetLocalConnection() throws IOException
    {
        closeLocalConnection();
    }

    public synchronized CuratorFramework getLocalConnection() throws IOException
    {
        if ( localConnection == null )
        {
            localConnection = CuratorFrameworkFactory.newClient
            (
                "localhost:" + instanceConfig.get().getInt(IntConfigs.CLIENT_PORT),
                connectionTimeOutMs * 10,
                connectionTimeOutMs,
                new ExponentialBackoffRetry(10, 3)
            );
            localConnection.start();
        }
        return localConnection;
    }
    
    public boolean isControlPanelSettingEnabled(ControlPanelTypes type)
    {
        return controlPanelSettings.get(type).get();
    }

    public void setControlPanelSettingEnabled(ControlPanelTypes type, boolean newValue)
    {
        controlPanelSettings.get(type).set(newValue);
    }

    public CleanupManager getCleanupManager()
    {
        return cleanupManager;
    }

    public BackupManager getBackupManager()
    {
        return backupManager;
    }
    
    public void addConfigListener(ConfigListener listener)
    {
        configListeners.add(listener);
    }

    private synchronized void closeLocalConnection()
    {
        Closeables.closeQuietly(localConnection);
        localConnection = null;
    }

}
