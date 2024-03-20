/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;

/**
 * Provides conversion between External and Internal (GS) roles. See
 * GeoServerJwtHeadersFilterConfig#getRoleConverterAsMap (and #RoleConverterString)
 */
public class RoleConverter {

    Map<String, String> conversionMap;

    boolean externalNameMustBeListed;

    public RoleConverter(GeoServerJwtHeadersFilterConfig config) {
        conversionMap = config.getRoleConverterAsMap();
        externalNameMustBeListed = config.isOnlyExternalListedRoles();
    }

    /**
     * convert a list of external roles (i.e. from and IDP) into internal (GS) roles using the
     * roleConverter in the configuration. Also, pays attention to the
     * config#isOnlyExternalListedRoles setting.
     *
     * @param externalRoles
     * @return
     */
    public List<GeoServerRole> convert(List<String> externalRoles) {
        List<GeoServerRole> result = new ArrayList<>();

        if (externalRoles == null) return result; // empty

        for (String externalRole : externalRoles) {
            String gsRole = conversionMap.get(externalRole);
            if (gsRole == null && !externalNameMustBeListed) {
                result.add(new GeoServerRole(externalRole));
            } else if (gsRole != null) {
                result.add(new GeoServerRole(gsRole));
            }
        }
        return result;
    }
}
