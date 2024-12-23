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
import javax.annotation.PostConstruct;
import org.geoserver.geofence.cache.RuleCacheLoaderFactory.NamePw;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.containers.ContainerAccessCacheLoaderFactory;
import org.geoserver.geofence.containers.ContainerLimitResolver;
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

    private RuleCacheLoaderFactory ruleServiceLoaderFactory;
    private ContainerAccessCacheLoaderFactory containerAccessCacheLoaderFactory;

    private LoadingCache<RuleFilter, AccessInfo> ruleCache;
    private LoadingCache<NamePw, AuthUser> userCache;
    private LoadingCache<RuleFilter, AccessInfo> authCache;
    private LoadingCache<ContainerAccessCacheLoaderFactory.ResolveParams, ContainerLimitResolver.ProcessingResult>
            contCache;

    private final GeoFenceConfigurationManager configurationManager;

    /** Latest configuration used */
    private CacheConfiguration cacheConfiguration = new CacheConfiguration();

    /**
     * This is a do-it-all constructor, that also calls the init() method. Useful whien testing. You may want to use the
     * simpler constructor + setters in a running environment, in order to avoid circular bean dependencies.
     */
    public CacheManager(
            GeoFenceConfigurationManager configurationManager,
            RuleCacheLoaderFactory cachedRuleLoaders,
            ContainerAccessCacheLoaderFactory containerAccessCacheLoaderFactory) {

        this(configurationManager);
        setRuleServiceLoaderFactory(ruleServiceLoaderFactory);
        setContainerAccessCacheLoaderFactory(containerAccessCacheLoaderFactory);
        init();
    }

    public CacheManager(GeoFenceConfigurationManager configurationManager) {

        this.configurationManager = configurationManager;
    }

    public final void setRuleServiceLoaderFactory(RuleCacheLoaderFactory ruleServiceLoaderFactory) {
        this.ruleServiceLoaderFactory = ruleServiceLoaderFactory;
    }

    public final void setContainerAccessCacheLoaderFactory(
            ContainerAccessCacheLoaderFactory containerAccessCacheLoaderFactory) {
        this.containerAccessCacheLoaderFactory = containerAccessCacheLoaderFactory;
    }

    /**
     * (Re)Init the cache, pulling the configuration from the configurationManager.
     *
     * <p>Please use {@link #getCacheInitParams() } to set the cache parameters before <code>init()
     * </code>ting the cache
     */
    @PostConstruct
    public final void init() {

        cacheConfiguration = configurationManager.getCacheConfiguration();

        ruleCache = getCacheBuilder().build(ruleServiceLoaderFactory.createRuleLoader());
        userCache = getCacheBuilder().build(ruleServiceLoaderFactory.createUserLoader());
        authCache = getCacheBuilder().build(ruleServiceLoaderFactory.createAuthLoader());
        contCache = getCacheBuilder().build(containerAccessCacheLoaderFactory.createProcessingResultLoader());
    }

    protected CacheBuilder<Object, Object> getCacheBuilder() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
                .maximumSize(cacheConfiguration.getSize())
                .refreshAfterWrite(
                        cacheConfiguration.getRefreshMilliSec(), TimeUnit.MILLISECONDS) // reloadable after x time
                .expireAfterWrite(
                        cacheConfiguration.getExpireMilliSec(), TimeUnit.MILLISECONDS) // throw away entries too old
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
        if (LOGGER.isLoggable(Level.WARNING)) LOGGER.log(Level.WARNING, "Forcing cache invalidation");
        ruleCache.invalidateAll();
        userCache.invalidateAll();
        authCache.invalidateAll();
        contCache.invalidateAll();
    }

    private AtomicLong dumpCnt = new AtomicLong(0);

    public void logStats() {

        if (LOGGER.isLoggable(Level.INFO))
            if (dumpCnt.incrementAndGet() % 10 == 0) {
                LOGGER.info("Rules  :" + ruleCache.stats());
                LOGGER.info("Users  :" + userCache.stats());
                LOGGER.info("Auth   :" + authCache.stats());
                LOGGER.info("Cont   :" + contCache.stats());
                LOGGER.fine("params :" + cacheConfiguration);
            }
    }

    // --------------------------------------------------------------------------

    public CacheConfiguration getCacheInitParams() {
        return cacheConfiguration;
    }

    public LoadingCache<RuleFilter, AccessInfo> getRuleCache() {
        if (ruleCache == null) throw new IllegalStateException("CacheManager is not properly inizialized");
        logStats();
        return ruleCache;
    }

    public LoadingCache<NamePw, AuthUser> getUserCache() {
        if (userCache == null) throw new IllegalStateException("CacheManager is not properly inizialized");
        logStats();
        return userCache;
    }

    public LoadingCache<RuleFilter, AccessInfo> getAuthCache() {
        if (authCache == null) throw new IllegalStateException("CacheManager is not properly inizialized");
        logStats();
        return authCache;
    }

    public LoadingCache<ContainerAccessCacheLoaderFactory.ResolveParams, ContainerLimitResolver.ProcessingResult>
            getContainerCache() {
        logStats();
        return contCache;
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
                + " Cont:"
                + contCache.stats()
                + " "
                + cacheConfiguration
                + "]";
    }
}
