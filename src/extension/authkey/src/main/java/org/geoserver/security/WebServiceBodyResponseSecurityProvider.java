/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geotools.util.logging.Logging;

/**
 * Security Provider to support {@linkplain WebServiceBodyResponseUserGroupService}
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class WebServiceBodyResponseSecurityProvider extends GeoServerSecurityProvider {

    static final Logger LOGGER = Logging.getLogger("org.geoserver.security");

    GeoServerSecurityManager securityManager;

    public WebServiceBodyResponseSecurityProvider(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void configure(XStreamPersister xp) {
        xp.getXStream().alias("webServiceBody", WebServiceBodyResponseSecurityProviderConfig.class);
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return WebServiceBodyResponseUserGroupService.class;
    }

    @Override
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {
        return null;
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return null;
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return null;
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return new WebServiceBodyResponseUserGroupService(config);
    }
}
