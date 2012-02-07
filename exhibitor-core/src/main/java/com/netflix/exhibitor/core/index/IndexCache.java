package com.netflix.exhibitor.core.index;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class IndexCache
{
    private final LoadingCache<File, IndexMetaData>     metaDataCache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(MAX_CACHE_MS, TimeUnit.MILLISECONDS)
        .build
        (
            new CacheLoader<File, IndexMetaData>()
            {
                @Override
                public IndexMetaData load(File indexDirectory) throws Exception
                {
                    return IndexMetaData.read(IndexMetaData.getMetaDataFile(indexDirectory));
                }
            }
        );

    private final ConcurrentMap<File, LogSearchHolder>  indexCache = Maps.newConcurrentMap();

    private static class LogSearchHolder
    {
        // all protected by synchronization
        private LogSearch               logSearch;
        private int                     useCount = 0;
        private long                    lastUse = System.currentTimeMillis();
    }

    private static final int        MAX_CACHE_MS = (int)TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    public IndexMetaData    getMetaData(File indexDirectory) throws Exception
    {
        return metaDataCache.get(indexDirectory);
    }

    public LogSearch        getLogSearch(File indexDirectory) throws Exception
    {
        clean();
        
        LogSearchHolder           newHolder = new LogSearchHolder();
        LogSearchHolder           oldHolder = indexCache.putIfAbsent(indexDirectory, newHolder);
        final LogSearchHolder     useHolder = (oldHolder != null) ? oldHolder : newHolder;

        LogSearch                 logSearch;
        synchronized(useHolder)
        {
            if ( useHolder.logSearch == null )
            {
                Preconditions.checkArgument(useHolder.useCount == 0, "use count is non zero but instance is null: " + useHolder.useCount);
                useHolder.logSearch = new LogSearch(indexDirectory);
            }

            ++useHolder.useCount;
            Preconditions.checkArgument(useHolder.useCount > 0, "use count has rolled over: " + useHolder.useCount);

            useHolder.lastUse = System.currentTimeMillis();

            logSearch = useHolder.logSearch;
        }
        
        return logSearch;
    }

    public void             releaseLogSearch(File indexDirectory)
    {
        LogSearchHolder     holder = indexCache.get(indexDirectory);
        holder = Preconditions.checkNotNull(holder, "No entry found for index being released: " + indexDirectory);

        synchronized(holder)
        {
            Preconditions.checkArgument(holder.useCount > 0, "non positive use count in release: " + holder.useCount);
            --holder.lastUse;
        }
    }

    private void        clean()
    {
        Iterator<LogSearchHolder> iterator = indexCache.values().iterator();
        while ( iterator.hasNext() )
        {
            LogSearch                 evicted = null;
            final LogSearchHolder     holder = iterator.next();
            synchronized(holder)
            {
                if ( holder.useCount == 0 )
                {
                    if ( (System.currentTimeMillis() - holder.lastUse) > MAX_CACHE_MS )
                    {
                        evicted = holder.logSearch;
                        holder.logSearch = null;
                        iterator.remove();
                    }
                }
            }

            if ( evicted != null )
            {
                evicted.close();
            }
        }
    }
}
