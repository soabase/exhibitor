package com.netflix.exhibitor;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.ActivityQueue;
import com.netflix.exhibitor.state.StandardProcessOperations;
import com.netflix.exhibitor.maintenance.CleanupManager;
import com.netflix.exhibitor.state.ProcessOperations;
import com.netflix.exhibitor.state.InstanceStateManager;
import com.netflix.exhibitor.state.MonitorRunningInstance;
import java.io.Closeable;
import java.io.IOException;
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
    private final InstanceConfig            instanceConfig;
    private final ProcessOperations         processOperations;
    private final CleanupManager            cleanupManager;
    private final AtomicBoolean             restartsEnabled = new AtomicBoolean(true);
    private final AtomicBoolean             backupCleanupEnabled = new AtomicBoolean(true);
    private final AtomicReference<State>    state = new AtomicReference<State>(State.LATENT);

    private CuratorFramework    localConnection;    // protected by synchronization

    private enum State
    {
        LATENT,
        STARTED,
        STOPPED
    }

    /**
     * @param instanceConfig static config for this instance
     *
     * @param zooKeeperDirectory path to the ZooKeeper distribution
     * @param dataDirectory path to where your ZooKeeper data is stored
     * @throws IOException errors
     */
    public Exhibitor(InstanceConfig instanceConfig, String zooKeeperDirectory, String dataDirectory) throws IOException
    {
        this.instanceConfig = instanceConfig;
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

    public InstanceConfig getConfig()
    {
        return instanceConfig;
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
            localConnection = CuratorFrameworkFactory.newClient("localhost:" + instanceConfig.getClientPort(), 30000, 5000, new ExponentialBackoffRetry(10, 3));
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

    public boolean backupCleanupEnabled()
    {
        return backupCleanupEnabled.get();
    }

    public void         setBackupCleanupEnabled(boolean newValue)
    {
        backupCleanupEnabled.set(newValue);
    }

    private synchronized void closeLocalConnection()
    {
        Closeables.closeQuietly(localConnection);
        localConnection = null;
    }
}
