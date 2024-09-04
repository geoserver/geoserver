/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.geofence.cache.CachedRuleLoaders.NamePw;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.AuthUser;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geotools.util.logging.Logging;

/**
 * A centralized point of cache control for GeoFence auth calls
 *
 * <p>Cache eviction policy is LRU.<br>
 * Cache coherence is handled by entry timeout.<br>
 *
 * <p>
 *
 * @author Emanuele Tajariol (etj at geo-solutions.it)
 */
public class CacheManager {

    static final Logger LOGGER = Logging.getLogger(CacheManager.class);

    private CachedRuleLoaders cachedRuleLoaders;

    private LoadingCache<RuleFilter, AccessInfo> ruleCache;
    private LoadingCache<NamePw, AuthUser> userCache;
    private LoadingCache<RuleFilter, AccessInfo> authCache;

    private final GeoFenceConfigurationManager configurationManager;

    /** Latest configuration used */
    private CacheConfiguration cacheConfiguration = new CacheConfiguration();

    public CacheManager(
            GeoFenceConfigurationManager configurationManager,
            CachedRuleLoaders cachedRuleLoaders) {

        this.configurationManager = configurationManager;
        this.cachedRuleLoaders = cachedRuleLoaders;

        // pull config when initializing
        init();
    }

    /**
     * (Re)Init the cache, pulling the configuration from the configurationManager.
     *
     * <p>Please use {@link #getCacheInitParams() } to set the cache parameters before <code>init()
     * </code>ting the cache
     */
    public final void init() {

        cacheConfiguration = configurationManager.getCacheConfiguration();

        ruleCache = getCacheBuilder().build(cachedRuleLoaders.new RuleLoader());
        userCache = getCacheBuilder().build(cachedRuleLoaders.new UserLoader());
        authCache = getCacheBuilder().build(cachedRuleLoaders.new AuthLoader());
    }

    protected CacheBuilder<Object, Object> getCacheBuilder() {
        CacheBuilder<Object, Object> builder =
                CacheBuilder.newBuilder()
                        .maximumSize(cacheConfiguration.getSize())
                        .refreshAfterWrite(
                                cacheConfiguration.getRefreshMilliSec(),
                                TimeUnit.MILLISECONDS) // reloadable after x time
                        .expireAfterWrite(
                                cacheConfiguration.getExpireMilliSec(),
                                TimeUnit.MILLISECONDS) // throw away entries too old
                        .recordStats();
        // .expireAfterAccess(timeoutMillis, TimeUnit.MILLISECONDS)
        // .removalListener(MY_LISTENER)
        // this should only be used while testing
        if (cacheConfiguration.getCustomTicker() != null) {
            LOGGER.log(
                    Level.SEVERE,
                    "Setting a custom Ticker in the cache {0}",
                    cacheConfiguration.getCustomTicker().getClass().getName());
            builder.ticker(cacheConfiguration.getCustomTicker());
        }
        return builder;
    }

    public void invalidateAll() {
        if (LOGGER.isLoggable(Level.WARNING))
            LOGGER.log(Level.WARNING, "Forcing cache invalidation");
        ruleCache.invalidateAll();
        userCache.invalidateAll();
        authCache.invalidateAll();
    }

    private AtomicLong dumpCnt = new AtomicLong(0);

    public void logStats() {

        if (LOGGER.isLoggable(Level.INFO))
            if (dumpCnt.incrementAndGet() % 10 == 0) {
                LOGGER.info("Rules  :" + ruleCache.stats());
                LOGGER.info("Users  :" + userCache.stats());
                LOGGER.info("Auth   :" + authCache.stats());
                LOGGER.fine("params :" + cacheConfiguration);
            }
    }

    // --------------------------------------------------------------------------

    public CacheConfiguration getCacheInitParams() {
        return cacheConfiguration;
    }

    public LoadingCache<RuleFilter, AccessInfo> getRuleCache() {
        logStats();
        return ruleCache;
    }

    public LoadingCache<NamePw, AuthUser> getUserCache() {
        logStats();
        return userCache;
    }

    public LoadingCache<RuleFilter, AccessInfo> getAuthCache() {
        logStats();
        return authCache;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "["
                + "Rule:"
                + ruleCache.stats()
                + " User:"
                + userCache.stats()
                + " Auth:"
                + authCache.stats()
                + " "
                + cacheConfiguration
                + "]";
    }
}
