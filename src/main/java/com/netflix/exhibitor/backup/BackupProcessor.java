package com.netflix.exhibitor.backup;

import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.utils.ZKPaths;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.spi.BackupSource;
import org.apache.zookeeper.common.PathUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class BackupProcessor
{
    private final Exhibitor exhibitor;
    
    static final String     FILE_VERSION = "1.0";
    static final String     EOF_MARKER = "!";   // an illegal path

    private static final String RECURSIVE_FLAG = "*";

    public BackupProcessor(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
    }

    public void execute() throws Exception
    {
        if ( exhibitor.getConfig().getBackupPaths() != null )
        {
            CuratorFramework    client = exhibitor.getLocalConnection();
            for ( String path : exhibitor.getConfig().getBackupPaths() )
            {
                backupPath(client, path);
            }
        }
    }

    private void backupPath(CuratorFramework client, String path) throws Exception
    {
        String      originalPath = path;
        boolean     isRecursive = path.endsWith(RECURSIVE_FLAG);
        if ( isRecursive )
        {
            int     endIndex = path.length() - RECURSIVE_FLAG.length();
            if ( endIndex < 1 )
            {
                exhibitor.getLog().add(ActivityLog.Type.ERROR, "Bad path in backups: " + path);
                return;
            }
            path = path.substring(0, endIndex);
        }
        
        try
        {
            PathUtils.validatePath(path);
        }
        catch ( IllegalArgumentException e )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Bad path in backups: " + path, e);
            return;
        }

        File                tempFile = File.createTempFile("exhibitor", ".tmp");
        DataOutputStream    out = null;
        InputStream         in = null;
        try
        {
            out = new DataOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile))));

            try
            {
                out.writeUTF(FILE_VERSION);
                out.writeUTF(new Date().toString());
                out.writeUTF(originalPath);

                backupOnePath(client, path, out, isRecursive);
                out.writeUTF(EOF_MARKER);

                Closeables.closeQuietly(out);
                out = null;

                BackupSource backupSource = exhibitor.getBackupManager().getSource();

                in = new BufferedInputStream(new FileInputStream(tempFile));
                backupSource.backup(exhibitor.getConfig(), path, in);
            }
            catch ( Exception e )
            {
                exhibitor.getLog().add(ActivityLog.Type.ERROR, "Could not backup path: " + originalPath, e);
            }
        }
        finally
        {
            Closeables.closeQuietly(out);
            Closeables.closeQuietly(in);
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    private void backupOnePath(CuratorFramework client, String path, DataOutputStream out, boolean recursive) throws Exception
    {
        byte[]      data = client.getData().forPath(path);
        out.writeUTF(path);
        out.writeInt(data.length);
        out.write(data);
        
        if ( recursive )
        {
            List<String>    children = client.getChildren().forPath(path);
            for ( String name : children )
            {
                backupOnePath(client, ZKPaths.makePath(path, name), out, true);
            }
        }
    }
}
