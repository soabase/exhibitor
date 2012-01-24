package com.netflix.exhibitor;

import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.ActivityQueue;
import com.netflix.exhibitor.maintenance.BackupManager;
import com.netflix.exhibitor.maintenance.CleanupManager;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.spi.GlobalSharedConfig;
import com.netflix.exhibitor.spi.ProcessOperations;
import com.netflix.exhibitor.state.InstanceStateManager;
import com.netflix.exhibitor.state.MonitorRunningInstance;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Exhibitor implements Closeable
{
    private final ActivityLog               log = new ActivityLog();
    private final ActivityQueue             activityQueue = new ActivityQueue();
    private final MonitorRunningInstance    monitorRunningInstance;
    private final InstanceStateManager      instanceStateManager;
    private final InstanceConfig            instanceConfig;
    private final GlobalSharedConfig        globalSharedConfig;
    private final ProcessOperations         processOperations;
    private final CleanupManager            cleanupManager;
    private final BackupManager             backupManager;
    private final AtomicBoolean             restartsEnabled = new AtomicBoolean(true);

    private CuratorFramework    localConnection;    // protected by synchronization

    public Exhibitor(InstanceConfig instanceConfig, GlobalSharedConfig globalSharedConfig, ProcessOperations processOperations, BackupSource backupSource)
    {
        this.instanceConfig = instanceConfig;
        this.globalSharedConfig = globalSharedConfig;
        this.processOperations = processOperations;
        instanceStateManager = new InstanceStateManager(this);
        monitorRunningInstance = new MonitorRunningInstance(this);
        cleanupManager = new CleanupManager(this);
        backupManager = new BackupManager(this, backupSource);
    }

    public ActivityLog getLog()
    {
        return log;
    }

    public void start()
    {
        activityQueue.start();
        instanceStateManager.start();
        monitorRunningInstance.start();
        cleanupManager.start();
        backupManager.start();
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

    public GlobalSharedConfig getGlobalSharedConfig()
    {
        return globalSharedConfig;
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

    public BackupManager getBackupManager()
    {
        return backupManager;
    }

    @Override
    public void close() throws IOException
    {
        Closeables.closeQuietly(backupManager);
        Closeables.closeQuietly(cleanupManager);
        Closeables.closeQuietly(monitorRunningInstance);
        Closeables.closeQuietly(instanceStateManager);
        Closeables.closeQuietly(activityQueue);
        closeLocalConnection();
    }

    private synchronized void closeLocalConnection()
    {
        Closeables.closeQuietly(localConnection);
        localConnection = null;
    }
}
