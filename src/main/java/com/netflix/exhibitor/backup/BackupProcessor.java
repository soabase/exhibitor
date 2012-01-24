package com.netflix.exhibitor.backup;

import com.google.common.io.Closeables;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.utils.ZKPaths;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.spi.GlobalSharedConfig;
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
    private final GlobalSharedConfig sharedConfig;
    private final CuratorFramework client;
    private final ActivityLog log;
    private final BackupSource backupSource;
    private final InstanceConfig config;

    static final String     FILE_VERSION = "1.0";
    static final String     EOF_MARKER = "!";   // an illegal path

    private static final String RECURSIVE_FLAG = "*";

    public BackupProcessor(GlobalSharedConfig sharedConfig, CuratorFramework client, ActivityLog log, BackupSource backupSource, InstanceConfig config)
    {
        this.sharedConfig = sharedConfig;
        this.client = client;
        this.log = log;
        this.backupSource = backupSource;
        this.config = config;
    }

    public void execute() throws Exception
    {
        if ( sharedConfig.getBackupPaths() != null )
        {
            for ( String path : sharedConfig.getBackupPaths() )
            {
                backupPath(path);
            }
        }
    }

    private void backupPath(String path) throws Exception
    {
        String      originalPath = path;
        boolean     isRecursive = path.endsWith(RECURSIVE_FLAG);
        if ( isRecursive )
        {
            path = trimEnding(path, RECURSIVE_FLAG);
            if ( path == null )
            {
                return;
            }
        }
        if ( path.endsWith("/") )
        {
            path = trimEnding(path, "/");
            if ( path == null )
            {
                return;
            }
        }
        
        try
        {
            PathUtils.validatePath(path);
        }
        catch ( IllegalArgumentException e )
        {
            log.add(ActivityLog.Type.ERROR, "Bad path in backups: " + path, e);
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

                out.close();
                out = null;

                in = new BufferedInputStream(new FileInputStream(tempFile));
                backupSource.backup(config, path, in);
            }
            catch ( Exception e )
            {
                log.add(ActivityLog.Type.ERROR, "Could not backup path: " + originalPath, e);
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

    private String trimEnding(String path, String ending)
    {
        int     endIndex = path.length() - ending.length();
        if ( endIndex < 1 )
        {
            log.add(ActivityLog.Type.ERROR, "Bad path in backups: " + path);
            return null;
        }
        path = path.substring(0, endIndex);
        return path;
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
