/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.io.IOException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.validation.SecurityConfigException;

public class SecurityConfigFilterHelper {
    private final GeoServerSecurityManager securityManager;

    public SecurityConfigFilterHelper(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public BasicAuthenticationFilterConfig createBasciAuthFilterConfig(String name, boolean save) {
        BasicAuthenticationFilterConfig basicAuthConfig = new BasicAuthenticationFilterConfig();
        basicAuthConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        basicAuthConfig.setUseRememberMe(false);
        basicAuthConfig.setName(name);

        try {
            if (save) {
                securityManager.saveFilter(basicAuthConfig);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return basicAuthConfig;
    }

    public LogoutFilterConfig createLogoutFilterConfig(String name, boolean save)
            throws IOException, SecurityConfigException {
        LogoutFilterConfig logoutConfig = new LogoutFilterConfig();
        logoutConfig.setClassName(GeoServerLogoutFilter.class.getName());
        logoutConfig.setName(name);
        logoutConfig.setRedirectURL(GeoServerLogoutFilter.URL_AFTER_LOGOUT);

        if (save) {
            securityManager.saveFilter(logoutConfig);
        }
        return logoutConfig;
    }
}
