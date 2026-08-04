/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.cache;

import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geofence.core.services.dto.AccessInfo;
import org.geofence.core.services.dto.PermsResult;
import org.geofence.core.services.dto.RuleFilter;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.geotools.util.logging.Logging;

/**
 * Creates the CacheLoaders for calls to RuleReadService
 *
 * @author Emanuele Tajariol (etj at geo-solutions.it)
 */
public class RuleCacheLoaderFactory {

    static final Logger LOGGER = Logging.getLogger(RuleCacheLoaderFactory.class);

    private final RuleReaderServiceFactory rrsFactory;

    public RuleCacheLoaderFactory(RuleReaderServiceFactory rrsFactory) {
        this.rrsFactory = rrsFactory;
    }

    public RuleLoader createRuleLoader() {
        return new RuleLoader();
    }

    public PermLoader createPermLoader() {
        return new PermLoader();
    }

    public AuthLoader createAuthLoader() {
        return new AuthLoader();
    }

    class RuleLoader extends CacheLoader<RuleFilter, AccessInfo> {

        private RuleLoader() {}

        @Override
        public AccessInfo load(RuleFilter filter) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading {0}", filter);
            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();
            return rrsFactory.getService().getAccessInfo(clone);
        }

        @Override
        public ListenableFuture<AccessInfo> reload(final RuleFilter filter, AccessInfo accessInfo) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reloading {0}", filter);

            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();

            // this is a sync implementation
            AccessInfo ret = rrsFactory.getService().getAccessInfo(clone);
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

    class PermLoader extends CacheLoader<RuleFilter, PermsResult> {

        private PermLoader() {}

        @Override
        public PermsResult load(RuleFilter filter) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading perms for {0}", filter);
            RuleFilter clone = filter.clone();
            return rrsFactory.getService().getPermissionFilter(clone);
        }

        @Override
        public ListenableFuture<PermsResult> reload(final RuleFilter filter, PermsResult perms) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reloading perms for {0}", filter);

            RuleFilter clone = filter.clone();

            // this is a sync implementation
            PermsResult ret = rrsFactory.getService().getPermissionFilter(clone);
            return Futures.immediateFuture(ret);
        }
    }

    class AuthLoader extends CacheLoader<RuleFilter, AccessInfo> {

        private AuthLoader() {}

        @Override
        public AccessInfo load(RuleFilter filter) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading {0}", filter);
            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();
            return rrsFactory.getService().getAdminAuthorization(clone);
        }

        @Override
        public ListenableFuture<AccessInfo> reload(final RuleFilter filter, AccessInfo accessInfo) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reloading {0}", filter);

            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();

            // this is a sync implementation
            AccessInfo ret = rrsFactory.getService().getAdminAuthorization(clone);
            return Futures.immediateFuture(ret);
        }
    }
}
