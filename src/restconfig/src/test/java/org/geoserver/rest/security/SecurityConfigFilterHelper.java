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

    public BasicAuthenticationFilterConfig createBasciAuthFilterConfig(String name, boolean save)
            throws IOException, SecurityConfigException {
        BasicAuthenticationFilterConfig basicAuthConfig = new BasicAuthenticationFilterConfig();
        basicAuthConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        basicAuthConfig.setUseRememberMe(false);
        basicAuthConfig.setName(name);

        if (save) {
            securityManager.saveFilter(basicAuthConfig);
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
