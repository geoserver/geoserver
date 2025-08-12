/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.utils;

import java.util.List;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.AuthUser;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.services.dto.ShortRule;

/** @author etj */
public class RuleReaderServiceAdapter implements RuleReaderService {

    @Override
    public AccessInfo getAccessInfo(RuleFilter filter) {
        return null;
    }

    @Override
    public AccessInfo getAdminAuthorization(RuleFilter filter) {
        return null;
    }

    @Override
    public List<ShortRule> getMatchingRules(RuleFilter filter) {
        return null;
    }

    @Override
    public AuthUser authorize(String username, String password) {
        return null;
    }
}
