package com.netflix.exhibitor;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.ActivityQueue;
import com.netflix.exhibitor.maintenance.CleanupManager;
import com.netflix.exhibitor.state.InstanceStateManager;
import com.netflix.exhibitor.state.MonitorRunningInstance;
import com.netflix.exhibitor.state.ProcessOperations;
import com.netflix.exhibitor.state.StandardProcessOperations;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
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
    private final MonitorRunningInstance    monitorRunningInstance;
    private final InstanceStateManager      instanceStateManager;
    private final AtomicReference<InstanceConfig> instanceConfig = new AtomicReference<InstanceConfig>();
    private final Collection<UITab>         additionalUITabs;
    private final ProcessOperations         processOperations;
    private final CleanupManager            cleanupManager;
    private final AtomicBoolean             restartsEnabled = new AtomicBoolean(true);
    private final AtomicReference<State>    state = new AtomicReference<State>(State.LATENT);
    private final ConfigProvider            configProvider;

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
     * @param zooKeeperDirectory path to the ZooKeeper distribution
     * @param dataDirectory path to where your ZooKeeper data is stored
     * @throws IOException errors
     */
    public Exhibitor(ConfigProvider configProvider, Collection<UITab> additionalUITabs, String zooKeeperDirectory, String dataDirectory) throws Exception
    {
        this.configProvider = configProvider;
        this.instanceConfig.set(configProvider.loadConfig());
        this.additionalUITabs = (additionalUITabs != null) ? ImmutableList.copyOf(additionalUITabs) : ImmutableList.<UITab>of();
        this.processOperations = new StandardProcessOperations(this, zooKeeperDirectory, dataDirectory);
        instanceStateManager = new InstanceStateManager(this);
        monitorRunningInstance = new MonitorRunningInstance(this);
        cleanupManager = new CleanupManager(this);
    }

    public ActivityLog getLog()
    {
        return log;
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

    public boolean restartsAreEnabled()
    {
        return restartsEnabled.get();
    }
    
    public void         setRestartsEnabled(boolean newValue)
    {
        restartsEnabled.set(newValue);
    }

    private synchronized void closeLocalConnection()
    {
        Closeables.closeQuietly(localConnection);
        localConnection = null;
    }

}
