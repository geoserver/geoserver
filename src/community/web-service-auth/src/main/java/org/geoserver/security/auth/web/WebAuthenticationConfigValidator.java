/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth.web;

import static org.geoserver.security.auth.web.WebAuthConfigException.HTTP_CONNECTION_NOT_ALLOWED;
import static org.geoserver.security.auth.web.WebAuthConfigException.INVALID_REGEX_EXPRESSION;
import static org.geoserver.security.auth.web.WebAuthConfigException.INVALID_TIMEOUT;
import static org.geoserver.security.auth.web.WebAuthConfigException.INVALID_WEB_SERVICE_URL;
import static org.geoserver.security.auth.web.WebAuthConfigException.NO_ROLE_SERVICE_SELECTED;
import static org.geoserver.security.auth.web.WebAuthConfigException.PLACE_HOLDERS_NOT_FOUND;
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

public class WebAuthenticationConfigValidator extends SecurityConfigValidator {

    private static Logger LOGGER =
            Logger.getLogger(WebAuthenticationConfigValidator.class.getCanonicalName());

    Set<String> availableRoles;

    public WebAuthenticationConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
        try {
            availableRoles = securityManager.listRoleServices();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error in WebAuthenticationConfigValidator constructor", e);
        }
    }

    @Override
    public void validate(SecurityAuthProviderConfig config) throws SecurityConfigException {
        super.validate(config);
        WebAuthenticationConfig webAuthConfig = (WebAuthenticationConfig) config;

        validateServiceURL(webAuthConfig);

        if (webAuthConfig
                .getAuthorizationOption()
                .equalsIgnoreCase(WebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_WEB))
            validateRolesRegex(webAuthConfig);
        else {
            if (webAuthConfig.getRoleServiceName() == null
                    || webAuthConfig.getRoleServiceName().isEmpty()) {
                // check if Role Service is selected
                throw new WebAuthConfigException(NO_ROLE_SERVICE_SELECTED, null);
            } else if (!availableRoles.contains(webAuthConfig.getRoleServiceName())) {
                // check if Role Service is available
                throw new WebAuthConfigException(
                        ROLE_SERVICE_NOT_FOUND_$1,
                        new Object[] {webAuthConfig.getRoleServiceName()});
            }
        }
    }

    private void validateServiceURL(WebAuthenticationConfig config) throws SecurityConfigException {
        String connectionURL = config.getConnectionURL();

        if (connectionURL == null || connectionURL.isEmpty() || !connectionURL.startsWith("http"))
            throw new WebAuthConfigException(INVALID_WEB_SERVICE_URL, new Object[] {connectionURL});

        try {
            new URL(connectionURL);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new WebAuthConfigException(INVALID_WEB_SERVICE_URL, new Object[] {connectionURL});
        }

        if (!connectionURL.startsWith("https") && !config.isAllowHTTPConnection())
            throw new WebAuthConfigException(HTTP_CONNECTION_NOT_ALLOWED, new Object[] {});
        LOGGER.warning("Use of HTTPS is highly recommended");

        // look for {user} and {password} if not set to use HEADER
        if (!config.isUseHeader()) {
            if (!(connectionURL.contains(WebAuthenticationConfig.URL_PLACEHOLDER_USER)
                    && connectionURL.contains(WebAuthenticationConfig.URL_PLACEHOLDER_PASSWORD))) {
                throw new WebAuthConfigException(
                        PLACE_HOLDERS_NOT_FOUND,
                        new Object[] {
                            connectionURL,
                            WebAuthenticationConfig.URL_PLACEHOLDER_USER,
                            WebAuthenticationConfig.URL_PLACEHOLDER_PASSWORD
                        });
            }
        }

        // connection timeouts cannot be 0 or below
        if (config.getReadTimeoutOut() <= 0 || config.getConnectionTimeOut() <= 0)
            throw new WebAuthConfigException(
                    INVALID_TIMEOUT, new Object[] {config.getReadTimeoutOut()});
    }

    // regex
    private void validateRolesRegex(WebAuthenticationConfig config) throws SecurityConfigException {
        String rolesRegex = config.getRoleRegex();
        // its optional
        if (rolesRegex == null || rolesRegex.isEmpty()) return;
        // if set try to compile as a Pattern
        try {
            Pattern.compile(rolesRegex);
        } catch (Exception e) {
            throw new WebAuthConfigException(INVALID_REGEX_EXPRESSION, new Object[] {rolesRegex});
        }
    }
}
