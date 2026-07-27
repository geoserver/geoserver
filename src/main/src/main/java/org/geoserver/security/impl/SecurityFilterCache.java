/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogInfo;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;

/**
 * Small cache for security prefilters keyed by user, catalog type, and admin-request state.
 *
 * <p>The helper keeps memoization isolated from the policy logic that builds the filters, which remains in
 * {@link DefaultResourceAccessManager}. This makes it possible to unit test cache keying and invalidation without
 * involving the catalog wrapper.
 */
class SecurityFilterCache {

    private static final Logger LOGGER = Logging.getLogger(SecurityFilterCache.class);

    /**
     * Creates security filters for a user, catalog type, and request mode.
     *
     * <p>The explicit {@code adminRequest} flag keeps the cache contract visible to callers and avoids relying on
     * ambient thread-local state during filter construction.
     */
    @FunctionalInterface
    interface FilterLoader {

        /**
         * Builds a security filter for the supplied lookup key.
         *
         * @param user authenticated user whose credentials influence the filter
         * @param clazz catalog type whose filter is being requested
         * @param adminRequest whether the lookup is happening inside an admin request
         * @return a newly built security filter
         */
        Filter load(Authentication user, Class<? extends CatalogInfo> clazz, boolean adminRequest);
    }

    /**
     * Cache key that keeps normal requests and AdminRequest-backed requests in separate buckets.
     *
     * <p>The same authenticated user may need a broader or narrower prefilter depending on whether the lookup is
     * happening inside an admin request, so the request mode must be part of the cache identity.
     */
    static final class SecurityFilterCacheKey {

        private final Authentication user;
        private final Class<? extends CatalogInfo> clazz;
        private final boolean adminRequest;

        /**
         * Builds a cache key for a user, catalog type, and request mode.
         *
         * @param user authenticated user used to derive the security filter
         * @param clazz catalog type whose security filter is being memoized
         * @param adminRequest whether the current lookup is inside {@code AdminRequest}
         */
        SecurityFilterCacheKey(Authentication user, Class<? extends CatalogInfo> clazz, boolean adminRequest) {
            this.user = user;
            this.clazz = clazz;
            this.adminRequest = adminRequest;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SecurityFilterCacheKey that)) return false;
            return adminRequest == that.adminRequest
                    && Objects.equals(user, that.user)
                    && Objects.equals(clazz, that.clazz);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(user);
            result = 31 * result + Objects.hashCode(clazz);
            result = 31 * result + Boolean.hashCode(adminRequest);
            return result;
        }
    }

    private final Cache<SecurityFilterCacheKey, Filter> cache;
    private final FilterLoader loader;

    /**
     * Builds a cache with the default Guava eviction policy used by the access manager.
     *
     * @param maxSize maximum number of cached filters
     * @param expiryMinutes time after access before an entry expires
     */
    SecurityFilterCache(int maxSize, long expiryMinutes, FilterLoader loader) {
        this(
                CacheBuilder.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterAccess(expiryMinutes, TimeUnit.MINUTES)
                        .build(),
                loader);
    }

    /**
     * Builds a cache around an existing Guava cache instance.
     *
     * <p>This constructor is primarily intended for focused tests.
     *
     * @param cache backing cache implementation
     */
    SecurityFilterCache(Cache<SecurityFilterCacheKey, Filter> cache, FilterLoader loader) {
        this.cache = cache;
        this.loader = loader;
    }

    /**
     * Returns the memoized filter for the supplied key, loading it on demand when missing.
     *
     * @param user authenticated user whose credentials influence the filter
     * @param clazz catalog type whose filter is being requested
     * @param adminRequest whether the request is inside an admin-request context
     * @return cached or freshly built filter
     */
    Filter get(Authentication user, Class<? extends CatalogInfo> clazz, boolean adminRequest) {
        SecurityFilterCacheKey key = new SecurityFilterCacheKey(user, clazz, adminRequest);
        try {
            AtomicBoolean builtByThisCall = new AtomicBoolean(false);
            Filter result = cache.get(key, () -> {
                builtByThisCall.set(true);
                return loader.load(user, clazz, adminRequest);
            });
            if (builtByThisCall.get()) {
                LOGGER.fine(() -> "Security filter cache built for user=" + describeUser(user)
                        + ", type=" + clazz.getSimpleName()
                        + ", adminRequest=" + adminRequest);
            } else {
                LOGGER.fine(() -> "Security filter cache reused existing value for user=" + describeUser(user)
                        + ", type=" + clazz.getSimpleName()
                        + ", adminRequest=" + adminRequest);
            }
            return result;
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to build security prefilter", e);
        }
    }

    /** Clears every cached security prefilter. */
    void invalidateAll() {
        LOGGER.fine("Invalidating all cached security prefilters");
        cache.invalidateAll();
    }

    private String describeUser(Authentication user) {
        return user == null ? "<anonymous>" : user.getName();
    }
}
