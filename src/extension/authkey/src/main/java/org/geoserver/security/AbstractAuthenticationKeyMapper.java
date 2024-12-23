/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.FilterConfigException;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link AuthenticationKeyMapper} implementations
 *
 * @author christian
 */
public abstract class AbstractAuthenticationKeyMapper implements AuthenticationKeyMapper {

    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security");

    private String beanName;
    private String userGroupServiceName;
    private String authenticationFilterName;
    private GeoServerSecurityManager securityManager;

    private Map<String, String> parameters = new HashMap<>();

    // Cache map with expiration tracking
    private final ConcurrentHashMap<String, CacheEntry> userCache = new ConcurrentHashMap<>();

    // Default TTL for cache entries (in seconds)
    private long cacheTtlSeconds = 300; // Default: 5 minutes

    // Executor for cleaning up expired cache entries
    private final ScheduledExecutorService cacheCleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public AbstractAuthenticationKeyMapper() {
        super();
        fillDefaultParameters();
        startCacheCleanupTask();
    }

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

    @Override
    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    @Override
    public GeoServerSecurityManager getSecurityManager() {
        return securityManager;
    }

    @Override
    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    protected GeoServerUserGroupService getUserGroupService() throws IOException {
        GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(getUserGroupServiceName());
        if (service == null) {
            throw new IOException("Unknown user/group service: " + getUserGroupServiceName());
        }
        return service;
    }

    protected void checkProperties() throws IOException {
        if (!StringUtils.hasLength(getUserGroupServiceName())) {
            throw new IOException("User/Group Service Name is unset");
        }
        if (getSecurityManager() == null) {
            throw new IOException("Security manager is unset");
        }
        this.checkPropertiesInternal();
    }

    protected abstract void checkPropertiesInternal() throws IOException;

    protected String createAuthKey() {
        return UUID.randomUUID().toString();
    }

    /** Returns the list of configuration parameters supported by the mapper. */
    @Override
    public Set<String> getAvailableParameters() {
        return new HashSet<>();
    }

    /**
     * Configures the mapper parameters.
     *
     * @param parameters mapper parameters
     */
    @Override
    public void configureMapper(Map<String, String> parameters) {
        this.parameters = parameters;
        fillDefaultParameters();

        // Configure cache TTL if specified
        if (parameters.containsKey("cacheTtlSeconds")) {
            try {
                cacheTtlSeconds = Long.parseLong(parameters.get("cacheTtlSeconds"));
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid cacheTtlSeconds value. Using default.");
            }
        }
    }

    /** Fills parameters with default values (if defined by the mapper. */
    private void fillDefaultParameters() {
        for (String paramName : getAvailableParameters()) {
            if (!this.parameters.containsKey(paramName)) {
                this.parameters.put(paramName, getDefaultParamValue(paramName));
            }
        }
    }

    /** Gets the default value for the given parameter. The Default implementation always returns an empty string. */
    protected String getDefaultParamValue(String paramName) {
        return "";
    }

    @Override
    public Map<String, String> getMapperConfiguration() {
        return parameters;
    }

    /** Validates the given parameter (used by the filter validator). */
    @Override
    public void validateParameter(String paramName, String value) throws FilterConfigException {}

    /** Creates a validation exception (used by inheriting mappers). */
    protected AuthenticationKeyFilterConfigException createFilterException(String errorid, Object... args) {
        return new AuthenticationKeyFilterConfigException(errorid, args);
    }

    /** Get the belonging Auth Filter Name to allow the Mapper accessing the auth cache * */
    public String getAuthenticationFilterName() {
        return authenticationFilterName;
    }

    /** Set the belonging Auth Filter Name to allow the Mapper accessing the auth cache * */
    @Override
    public void setAuthenticationFilterName(String authenticationFilterName) {
        this.authenticationFilterName = authenticationFilterName;
    }

    @Override
    public GeoServerUser getUser(String key) throws IOException {
        checkProperties();

        // Check if the user is already in the cache and not expired
        CacheEntry entry = userCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getUser();
        }

        // Proceed to call the web service if user not in the cache or expired
        GeoServerUser user = this.getUserInternal(key);
        if (key != null && user != null) {
            // Cache the user for future requests
            userCache.put(key, new CacheEntry(user, System.currentTimeMillis() + cacheTtlSeconds * 1000));
        }
        return user;
    }

    protected abstract GeoServerUser getUserInternal(String key) throws IOException;

    /** Clears the cache entries, forcing a reset. */
    public void resetUserCache() {
        userCache.clear();
    }

    /**
     * Sets the cache TTL in seconds.
     *
     * @param ttlSeconds TTL in seconds
     */
    @Override
    public void setCacheTtlSeconds(long ttlSeconds) {
        this.cacheTtlSeconds = ttlSeconds;
    }

    /** Returns the current cache TTL in seconds. */
    @Override
    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    /** Starts a periodic task to clean up expired cache entries. */
    private void startCacheCleanupTask() {
        cacheCleanupExecutor.scheduleAtFixedRate(
                () -> {
                    userCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
                },
                cacheTtlSeconds,
                cacheTtlSeconds,
                TimeUnit.SECONDS);
    }

    /** Shuts down the cache cleanup executor. */
    public void shutdown() {
        cacheCleanupExecutor.shutdownNow();
    }

    /** Inner class to represent a cache entry with a TTL. */
    private static class CacheEntry {
        private final GeoServerUser user;
        private final long expiryTime;

        CacheEntry(GeoServerUser user, long expiryTime) {
            this.user = user;
            this.expiryTime = expiryTime;
        }

        public GeoServerUser getUser() {
            return user;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
