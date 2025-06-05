/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.validation.SecurityConfigException;

public class AuthenticationProviderHelper {

    private static final String USER_GROUP_SERVICE_NAME = "default";

    private final GeoServerSecurityManager securityManager;

    public AuthenticationProviderHelper(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    protected UsernamePasswordAuthenticationProviderConfig createUsernamePasswordAuthenticationProviderConfig(
            String name, boolean save) {
        try {
            UsernamePasswordAuthenticationProviderConfig config = new UsernamePasswordAuthenticationProviderConfig();
            config.setName(name);
            config.setUserGroupServiceName(USER_GROUP_SERVICE_NAME);
            config.setClassName(UsernamePasswordAuthenticationProvider.class.getName());

            if (save) {
                securityManager.saveAuthenticationProvider(config);
                securityManager.reload();
            }
            return config;
        } catch (IOException | SecurityConfigException e) {
            fail("Unexpected exception " + e.getMessage());
            return null;
        }
    }

    protected static void checkProvideUsernamePasswordAuthenticationProvider(
            UsernamePasswordAuthenticationProviderConfig provider, SecurityAuthProviderConfig config) {
        assertNotNull(config);
        assertEquals(provider.getUserGroupServiceName(), config.getUserGroupServiceName());
    }
}
