package com.netflix.exhibitor.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.ActivityQueue;
import com.netflix.exhibitor.core.backup.BackupManager;
import com.netflix.exhibitor.core.backup.BackupProvider;
import com.netflix.exhibitor.core.cluster.ClusterStatus;
import com.netflix.exhibitor.core.config.ConfigListener;
import com.netflix.exhibitor.core.config.ConfigManager;
import com.netflix.exhibitor.core.config.ConfigProvider;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.controlpanel.ControlPanelValues;
import com.netflix.exhibitor.core.index.IndexCache;
import com.netflix.exhibitor.core.state.CleanupManager;
import com.netflix.exhibitor.core.state.InstanceStateManager;
import com.netflix.exhibitor.core.state.MonitorRunningInstance;
import com.netflix.exhibitor.core.state.ProcessOperations;
import com.netflix.exhibitor.core.state.StandardProcessOperations;
import com.netflix.exhibitor.rest.UITab;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
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
    private final Collection<UITab>         additionalUITabs;
    private final ProcessOperations         processOperations;
    private final CleanupManager            cleanupManager;
    private final AtomicReference<State>    state = new AtomicReference<State>(State.LATENT);
    private final IndexCache                indexCache;
    private final ControlPanelValues        controlPanelValues;
    private final BackupManager             backupManager;
    private final ConfigManager             configManager;
    private final Arguments                 arguments;
    private final ClusterStatus             clusterStatus;

    private CuratorFramework    localConnection;    // protected by synchronization

    private enum State
    {
        LATENT,
        STARTED,
        STOPPED
    }

    public static class Arguments
    {
        private final int       connectionTimeOutMs;
        private final int       logWindowSizeLines;
        private final int       configCheckMs;
        private final String    thisJVMHostname;

        public Arguments(int connectionTimeOutMs, int logWindowSizeLines, String thisJVMHostname, int configCheckMs)
        {
            this.connectionTimeOutMs = connectionTimeOutMs;
            this.logWindowSizeLines = logWindowSizeLines;
            this.thisJVMHostname = thisJVMHostname;
            this.configCheckMs = configCheckMs;
        }
    }

    /**
     * Return this VM's hostname if possible
     *
     * @return hostname
     */
    public static String getHostname()
    {
        String      host = "unknown";
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch ( UnknownHostException e )
        {
            // ignore
        }
        return host;
    }

    /**
     * @param configProvider config source
     * @param additionalUITabs any additional tabs in the UI (can be null)
     * @param backupProvider backup provider or null
     * @param arguments startup arguments
     * @throws IOException errors
     */
    public Exhibitor(ConfigProvider configProvider, Collection<UITab> additionalUITabs, BackupProvider backupProvider, Arguments arguments) throws Exception
    {
        this.arguments = arguments;
        log = new ActivityLog(arguments.logWindowSizeLines);
        this.configManager = new ConfigManager(this, configProvider, arguments.configCheckMs);
        this.additionalUITabs = (additionalUITabs != null) ? ImmutableList.copyOf(additionalUITabs) : ImmutableList.<UITab>of();
        this.processOperations = new StandardProcessOperations(this);
        instanceStateManager = new InstanceStateManager(this);
        monitorRunningInstance = new MonitorRunningInstance(this);
        cleanupManager = new CleanupManager(this);
        indexCache = new IndexCache(log);
        clusterStatus = new ClusterStatus(this);

        controlPanelValues = new ControlPanelValues();

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
     *
     * @throws Exception errors
     */
    public void start() throws Exception
    {
        Preconditions.checkState(state.compareAndSet(State.LATENT, State.STARTED));

        activityQueue.start();
        configManager.start();
        monitorRunningInstance.start();
        cleanupManager.start();
        clusterStatus.start();
        backupManager.start();

        configManager.addConfigListener
        (
            new ConfigListener()
            {
                @Override
                public void configUpdated()
                {
                    try
                    {
                        resetLocalConnection();
                    }
                    catch ( IOException e )
                    {
                        log.add(ActivityLog.Type.ERROR, "Resetting connection", e);
                    }
                }
            }
        );
    }

    @Override
    public void close() throws IOException
    {
        Preconditions.checkState(state.compareAndSet(State.STARTED, State.STOPPED));

        Closeables.closeQuietly(indexCache);
        Closeables.closeQuietly(backupManager);
        Closeables.closeQuietly(clusterStatus);
        Closeables.closeQuietly(cleanupManager);
        Closeables.closeQuietly(monitorRunningInstance);
        Closeables.closeQuietly(configManager);
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

    public ClusterStatus getClusterStatus()
    {
        return clusterStatus;
    }

    public ConfigManager getConfigManager()
    {
        return configManager;
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

    /**
     * Return the configured ZK connection timeout in ms
     *
     * @return timeout
     */
    public int getConnectionTimeOutMs()
    {
        return arguments.connectionTimeOutMs;
    }
    
    public String getThisJVMHostname()
    {
        return arguments.thisJVMHostname;
    }

    /**
     * Closes/resets the ZK connection or does nothing if it hasn't been opened yet
     *
     * @throws IOException errors
     */
    public synchronized void resetLocalConnection() throws IOException
    {
        closeLocalConnection();
    }

    /**
     * Return a connection ot the ZK instance (creating it if needed)
     *
     * @return connection
     * @throws IOException errors
     */
    public synchronized CuratorFramework getLocalConnection() throws IOException
    {
        if ( localConnection == null )
        {
            localConnection = CuratorFrameworkFactory.newClient
            (
                "localhost:" + configManager.getConfig().getInt(IntConfigs.CLIENT_PORT),
                arguments.connectionTimeOutMs * 10,
                arguments.connectionTimeOutMs,
                new ExponentialBackoffRetry(10, 3)
            );
            localConnection.start();
        }
        return localConnection;
    }

    public ControlPanelValues getControlPanelValues()
    {
        return controlPanelValues;
    }

    public BackupManager getBackupManager()
    {
        return backupManager;
    }

    private synchronized void closeLocalConnection()
    {
        Closeables.closeQuietly(localConnection);
        localConnection = null;
    }

}
