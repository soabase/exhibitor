package com.netflix.exhibitor.core.temp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class LoadingCache<K, V> implements Cache<K, V>
{
    private final Cache<K, V> cache;

    public LoadingCache(Cache<K, V> cache)
    {
        this.cache = cache;
    }

    @Override
    public V get(K key) throws ExecutionException
    {
        return cache.get(key);
    }

    @Override
    public V getUnchecked(K key)
    {
        return cache.getUnchecked(key);
    }

    @Override
    public V apply(K key)
    {
        return cache.apply(key);
    }

    @Override
    public void invalidate(Object key)
    {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll()
    {
        cache.invalidateAll();
    }

    @Override
    public long size()
    {
        return cache.size();
    }

    @Override
    public CacheStats stats()
    {
        return cache.stats();
    }

    @Override
    public ConcurrentMap<K, V> asMap()
    {
        return cache.asMap();
    }

    @Override
    public void cleanUp()
    {
        cache.cleanUp();
    }

    public V put(K key, V newValue)
    {
        return cache.asMap().put(key, newValue);
    }

    public V getIfPresent(K key)
    {
        synchronized(cache)
        {
            if ( cache.asMap().containsKey(key) )
            {
                try
                {
                    return cache.get(key);
                }
                catch ( ExecutionException e )
                {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;  //To change body of created methods use File | Settings | File Templates.
    }
}
