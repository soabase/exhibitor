package com.netflix.exhibitor.backup;

import com.google.common.io.Closeables;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.spi.BackupSpec;
import org.apache.zookeeper.KeeperException;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class RestoreProcessor
{
    private final CuratorFramework client;
    private final ActivityLog log;
    private final BackupSource backupSource;
    private final InstanceConfig config;

    public RestoreProcessor(CuratorFramework client, ActivityLog log, BackupSource backupSource, InstanceConfig config)
    {
        this.client = client;
        this.log = log;
        this.backupSource = backupSource;
        this.config = config;
    }

    public void restoreFromBackup(BackupSpec spec) throws Exception
    {
        boolean             hasRestorations = false;
        InputStream         in = null;
        try
        {
            in = backupSource.openRestoreStream(config, spec);

            DataInputStream wrapped = new DataInputStream(new GZIPInputStream(new BufferedInputStream(in)));
            in = wrapped;

            String      version = wrapped.readUTF();
            if ( !version.equals(BackupProcessor.FILE_VERSION) )
            {
                throw new IOException("Bad data version: " + version);
            }
            wrapped.readUTF();  // date
            wrapped.readUTF();  // top path

            for(;;)
            {
                String  thisPath = wrapped.readUTF();
                if ( thisPath.equals(BackupProcessor.EOF_MARKER) )
                {
                    break;
                }
                int     length = wrapped.readInt();
                byte[]  data = new byte[length];
                wrapped.readFully(data);

                restoreWithRetry(client, thisPath, data);
                log.add(ActivityLog.Type.INFO, "Restored: " + thisPath);
                hasRestorations = true;
            }
        }
        catch ( Exception e )
        {
            String  message = "Could not complete restore of: " + spec;
            if ( hasRestorations )
            {
                message += " - IMPORTATION: Some paths have been restored already. Check the log for details.";
            }
            log.add(ActivityLog.Type.ERROR, message, e);
        }
        finally
        {
            Closeables.closeQuietly(in);
        }
    }

    private void restoreWithRetry(CuratorFramework client, String thisPath, byte[] data) throws Exception
    {
        long        start = System.currentTimeMillis();
        int         retryCount = 0;
        RetryPolicy retryPolicy = client.getZookeeperClient().getRetryPolicy();
        for(;;)
        {
            try
            {
                client.setData().forPath(thisPath, data);
                break;
            }
            catch ( KeeperException.NoNodeException dummy )
            {
                try
                {
                    client.create().creatingParentsIfNeeded().forPath(thisPath, data);
                }
                catch ( KeeperException.NodeExistsException e )
                {
                    if ( !retryPolicy.allowRetry(retryCount++, System.currentTimeMillis() - start) )
                    {
                        throw e;
                    }
                }
            }
        }
    }
}
