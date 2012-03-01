package com.netflix.exhibitor.core.temp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import java.util.concurrent.TimeUnit;

public class CacheBuilder<K, V>
{
    private final com.google.common.cache.CacheBuilder<Object, Object>      realCacheBuilder = com.google.common.cache.CacheBuilder.newBuilder();

    public static CacheBuilder<Object, Object> newBuilder() {
        return new CacheBuilder<Object, Object>();
    }

    public CacheBuilder<K, V> softValues()
    {
        realCacheBuilder.softValues();
        return this;
    }

    public CacheBuilder<K, V> expireAfterAccess(long duration, TimeUnit unit) {
        realCacheBuilder.expireAfterAccess(duration, unit);
        return this;
    }

    public <K1 extends K, V1 extends V> LoadingCache<K1, V1> build(CacheLoader<? super K1, V1> loader) {
        Cache<K1, V1> cache = realCacheBuilder.build(loader);
        return new LoadingCache<K1, V1>(cache);
    }

    public <K1 extends K, V1 extends V> LoadingCache<K1, V1> build()
    {
        Cache<K1, V1> cache = realCacheBuilder.build
        (
            new CacheLoader<K1, V1>()
            {
                @Override
                public V1 load(K1 key) throws Exception
                {
                    return null;    // should never get here
                }
            }
        );
        return new LoadingCache<K1, V1>(cache);
    }
}
