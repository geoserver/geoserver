/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.geoserver.platform.GeoServerExtensions;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class DefaultCacheProvider implements CacheProvider {

    public static final int DEFAULT_CONCURRENCY_LEVEL = 4;

    public static final int DEFAULT_EXPIRATION_MINUTES = 20;

    public static final int DEFAULT_MAX_ENTRIES = 25000;

    @Override
    public <K extends Serializable, V extends Serializable> Cache<K, V> getCache(String cacheName) {

        Cache<K, V> cache = CacheBuilder.newBuilder().weakValues()
                .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
                .expireAfterAccess(DEFAULT_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(DEFAULT_MAX_ENTRIES).build();

        return cache;
    }

    public static CacheProvider findProvider() {
        CacheProvider cacheProvider = GeoServerExtensions.bean(CacheProvider.class);
        if (cacheProvider == null) {
            cacheProvider = new DefaultCacheProvider();
        }
        return cacheProvider;
    }
}
