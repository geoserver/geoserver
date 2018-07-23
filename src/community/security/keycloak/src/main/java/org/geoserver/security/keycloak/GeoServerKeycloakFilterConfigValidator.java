/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.geotools.util.logging.Logging;
import org.keycloak.adapters.KeycloakDeploymentBuilder;

/** Validates {@link GeoServerKeycloakFilterConfig} instances. */
public class GeoServerKeycloakFilterConfigValidator extends FilterConfigValidator {

    private static final Logger LOG =
            Logging.getLogger(GeoServerKeycloakFilterConfigValidator.class);

    /**
     * Default constructor.
     *
     * @param securityManager the active security manager for the context
     */
    public GeoServerKeycloakFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
        // no-op
    }

    /** Validates the configuration type and content. */
    @Override
    public void validateFilterConfig(SecurityNamedServiceConfig config)
            throws FilterConfigException {
        LOG.log(Level.FINER, "GeoServerKeycloakFilterConfigValidator.validateFilterConfig ENTRY");
        if (config instanceof GeoServerKeycloakFilterConfig) {
            LOG.log(Level.FINE, "valid config type");
            validateKeycloakConfig((GeoServerKeycloakFilterConfig) config);
            super.validateFilterConfig(config);
        } else {
            LOG.log(Level.FINE, "invalid config type");
            throw new FilterConfigException(
                    FilterConfigException.CLASS_WRONG_TYPE_$2,
                    "configuration type is not appropriate for the requested filter type",
                    config.getClass().getName(),
                    GeoServerKeycloakFilterConfig.class.getName());
        }
    }

    /**
     * Validates the configuration content. This builds a dummy deployment, and recasts and
     * exceptions so that GeoServer can process them as security-related.
     *
     * @param config the configuration to validate
     * @throws FilterConfigException if the configuration is invalid
     */
    public void validateKeycloakConfig(GeoServerKeycloakFilterConfig config)
            throws FilterConfigException {
        try {
            KeycloakDeploymentBuilder.build(config.readAdapterConfig());
            LOG.log(Level.FINE, "valid Keycloak config");
        } catch (RuntimeException | IOException e) {
            LOG.log(Level.FINE, "invalid Keycloak config", e);
            throw new FilterConfigException(null, e.getLocalizedMessage());
        }
    }
}
