/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.cache;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.geofence.cache.RuleCacheLoaderFactory.NamePw;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.AuthUser;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geotools.util.logging.Logging;

/**
 * A delegating {@link RuleReaderService} with caching capabilities.
 *
 * @author Emanuele Tajariol (etj at geo-solutions.it)
 */
public class CachedRuleReader implements RuleReaderService {

    static final Logger LOGGER = Logging.getLogger(CachedRuleReader.class);

    private CacheManager cacheManager;

    public CachedRuleReader() {}

    public CachedRuleReader(CacheManager cacheManager) {
        setCacheManager(cacheManager);
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public AccessInfo getAccessInfo(RuleFilter filter) {
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Request for {0}", filter);

        AccessInfo accessInfo = null;
        try {
            accessInfo = cacheManager.getRuleCache().get(filter);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex); // fixme: handle me
        }
        return accessInfo;
    }

    @Override
    public AccessInfo getAdminAuthorization(RuleFilter filter) {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "AdminAuth Request for {0}", filter);
        }

        try {
            return cacheManager.getAuthCache().get(filter);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex); // fixme: handle me
        }
    }

    @Override
    public List<ShortRule> getMatchingRules(RuleFilter filter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AuthUser authorize(String username, String password) {
        try {
            return cacheManager.getUserCache().get(new NamePw(username, password));
        } catch (ExecutionException ex) {
            LOGGER.warning(ex.getMessage());
            return null;
        }
    }
}
