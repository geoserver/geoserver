/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth.web;

import static org.geoserver.security.auth.web.SimpleWebAuthConfigException.INVALID_REGEX_EXPRESSION;
import static org.geoserver.security.auth.web.SimpleWebAuthConfigException.INVALID_TIMEOUT;
import static org.geoserver.security.auth.web.SimpleWebAuthConfigException.INVALID_WEB_SERVICE_URL;
import static org.geoserver.security.auth.web.SimpleWebAuthConfigException.NO_ROLE_SERVICE_SELECTED;
import static org.geoserver.security.auth.web.SimpleWebAuthConfigException.PLACE_HOLDERS_NOT_FOUND;
import static org.geoserver.security.validation.SecurityConfigException.ROLE_SERVICE_NOT_FOUND_$1;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;

public class SimpleWebAuthenticationConfigValidator extends SecurityConfigValidator {

    private static Logger LOGGER =
            Logger.getLogger(SimpleWebAuthenticationConfigValidator.class.getCanonicalName());

    Set<String> availableRoles;

    public SimpleWebAuthenticationConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
        try {
            availableRoles = securityManager.listRoleServices();
        } catch (IOException e) {
            LOGGER.log(
                    Level.SEVERE, "Error in SimpleWebAuthenticationConfigValidator constructor", e);
        }
    }

    @Override
    public void validate(SecurityAuthProviderConfig config) throws SecurityConfigException {
        super.validate(config);
        SimpleWebAuthenticationConfig webAuthConfig = (SimpleWebAuthenticationConfig) config;

        validateServiceURL(webAuthConfig);

        if (webAuthConfig
                .getAuthorizationOption()
                .equalsIgnoreCase(SimpleWebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_WEB))
            validateRolesRegex(webAuthConfig);
        else {
            if (webAuthConfig.getRoleServiceName() == null
                    || webAuthConfig.getRoleServiceName().isEmpty()) {
                // check if Role Service is selected
                throw new SimpleWebAuthConfigException(NO_ROLE_SERVICE_SELECTED, null);
            } else if (!availableRoles.contains(webAuthConfig.getRoleServiceName())) {
                // check if Role Service is available
                throw new SimpleWebAuthConfigException(
                        ROLE_SERVICE_NOT_FOUND_$1,
                        new Object[] {webAuthConfig.getRoleServiceName()});
            }
        }
    }

    private void validateServiceURL(SimpleWebAuthenticationConfig config)
            throws SecurityConfigException {
        String connectionURL = config.getConnectionURL();

        if (connectionURL == null || connectionURL.isEmpty() || !connectionURL.startsWith("http"))
            throw new SimpleWebAuthConfigException(
                    INVALID_WEB_SERVICE_URL, new Object[] {connectionURL});

        try {
            new URL(connectionURL);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new SimpleWebAuthConfigException(
                    INVALID_WEB_SERVICE_URL, new Object[] {connectionURL});
        }

        if (!connectionURL.startsWith("https"))
            LOGGER.warning("Use of HTTPS is highly recommended");

        // look for {user} and {password} if not set to use HEADER
        if (!config.isUseHeader()) {
            if (!(connectionURL
                            .toLowerCase()
                            .contains(
                                    SimpleWebAuthenticationConfig.URL_PLACEHOLDER_USER
                                            .toLowerCase())
                    && connectionURL
                            .toLowerCase()
                            .contains(
                                    SimpleWebAuthenticationConfig.URL_PLACEHOLDER_PASSWORD
                                            .toLowerCase()))) {
                throw new SimpleWebAuthConfigException(
                        PLACE_HOLDERS_NOT_FOUND,
                        new Object[] {
                            connectionURL,
                            SimpleWebAuthenticationConfig.URL_PLACEHOLDER_USER,
                            SimpleWebAuthenticationConfig.URL_PLACEHOLDER_PASSWORD
                        });
            }
        }

        // connection timeouts cannot be 0 or below
        if (config.getReadTimeoutOut() <= 0 || config.getConnectionTimeOut() <= 0)
            throw new SimpleWebAuthConfigException(
                    INVALID_TIMEOUT, new Object[] {config.getReadTimeoutOut()});
    }

    // regex
    private void validateRolesRegex(SimpleWebAuthenticationConfig config)
            throws SecurityConfigException {
        String rolesRegex = config.getRoleRegex();
        // its optional
        if (rolesRegex == null || rolesRegex.isEmpty()) return;
        // if set try to compile as a Pattern
        try {
            Pattern.compile(rolesRegex);
        } catch (Exception e) {
            throw new SimpleWebAuthConfigException(
                    INVALID_REGEX_EXPRESSION, new Object[] {rolesRegex});
        }
    }
}
