package com.netflix.exhibitor;

import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.ActivityQueue;
import com.netflix.exhibitor.state.InstanceStateManager;
import com.netflix.exhibitor.state.MonitorRunningInstance;
import java.io.Closeable;
import java.io.IOException;

public class Exhibitor implements Closeable
{
    private final ActivityLog               log = new ActivityLog();
    private final ActivityQueue             activityQueue = new ActivityQueue();
    private final MonitorRunningInstance    monitorRunningInstance;
    private final InstanceStateManager      instanceStateManager;
    private final ExhibitorConfig           exhibitorConfig;
    private final ProcessOperations         processOperations;

    private CuratorFramework    localConnection;    // protected by synchronization

    public Exhibitor(ExhibitorConfig exhibitorConfig, ProcessOperations processOperations)
    {
        this.exhibitorConfig = exhibitorConfig;
        this.processOperations = processOperations;
        instanceStateManager = new InstanceStateManager(this);
        monitorRunningInstance = new MonitorRunningInstance(this);
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
    }

    public ExhibitorConfig getConfig()
    {
        return exhibitorConfig;
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
            localConnection = CuratorFrameworkFactory.newClient("localhost:" + exhibitorConfig.getClientPort(), 30000, 5000, new ExponentialBackoffRetry(10, 3));
            localConnection.start();
        }
        return localConnection;
    }

    @Override
    public void close() throws IOException
    {
        closeLocalConnection();
        Closeables.closeQuietly(monitorRunningInstance);
        Closeables.closeQuietly(instanceStateManager);
        Closeables.closeQuietly(activityQueue);
    }

    private synchronized void closeLocalConnection()
    {
        Closeables.closeQuietly(localConnection);
        localConnection = null;
    }
}
