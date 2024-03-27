/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.geotools.util.logging.Logging;

/** Validate the configuration. */
public class GeoServerJwtHeadersFilterConfigValidator extends FilterConfigValidator {

    private static final Logger LOG =
            Logging.getLogger(GeoServerJwtHeadersFilterConfigValidator.class);

    /**
     * Default constructor.
     *
     * @param securityManager the active security manager for the context
     */
    public GeoServerJwtHeadersFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    /** Validates the configuration type and content. */
    @Override
    public void validateFilterConfig(SecurityNamedServiceConfig config)
            throws FilterConfigException {
        if (config instanceof GeoServerJwtHeadersFilterConfig) {
            validateGeoServerJwtHeadersFilterConfig((GeoServerJwtHeadersFilterConfig) config);
            super.validateFilterConfig(config);
        } else {
            throw new FilterConfigException(
                    FilterConfigException.CLASS_WRONG_TYPE_$2,
                    "configuration type is not appropriate for the requested filter type",
                    config.getClass().getName(),
                    GeoServerJwtHeadersFilterConfig.class.getName());
        }
    }

    public void validateGeoServerJwtHeadersFilterConfig(GeoServerJwtHeadersFilterConfig config)
            throws FilterConfigException {
        try {
            // todo
        } catch (RuntimeException e) {
            throw new FilterConfigException(null, e.getLocalizedMessage());
        }
    }
}
