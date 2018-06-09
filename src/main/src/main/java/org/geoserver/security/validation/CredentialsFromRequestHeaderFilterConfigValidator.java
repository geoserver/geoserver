/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.validation;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.CredentialsFromRequestHeaderFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Validates {@link CredentialsFromRequestHeaderFilterConfig} objects.
 *
 * @author Lorenzo Natali, GeoSolutions
 * @author Mauro Bartolomeoli, GeoSolutions
 */
public class CredentialsFromRequestHeaderFilterConfigValidator extends FilterConfigValidator {

    public CredentialsFromRequestHeaderFilterConfigValidator(
            GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    public void validateFilterConfig(SecurityNamedServiceConfig config)
            throws FilterConfigException {

        if (config instanceof CredentialsFromRequestHeaderFilterConfig)
            validateFilterConfig((CredentialsFromRequestHeaderFilterConfig) config);
        else super.validateFilterConfig(config);
    }

    public void validateFilterConfig(CredentialsFromRequestHeaderFilterConfig config)
            throws FilterConfigException {
        if (config.getUserNameHeaderName() == null || "".equals(config.getUserNameHeaderName())) {
            throw new CredentialsFromRequestHeaderFilterConfigException(
                    CredentialsFromRequestHeaderFilterConfigException.USERNAME_HEADER_REQUIRED,
                    null);
        }
        if (config.getUserNameRegex() == null || "".equals(config.getUserNameRegex())) {
            throw new CredentialsFromRequestHeaderFilterConfigException(
                    CredentialsFromRequestHeaderFilterConfigException.USERNAME_REGEX_REQUIRED,
                    null);
        }
        if (config.getPasswordHeaderName() == null || "".equals(config.getPasswordHeaderName())) {
            throw new CredentialsFromRequestHeaderFilterConfigException(
                    CredentialsFromRequestHeaderFilterConfigException.PASSWORD_HEADER_REQUIRED,
                    null);
        }
        if (config.getPasswordRegex() == null || "".equals(config.getPasswordRegex())) {
            throw new CredentialsFromRequestHeaderFilterConfigException(
                    CredentialsFromRequestHeaderFilterConfigException.PASSWORD_REGEX_REQUIRED,
                    null);
        }
    }
}
