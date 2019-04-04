/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensions.MultipleBeansException;
import org.geotools.util.logging.Logging;

public class DefaultCacheProvider implements CacheProvider {
    private static final Logger LOGGER = Logging.getLogger(DefaultCacheProvider.class);

    public static final int DEFAULT_CONCURRENCY_LEVEL = 4;

    public static final int DEFAULT_EXPIRATION_MINUTES = 20;

    public static final int DEFAULT_MAX_ENTRIES = 25000;

    public static final String BEAN_NAME_PROPERTY = "GEOSERVER_DEFAULT_CACHE_PROVIDER";

    @Override
    public <K extends Serializable, V extends Serializable> Cache<K, V> getCache(String cacheName) {

        Cache<K, V> cache =
                CacheBuilder.newBuilder()
                        .weakValues()
                        .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
                        .expireAfterAccess(DEFAULT_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                        .maximumSize(DEFAULT_MAX_ENTRIES)
                        .build();

        return cache;
    }

    public static CacheProvider findProvider() {
        CacheProvider cacheProvider = null;
        // Check for a named bean
        String providerNames = GeoServerExtensions.getProperty(BEAN_NAME_PROPERTY);
        if (providerNames != null) {
            for (String providerName : providerNames.split("\\s*,\\s*")) {
                cacheProvider =
                        (CacheProvider) (CacheProvider) GeoServerExtensions.bean(providerName);
                if (cacheProvider != null) {
                    LOGGER.log(Level.INFO, "Using specified Cache Provider ", providerName);
                    break;
                }
            }
            if (cacheProvider == null) {
                LOGGER.log(
                        Level.INFO,
                        "{0} was specified but no beans matched it.",
                        BEAN_NAME_PROPERTY);
            }
        }
        // Find a bean by interface
        if (cacheProvider == null) {
            try {
                cacheProvider = GeoServerExtensions.bean(CacheProvider.class);
            } catch (MultipleBeansException ex) {
                String providerName = ex.getAvailableBeans().iterator().next();
                if (LOGGER.isLoggable(Level.WARNING)) {
                    String available = StringUtils.join(ex.getAvailableBeans(), ", ");
                    LOGGER.log(
                            Level.WARNING,
                            "Multiple Cache Providers in context: {0}\n\tUsing {1}.  Override by setting system property {2}",
                            new Object[] {available, providerName, BEAN_NAME_PROPERTY});
                }
                cacheProvider =
                        (CacheProvider) (CacheProvider) GeoServerExtensions.bean(providerName);
            }
        }

        // Use the default
        if (cacheProvider == null) {
            cacheProvider = new DefaultCacheProvider();
        }
        return cacheProvider;
    }
}
