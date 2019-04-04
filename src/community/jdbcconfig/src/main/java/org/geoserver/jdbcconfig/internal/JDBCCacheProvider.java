/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import org.geoserver.util.CacheProvider;

/**
 * This class implements the {@link CacheProvider} interface and can be used for configuring a new
 * {@link Cache} object with two Java parameters called maxCachedEntries and evictionTime. The first
 * parameters indicates the maximum entries that the cache could contain (by default 25000) while
 * the second one indicates the eviction time for each entry in minutes(default 20).
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class JDBCCacheProvider implements CacheProvider {

    public static final int DEFAULT_CONCURRENCY_LEVEL = 4;

    public static final int DEFAULT_EXPIRATION_MINUTES = 20;

    public static final int DEFAULT_MAX_ENTRIES = 25000;

    public static final String DEFAULT_SIZE_KEY = "maxCachedEntries";

    public static final String DEFAULT_TIME_KEY = "evictionTime";

    /** Maximum number of cache entries */
    public final int maxEntries =
            Integer.parseInt(System.getProperty(DEFAULT_SIZE_KEY, DEFAULT_MAX_ENTRIES + ""));

    /** Expiration time in minutes for each entry */
    public final long expirationMinutes =
            Long.parseLong(System.getProperty(DEFAULT_TIME_KEY, DEFAULT_EXPIRATION_MINUTES + ""));

    @Override
    public <K extends Serializable, V extends Serializable> Cache<K, V> getCache(String cacheName) {
        // Cache creation
        Cache<K, V> cache =
                CacheBuilder.newBuilder()
                        .softValues()
                        .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
                        .expireAfterAccess(expirationMinutes, TimeUnit.MINUTES)
                        .maximumSize(maxEntries)
                        .build();

        return cache;
    }
}
