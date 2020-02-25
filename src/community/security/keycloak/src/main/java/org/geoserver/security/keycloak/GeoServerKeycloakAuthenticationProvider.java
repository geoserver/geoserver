/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import com.thoughtworks.xstream.XStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The provider creates {@link Filter} objects appropriate for authentication using Keycloak. It is
 * also responsible for providing/validating the configuration of these filters, and for
 * persisting/retrieving the configuration.
 */
public class GeoServerKeycloakAuthenticationProvider extends AbstractFilterProvider {

    private static final Logger LOG =
            Logging.getLogger(GeoServerKeycloakAuthenticationProvider.class);

    @Autowired
    public GeoServerKeycloakAuthenticationProvider() {
        // no-op
    }

    @Override
    public void configure(XStreamPersister xp) {
        LOG.log(Level.FINER, "GeoServerKeycloakAuthenticationProvider.configure ENTRY");
        super.configure(xp);
        XStream xs = xp.getXStream();
        xs.allowTypes(new Class[] {GeoServerKeycloakFilterConfig.class});
        xs.alias("keycloakAdapter", GeoServerKeycloakFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        LOG.log(Level.FINER, "GeoServerKeycloakAuthenticationProvider.getFilterClass ENTRY");
        return GeoServerKeycloakFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        LOG.log(Level.FINER, "GeoServerKeycloakAuthenticationProvider.createFilter ENTRY");
        return new GeoServerKeycloakFilter();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        LOG.log(
                Level.FINER,
                "GeoServerKeycloakAuthenticationProvider.createConfigurationValidator ENTRY");
        return new GeoServerKeycloakFilterConfigValidator(securityManager);
    }
}
