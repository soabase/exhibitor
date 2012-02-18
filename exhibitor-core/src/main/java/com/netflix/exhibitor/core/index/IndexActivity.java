package com.netflix.exhibitor.core.index;

import com.google.common.io.Closeables;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IndexActivity implements Activity
{
    private final LogIndexer indexer;
    private final ActivityLog log;
    private final InputStream in;
    private final CompletionListener completionListener;

    public interface CompletionListener
    {
        public void completed();
    }

    public IndexActivity(LogIndexer indexer, ActivityLog log, InputStream in)
    {
        this(indexer, log, in, null);
    }

    public IndexActivity(LogIndexer indexer, ActivityLog log, InputStream in, CompletionListener completionListener)
    {
        this.indexer = indexer;
        this.log = log;
        this.in = in;
        this.completionListener = completionListener;
    }

    @Override
    public void completed(boolean wasSuccessful)
    {
        Closeables.closeQuietly(in);
        if ( completionListener != null )
        {
            completionListener.completed();
        }
    }

    @Override
    public Boolean call() throws Exception
    {
        ExecutorService     server = Executors.newSingleThreadExecutor();
        server.submit
        (
            new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    try
                    {
                        while ( !Thread.currentThread().isInterrupted() )
                        {
                            Thread.sleep(1000);
                            log.add(ActivityLog.Type.INFO, "Indexing " + indexer.getLogSourceName() + " " + indexer.getPercentDone() + "%");
                        }
                    }
                    catch ( InterruptedException e )
                    {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }
            }
        );
        try
        {
            indexer.index();
            log.add(ActivityLog.Type.INFO, "Indexing " + indexer.getLogSourceName() + " done");
        }
        catch ( Exception e )
        {
            log.add(ActivityLog.Type.ERROR, "Indexing " + indexer.getLogSourceName(), e);
        }
        server.shutdownNow();
        return true;
    }
}
