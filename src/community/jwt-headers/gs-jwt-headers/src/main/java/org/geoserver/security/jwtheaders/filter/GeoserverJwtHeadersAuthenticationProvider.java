/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * FilterProvider from JWT Headers filter. This sets up the infrastructure for the JWT Header
 * filters.
 */
public class GeoserverJwtHeadersAuthenticationProvider extends AbstractFilterProvider {

    private static final Logger LOG =
            Logging.getLogger(GeoserverJwtHeadersAuthenticationProvider.class);

    @Autowired
    public GeoserverJwtHeadersAuthenticationProvider() {}

    @Override
    public void configure(XStreamPersister xp) {
        LOG.log(Level.FINER, "GeoserverJwtHeadersAuthenticationProvider.configure ENTRY");
        super.configure(xp);
        xp.getXStream().alias("JwtHeadersAuthentication", GeoServerJwtHeadersFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        LOG.log(Level.FINER, "GeoserverJwtHeadersAuthenticationProvider.getFilterClass ENTRY");
        return GeoServerJwtHeadersFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        LOG.log(Level.FINER, "GeoserverJwtHeadersAuthenticationProvider.createFilter ENTRY");
        return new GeoServerJwtHeadersFilter();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        LOG.log(
                Level.FINER,
                "GeoserverJwtHeadersAuthenticationProvider.createConfigurationValidator ENTRY");
        return new GeoServerJwtHeadersFilterConfigValidator(securityManager);
    }
}
