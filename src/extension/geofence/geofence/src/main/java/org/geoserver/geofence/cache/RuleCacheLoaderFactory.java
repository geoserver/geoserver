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
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.AuthUser;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geotools.util.logging.Logging;

/**
 * Creates the CacheLoaders for calls to RuleReadService
 *
 * @author Emanuele Tajariol (etj at geo-solutions.it)
 */
public class RuleCacheLoaderFactory {

    static final Logger LOGGER = Logging.getLogger(RuleCacheLoaderFactory.class);

    private RuleReaderService realRuleReaderService;

    public RuleCacheLoaderFactory(RuleReaderService realRuleReaderService) {
        this.realRuleReaderService = realRuleReaderService;
    }

    public RuleLoader createRuleLoader() {
        return new RuleLoader();
    }

    public AuthLoader createAuthLoader() {
        return new AuthLoader();
    }

    public UserLoader createUserLoader() {
        return new UserLoader();
    }

    class RuleLoader extends CacheLoader<RuleFilter, AccessInfo> {

        private RuleLoader() {}

        @Override
        public AccessInfo load(RuleFilter filter) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading {0}", filter);
            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();
            return realRuleReaderService.getAccessInfo(clone);
        }

        @Override
        public ListenableFuture<AccessInfo> reload(final RuleFilter filter, AccessInfo accessInfo) throws Exception {
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

    class AuthLoader extends CacheLoader<RuleFilter, AccessInfo> {

        private AuthLoader() {}

        @Override
        public AccessInfo load(RuleFilter filter) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading {0}", filter);
            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();
            return realRuleReaderService.getAdminAuthorization(clone);
        }

        @Override
        public ListenableFuture<AccessInfo> reload(final RuleFilter filter, AccessInfo accessInfo) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reloading {0}", filter);

            // the service, when integrated, may modify the filter
            RuleFilter clone = filter.clone();

            // this is a sync implementation
            AccessInfo ret = realRuleReaderService.getAdminAuthorization(clone);
            return Futures.immediateFuture(ret);
        }
    }

    class UserLoader extends CacheLoader<NamePw, AuthUser> {

        private UserLoader() {}

        @Override
        public AuthUser load(NamePw user) throws NoAuthException {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading user '" + user.getName() + "'");
            AuthUser auth = realRuleReaderService.authorize(user.getName(), user.getPw());
            if (auth == null) throw new NoAuthException("Can't auth user [" + user.getName() + "]");
            return auth;
        }

        @Override
        public ListenableFuture<AuthUser> reload(final NamePw user, AuthUser authUser) throws NoAuthException {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reloading user '" + user.getName() + "'");

            // this is a sync implementation
            AuthUser auth = realRuleReaderService.authorize(user.getName(), user.getPw());
            if (auth == null) throw new NoAuthException("Can't auth user [" + user.getName() + "]");
            return Futures.immediateFuture(auth);

            // todo: we may want a asynchronous implementation
        }
    }

    public static class NamePw {
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

    static class NoAuthException extends Exception {

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
