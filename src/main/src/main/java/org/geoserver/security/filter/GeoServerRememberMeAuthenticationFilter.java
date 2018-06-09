/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;

/**
 * Named RemeberMe Authentication Filter
 *
 * @author mcr
 */
public class GeoServerRememberMeAuthenticationFilter extends GeoServerCompositeFilter
        implements GeoServerAuthenticationFilter {

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        //       not needed at the moment
        //        RememberMeAuthenticationFilterConfig authConfig =
        //                (RememberMeAuthenticationFilterConfig) config;

        GeoServerSecurityManager secMgr = getSecurityManager();
        RememberMeAuthenticationFilter filter =
                new RememberMeAuthenticationFilter(
                        secMgr.authenticationManager(), secMgr.getRememberMeService());
        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml() */
    @Override
    public boolean applicableForHtml() {
        return true;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices() */
    @Override
    public boolean applicableForServices() {
        return false;
    }
}
