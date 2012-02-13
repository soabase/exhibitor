package com.netflix.exhibitor.core.index;

import com.netflix.curator.framework.CuratorFramework;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import org.apache.zookeeper.KeeperException;

public class RestoreActivity implements Activity
{
    private final Exhibitor exhibitor;
    private final EntryTypes type;
    private final String path;
    private final byte[] data;

    public RestoreActivity(Exhibitor exhibitor, EntryTypes type, String path, byte[] data)
    {
        this.exhibitor = exhibitor;
        this.type = type;
        this.path = path;
        this.data = data;
    }

    @Override
    public void completed(boolean wasSuccessful)
    {
    }

    @Override
    public Boolean call() throws Exception
    {
        exhibitor.getLog().add(ActivityLog.Type.INFO, "Recovering path " + path);

        CuratorFramework        client = exhibitor.getLocalConnection();
        switch ( type )
        {
            case CREATE_PERSISTENT:
            case CREATE_EPHEMERAL:
            {
                try
                {
                    client.create().forPath(path, data);
                }
                catch ( KeeperException.NodeExistsException dummy )
                {
                    client.setData().forPath(path, data);
                }
                break;
            }

            case DELETE:
            {
                client.delete().forPath(path);
                break;
            }

            case SET_DATA:
            {
                try
                {
                    client.setData().forPath(path, data);
                }
                catch ( KeeperException.NoNodeException dummy )
                {
                    client.create().forPath(path, data);
                }
                break;
            }

            default:
            {
                // NOP
                break;
            }
        }

        exhibitor.getLog().add(ActivityLog.Type.INFO, "Completed recovering path " + path);
        return true;
    }
}
