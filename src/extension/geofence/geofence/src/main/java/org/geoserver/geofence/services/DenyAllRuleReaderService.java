/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.services;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geofence.core.services.RuleReaderService;
import org.geofence.core.services.dto.AccessInfo;
import org.geofence.core.services.dto.GrantTypeDTO;
import org.geofence.core.services.dto.RuleFilter;
import org.geofence.core.services.dto.ShortRule;
import org.geotools.util.logging.Logging;

/**
 * Fail-safe {@link RuleReaderService} that denies everything - used by {@link RuleReaderServiceFactory} as a fallback.
 * Every call is logged as a warning: this should never actually be serving requests.
 */
public class DenyAllRuleReaderService implements RuleReaderService {

    private static final Logger LOGGER = Logging.getLogger(DenyAllRuleReaderService.class);

    @Override
    public AccessInfo getAccessInfo(RuleFilter filter) {
        warn("getAccessInfo", filter);
        return new AccessInfo(GrantTypeDTO.DENY);
    }

    @Override
    public AccessInfo getAdminAuthorization(RuleFilter filter) {
        warn("getAdminAuthorization", filter);
        // grant is always ALLOW by contract; no admin rights is the default on a fresh instance.
        AccessInfo ret = new AccessInfo(GrantTypeDTO.ALLOW);
        ret.setAdminRights(false);
        return ret;
    }

    @Override
    public List<ShortRule> getMatchingRules(RuleFilter filter) {
        warn("getMatchingRules", filter);
        return Collections.emptyList();
    }

    private void warn(String method, RuleFilter filter) {
        LOGGER.log(Level.WARNING, "DenyAllRuleReaderService.{0}() called with {1}", new Object[] {method, filter});
    }
}
