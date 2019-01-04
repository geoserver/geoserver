/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.CredentialsFromRequestHeaderFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.CredentialsFromRequestHeaderFilterConfigValidator;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Security provider to extract user credentials (username and password) from Request Headers in a
 * configurable way.
 *
 * @author Lorenzo Natali, GeoSolutions
 * @author Mauro Bartolomeoli, GeoSolutions
 */
public class GeoServerCredentialsFromRequestHeaderProvider extends AbstractFilterProvider {
    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream()
                .alias(
                        "credentialsFromRequestHeaderAuthentication",
                        CredentialsFromRequestHeaderFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerCredentialsFromRequestHeaderFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerCredentialsFromRequestHeaderFilter();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new CredentialsFromRequestHeaderFilterConfigValidator(securityManager);
    }
}
