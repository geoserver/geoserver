/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Security provider for basic auth
 *
 * @author mcr
 */
public class GeoServerDigestAuthenticationProvider extends AbstractFilterProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("digiestAuthentication", DigestAuthenticationFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerDigestAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerDigestAuthenticationFilter();
    }
}
