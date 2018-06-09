/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Security provider for j2ee authentication
 *
 * @author mcr
 */
public class GeoServerJ2eeAuthenticationProvider extends AbstractFilterProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("j2eeAuthentication", J2eeAuthenticationFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerJ2eeAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerJ2eeAuthenticationFilter();
    }
}
