/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.AuthUser;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geotools.util.logging.Logging;

/**
 * A delegating {@link RuleReaderService} with caching capabilities.
 *
 * <p>Cache eviction policy is LRU.<br>
 * Cache coherence is handled by entry timeout.<br>
 *
 * <p>
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class CachedRuleReader implements RuleReaderService {

    static final Logger LOGGER = Logging.getLogger(CachedRuleReader.class);

    private RuleReaderService realRuleReaderService;

    private LoadingCache<RuleFilter, AccessInfo> ruleCache;

    private LoadingCache<NamePw, AuthUser> userCache;

    private LoadingCache<RuleFilter, AccessInfo> authCache;

    private final GeoFenceConfigurationManager configurationManager;

    /** Latest configuration used */
    private CacheConfiguration cacheConfiguration = new CacheConfiguration();

    public CachedRuleReader(GeoFenceConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;

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

        ruleCache = getCacheBuilder().build(new RuleLoader());
        userCache = getCacheBuilder().build(new UserLoader());
        authCache = getCacheBuilder().build(new AuthLoader());
    }

    protected CacheBuilder getCacheBuilder() {
        CacheBuilder builder =
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

    private class RuleLoader extends CacheLoader<RuleFilter, AccessInfo> {

        @Override
        public AccessInfo load(RuleFilter filter) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading {0}", filter);
            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();
            return realRuleReaderService.getAccessInfo(clone);
        }

        @Override
        public ListenableFuture<AccessInfo> reload(final RuleFilter filter, AccessInfo accessInfo)
                throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reloading {0}", filter);

            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();

            // this is a sync implementation
            AccessInfo ret = realRuleReaderService.getAccessInfo(clone);
            return Futures.immediateFuture(ret);

            // next there is an asynchronous implementation, but in tests it seems to hang
            // return ListenableFutureTask.create(new Callable<AccessInfo>() {
            // @Override
            // public AccessInfo call() throws Exception {
            // if(LOGGER.isLoggable(Level.FINE))
            // LOGGER.log(Level.FINE, "Asynch reloading {0}", filter);
            // return realRuleReaderService.getAccessInfo(filter);
            // }
            // });
        }
    }

    private class AuthLoader extends CacheLoader<RuleFilter, AccessInfo> {

        @Override
        public AccessInfo load(RuleFilter filter) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading {0}", filter);
            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();
            return realRuleReaderService.getAdminAuthorization(clone);
        }

        @Override
        public ListenableFuture<AccessInfo> reload(final RuleFilter filter, AccessInfo accessInfo)
                throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reloading {0}", filter);

            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();

            // this is a sync implementation
            AccessInfo ret = realRuleReaderService.getAdminAuthorization(clone);
            return Futures.immediateFuture(ret);
        }
    }

    private class UserLoader extends CacheLoader<NamePw, AuthUser> {

        @Override
        public AuthUser load(NamePw user) throws NoAuthException {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Loading user '" + user.getName() + "'");
            AuthUser auth = realRuleReaderService.authorize(user.getName(), user.getPw());
            if (auth == null) throw new NoAuthException("Can't auth user [" + user.getName() + "]");
            return auth;
        }

        @Override
        public ListenableFuture<AuthUser> reload(final NamePw user, AuthUser authUser)
                throws NoAuthException {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Reloading user '" + user.getName() + "'");

            // this is a sync implementation
            AuthUser auth = realRuleReaderService.authorize(user.getName(), user.getPw());
            if (auth == null) throw new NoAuthException("Can't auth user [" + user.getName() + "]");
            return Futures.immediateFuture(auth);

            // todo: we may want a asynchronous implementation
        }
    }

    public void invalidateAll() {
        if (LOGGER.isLoggable(Level.WARNING))
            LOGGER.log(Level.WARNING, "Forcing cache invalidation");
        ruleCache.invalidateAll();
        userCache.invalidateAll();
        authCache.invalidateAll();
    }

    /**
     * <B>Deprecated method are not cached.</B>
     *
     * @deprecated Use {@link #getAccessInfo(RuleFilter filter) }
     */
    @Override
    public AccessInfo getAccessInfo(
            String userName,
            String profileName,
            String instanceName,
            String sourceAddress,
            String service,
            String request,
            String workspace,
            String layer) {
        LOGGER.severe("DEPRECATED METHODS ARE NOT CACHED");
        return realRuleReaderService.getAccessInfo(
                userName,
                profileName,
                instanceName,
                sourceAddress,
                service,
                request,
                workspace,
                layer);
    }

    private AtomicLong dumpCnt = new AtomicLong(0);

    @Override
    public AccessInfo getAccessInfo(RuleFilter filter) {
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Request for {0}", filter);

        if (LOGGER.isLoggable(Level.INFO))
            if (dumpCnt.incrementAndGet() % 10 == 0) {
                LOGGER.info("Rules  :" + ruleCache.stats());
                LOGGER.info("Users  :" + userCache.stats());
                LOGGER.info("Auth   :" + authCache.stats());
                LOGGER.fine("params :" + cacheConfiguration);
            }

        AccessInfo accessInfo = null;
        try {
            accessInfo = ruleCache.get(filter);
        } catch (ExecutionException ex) {
            // throw new RuntimeException(ex); // fixme: handle me
        }
        return accessInfo;
    }

    @Override
    public AccessInfo getAdminAuthorization(RuleFilter filter) {
        // return realRuleReaderService.getAdminAuthorization(filter);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "AdminAuth Request for {0}", filter);
        }

        try {
            return authCache.get(filter);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex); // fixme: handle me
        }
    }

    /**
     * <B>Deprecated method are not cached.</B>
     *
     * @deprecated Use {@link #getMatchingRules(RuleFilter filter) }
     */
    @Override
    public List<ShortRule> getMatchingRules(
            String userName,
            String profileName,
            String instanceName,
            String sourceAddress,
            String service,
            String request,
            String workspace,
            String layer) {
        LOGGER.severe("DEPRECATED METHODS ARE NOT CACHED");
        return realRuleReaderService.getMatchingRules(
                userName,
                profileName,
                instanceName,
                sourceAddress,
                service,
                request,
                workspace,
                layer);
    }

    @Override
    public List<ShortRule> getMatchingRules(RuleFilter filter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AuthUser authorize(String username, String password) {
        try {
            return userCache.get(new NamePw(username, password));
            // } catch (NoAuthException ex) {
            // LOGGER.warning(ex.getMessage());
            // return null;
        } catch (ExecutionException ex) {
            LOGGER.warning(ex.getMessage());
            return null;
        }
    }

    // --------------------------------------------------------------------------
    public void setRealRuleReaderService(RuleReaderService realRuleReaderService) {
        this.realRuleReaderService = realRuleReaderService;
    }

    public CacheConfiguration getCacheInitParams() {
        return cacheConfiguration;
    }

    public CacheStats getStats() {
        return ruleCache.stats();
    }

    public CacheStats getAdminAuthStats() {
        return authCache.stats();
    }

    public CacheStats getUserStats() {
        return userCache.stats();
    }

    public long getCacheSize() {
        return ruleCache.size();
    }

    public long getAdminAuthCacheSize() {
        return authCache.size();
    }

    public long getUserCacheSize() {
        return userCache.size();
    }

    /** May be useful if an external peer doesn't want to use the guava dep. */
    public String getStatsString() {
        return ruleCache.stats().toString();
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

    protected static class NamePw {
        private String name;

        private String pw;

        public NamePw() {}

        public NamePw(String name, String pw) {
            this.name = name;
            this.pw = pw;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPw() {
            return pw;
        }

        public void setPw(String pw) {
            this.pw = pw;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 89 * hash + (this.pw != null ? this.pw.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NamePw other = (NamePw) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            if ((this.pw == null) ? (other.pw != null) : !this.pw.equals(other.pw)) {
                return false;
            }
            return true;
        }
    }

    class NoAuthException extends Exception {

        public NoAuthException() {}

        public NoAuthException(String message) {
            super(message);
        }

        public NoAuthException(String message, Throwable cause) {
            super(message, cause);
        }

        public NoAuthException(Throwable cause) {
            super(cause);
        }
    }
}
