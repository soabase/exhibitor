package com.netflix.exhibitor.core.index;

import com.google.common.io.Closeables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.StringConfigs;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class IndexerUtil
{
    public static void      startIndexing(Exhibitor exhibitor, File path) throws Exception
    {

    }

    public static void      startIndexing(Exhibitor exhibitor, File path, IndexActivity.CompletionListener listener) throws Exception
    {
        InputStream in = null;
        try
        {
            if ( path.isDirectory() )
            {
                DirectoryInputStream        directoryInputStream = new DirectoryInputStream(path);
                in = directoryInputStream;
                startIndexing(exhibitor, in, path.getName(), directoryInputStream.length(), listener);
            }
            else
            {
                in = new BufferedInputStream(new FileInputStream(path));
                startIndexing(exhibitor, in, path.getName(), path.length(), listener);
            }
        }
        finally
        {
            Closeables.closeQuietly(in);
        }
    }

    private static void      startIndexing(Exhibitor exhibitor, InputStream in, String name, long length, IndexActivity.CompletionListener listener) throws Exception
    {
        InstanceConfig  config = exhibitor.getConfigManager().getConfig();

        File indexDirectory = new File(config.getString(StringConfigs.LOG_INDEX_DIRECTORY), "exhibitor-" + System.currentTimeMillis());

        LogIndexer      logIndexer;
        try
        {
            logIndexer = new LogIndexer(in, name, length, indexDirectory);
        }
        catch ( Exception e )
        {
            Closeables.closeQuietly(in);
            if ( listener != null )
            {
                listener.completed();
            }
            throw e;
        }
        if ( logIndexer.isValid() )
        {
            IndexActivity   activity = new IndexActivity(logIndexer, exhibitor.getLog(), in, listener);
            exhibitor.getActivityQueue().add(QueueGroups.MAIN, activity);
        }
        else if ( listener != null )
        {
            listener.completed();
        }
    }
    
    private IndexerUtil()
    {
    }
}
