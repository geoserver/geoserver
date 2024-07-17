/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.security.core.Authentication;

/**
 * Implementation of GeoServer AuthenticationCache based on Guava Cache.
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli at geo-solutions.it)
 */
public class GuavaAuthenticationCacheImpl
        implements AuthenticationCache, GeoServerLifecycleHandler, DisposableBean {

    /** Default eviction interval (double of the idle time). */
    public static final int DEFAULT_CLEANUP_TIME = DEFAULT_IDLE_TIME * 2;

    /**
     * Default concurrency level (allows guava cache to optimize internal size to serve the given #
     * of threads at the same time).
     */
    public static final int DEFAULT_CONCURRENCY_LEVEL = 3;

    private int timeToIdleSeconds, timeToLiveSeconds;

    private final ScheduledExecutorService scheduler;

    private Cache<AuthenticationCacheKey, AuthenticationCacheEntry> cache;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** Eviction thread code. Delegates to guava Cache cleanUp. */
    private Runnable evictionTask =
            new Runnable() {
                @Override
                public void run() {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("AuthenticationCache Eviction task running");
                        LOGGER.fine("Cache entries #: " + cache.size());
                    }
                    cache.cleanUp();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("AuthenticationCache Eviction task completed");
                        LOGGER.fine("Cache entries #: " + cache.size());
                    }
                }
            };

    public GuavaAuthenticationCacheImpl(int maxEntries) {
        this(
                maxEntries,
                DEFAULT_IDLE_TIME,
                DEFAULT_LIVE_TIME,
                DEFAULT_CLEANUP_TIME,
                DEFAULT_CONCURRENCY_LEVEL);
    }

    // Use a counter to ensure a unique prefix for each pool.
    private static AtomicInteger poolCounter = new AtomicInteger();

    private ThreadFactory getThreadFactory() {
        CustomizableThreadFactory tFactory =
                new CustomizableThreadFactory(
                        String.format("GuavaAuthCache-%d-", poolCounter.getAndIncrement()));
        tFactory.setDaemon(true);
        return tFactory;
    }

    public GuavaAuthenticationCacheImpl(
            int maxEntries,
            int timeToIdleSeconds,
            int timeToLiveSeconds,
            int cleanUpSeconds,
            int concurrencyLevel) {
        this.timeToIdleSeconds = timeToIdleSeconds;
        this.timeToLiveSeconds = timeToLiveSeconds;

        scheduler = Executors.newScheduledThreadPool(1, getThreadFactory());

        cache =
                CacheBuilder.newBuilder()
                        .maximumSize(maxEntries)
                        .expireAfterAccess(timeToIdleSeconds, TimeUnit.SECONDS)
                        .expireAfterWrite(timeToLiveSeconds, TimeUnit.SECONDS)
                        .concurrencyLevel(concurrencyLevel)
                        .build();
        if (LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config(
                    "AuthenticationCache Initialized with "
                            + maxEntries
                            + " Max Entries, "
                            + timeToIdleSeconds
                            + " seconds idle time, "
                            + timeToLiveSeconds
                            + " seconds time to live and "
                            + concurrencyLevel
                            + " concurrency level");
        }

        // schedule eviction thread
        scheduler.scheduleAtFixedRate(
                evictionTask, cleanUpSeconds, cleanUpSeconds, TimeUnit.SECONDS);
        if (LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config(
                    "AuthenticationCache Eviction Task created to run every "
                            + cleanUpSeconds
                            + " seconds");
        }
    }

    @Override
    public void removeAll() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache removing all entries");
            LOGGER.fine("Cache entries #: " + cache.size());
        }
        cache.invalidateAll();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache removed all entries");
            LOGGER.fine("Cache entries #: " + cache.size());
        }
    }

    @Override
    public void removeAll(String filterName) {
        if (filterName == null) return;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache removing all entries for " + filterName);
            LOGGER.fine("Cache entries #: " + cache.size());
        }
        Set<AuthenticationCacheKey> toBeRemoved = new HashSet<>();
        for (AuthenticationCacheKey key : cache.asMap().keySet()) {
            if (filterName.equals(key.getFilterName())) toBeRemoved.add(key);
        }

        cache.invalidateAll(toBeRemoved);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "AuthenticationCache removed "
                            + toBeRemoved.size()
                            + " entries for "
                            + filterName);
            LOGGER.fine("Cache entries #: " + cache.size());
        }
    }

    @Override
    public void remove(String filterName, String cacheKey) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache removing " + filterName + ", " + cacheKey + " entry");
            LOGGER.fine("Cache entries #: " + cache.size());
        }
        cache.invalidate(new AuthenticationCacheKey(filterName, cacheKey));
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache removed " + filterName + ", " + cacheKey + " entry");
            LOGGER.fine("Cache entries #: " + cache.size());
        }
    }

    @Override
    public Authentication get(String filterName, String cacheKey) {
        final AuthenticationCacheKey key = new AuthenticationCacheKey(filterName, cacheKey);
        AuthenticationCacheEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("AuthenticationCache has no entry for " + filterName + ", " + cacheKey);
            }
            return null;
        }
        long currentTime = System.currentTimeMillis();
        if (entry.hasExpired(currentTime)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Entry has expired");
            }
            cache.invalidate(key);
            return null;
        }
        entry.setLastAccessed(System.currentTimeMillis());
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache found an entry for " + filterName + ", " + cacheKey);
        }
        return entry.getAuthentication();
    }

    @Override
    public void put(
            String filterName,
            String cacheKey,
            Authentication auth,
            Integer timeToIdleSeconds,
            Integer timeToLiveSeconds) {
        timeToIdleSeconds = timeToIdleSeconds != null ? timeToIdleSeconds : this.timeToIdleSeconds;
        timeToLiveSeconds = timeToLiveSeconds != null ? timeToLiveSeconds : this.timeToLiveSeconds;

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache adding new entry for " + filterName + ", " + cacheKey);
            LOGGER.fine("Cache entries #: " + cache.size());
        }
        cache.put(
                new AuthenticationCacheKey(filterName, cacheKey),
                new AuthenticationCacheEntry(auth, timeToIdleSeconds, timeToLiveSeconds));
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache added new entry for " + filterName + ", " + cacheKey);
            LOGGER.fine("Cache entries #: " + cache.size());
        }
    }

    @Override
    public void put(String filterName, String cacheKey, Authentication auth) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache adding new entry for " + filterName + ", " + cacheKey);
            LOGGER.fine("Cache entries #: " + cache.size());
        }
        put(filterName, cacheKey, auth, timeToIdleSeconds, timeToLiveSeconds);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("AuthenticationCache added new entry for " + filterName + ", " + cacheKey);
            LOGGER.fine("Cache entries #: " + cache.size());
        }
    }

    public boolean isEmpty() {
        return cache.size() == 0;
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }

    @Override
    public void onReset() {
        removeAll();
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {}

    @Override
    public void onReload() {}
}
