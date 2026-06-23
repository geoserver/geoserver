/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.services;

import java.util.List;
import org.geofence.core.services.RuleReaderService;
import org.geofence.core.services.dto.AccessInfo;
import org.geofence.core.services.dto.RuleFilter;
import org.geofence.core.services.dto.ShortRule;
import org.springframework.stereotype.Component;

@Component("RestRuleReaderService")
public class RestRuleReaderService implements RuleReaderService {

    private String serviceUrl;

    public RestRuleReaderService(String serviceUrl) {}

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
        return List.of();
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
}
