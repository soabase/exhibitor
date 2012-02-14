package com.netflix.exhibitor.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.ActivityQueue;
import com.netflix.exhibitor.core.index.IndexCache;
import com.netflix.exhibitor.core.maintenance.CleanupManager;
import com.netflix.exhibitor.core.state.ControlPanelTypes;
import com.netflix.exhibitor.core.state.InstanceStateManager;
import com.netflix.exhibitor.core.state.MonitorRunningInstance;
import com.netflix.exhibitor.core.state.ProcessOperations;
import com.netflix.exhibitor.core.state.StandardProcessOperations;
import com.netflix.exhibitor.rest.UITab;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
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
    private final ActivityLog               log = new ActivityLog();
    private final ActivityQueue             activityQueue = new ActivityQueue();
    private final MonitorRunningInstance monitorRunningInstance;
    private final InstanceStateManager      instanceStateManager;
    private final AtomicReference<InstanceConfig> instanceConfig = new AtomicReference<InstanceConfig>();
    private final Collection<UITab>         additionalUITabs;
    private final ProcessOperations         processOperations;
    private final CleanupManager            cleanupManager;
    private final AtomicReference<State>    state = new AtomicReference<State>(State.LATENT);
    private final ConfigProvider            configProvider;
    private final IndexCache                indexCache;
    private final Map<ControlPanelTypes, AtomicBoolean> controlPanelSettings;

    private CuratorFramework    localConnection;    // protected by synchronization

    private enum State
    {
        LATENT,
        STARTED,
        STOPPED
    }

    /**
     *
     * @param configProvider config source
     * @param additionalUITabs any additional tabs in the UI (can be null)
     * @throws IOException errors
     */
    public Exhibitor(ConfigProvider configProvider, Collection<UITab> additionalUITabs) throws Exception
    {
        this.configProvider = configProvider;
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
    }

    public ActivityLog getLog()
    {
        return log;
    }

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
        instanceStateManager.start();
        monitorRunningInstance.start();
        cleanupManager.start();
    }

    @Override
    public void close() throws IOException
    {
        Preconditions.checkState(state.compareAndSet(State.STARTED, State.STOPPED));

        Closeables.closeQuietly(indexCache);
        Closeables.closeQuietly(cleanupManager);
        Closeables.closeQuietly(monitorRunningInstance);
        Closeables.closeQuietly(instanceStateManager);
        Closeables.closeQuietly(activityQueue);
        closeLocalConnection();
    }

    public Collection<UITab> getAdditionalUITabs()
    {
        return additionalUITabs;
    }

    public InstanceConfig getConfig()
    {
        return instanceConfig.get();
    }

    public synchronized void updateConfig(InstanceConfig newConfig) throws Exception
    {
        closeLocalConnection();
        instanceConfig.set(newConfig);
        configProvider.storeConfig(newConfig);
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

    public synchronized CuratorFramework getLocalConnection() throws IOException
    {
        if ( localConnection == null )
        {
            localConnection = CuratorFrameworkFactory.newClient
            (
                "localhost:" + instanceConfig.get().getClientPort(),
                instanceConfig.get().getConnectionTimeoutMs() * 10,
                instanceConfig.get().getConnectionTimeoutMs(),
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

    private synchronized void closeLocalConnection()
    {
        Closeables.closeQuietly(localConnection);
        localConnection = null;
    }

}
