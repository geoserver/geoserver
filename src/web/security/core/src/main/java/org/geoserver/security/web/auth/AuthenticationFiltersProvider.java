/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.web.SecurityNamedServiceProvider;

public class AuthenticationFiltersProvider
        extends SecurityNamedServiceProvider<SecurityAuthFilterConfig> {

    @Override
    protected List<SecurityAuthFilterConfig> getItems() {
        List<SecurityAuthFilterConfig> result = new ArrayList<SecurityAuthFilterConfig>();
        try {
            for (String name :
                    getSecurityManager().listFilters(GeoServerAuthenticationFilter.class)) {
                result.add((SecurityAuthFilterConfig) getSecurityManager().loadFilterConfig(name));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }
}
