/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Security provider for auth-key authentication
 *
 * @author mcr
 */
public class GeoServerAuthenticationKeyProvider extends AbstractFilterProvider {
    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("authKeyAuthentication", AuthenticationKeyFilterConfig.class);
        // xp.getXStream().alias("authKeyRESTUserGroupService",
        // GeoServerRestUserGroupServiceConfig.class);
        xp.getXStream().alias("authKeyRESTRoleService", GeoServerRestRoleServiceConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerAuthenticationKeyFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerAuthenticationKeyFilter();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new AuthenticationKeyFilterConfigValidator(securityManager);
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return new GeoServerRestRoleService(config);
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return super.createUserGroupService(config);
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return GeoServerRestRoleService.class;
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return super.getUserGroupServiceClass();
    }

    @Override
    public boolean roleServiceNeedsLockProtection() {
        return false;
    }

    @Override
    public boolean userGroupServiceNeedsLockProtection() {
        return super.userGroupServiceNeedsLockProtection();
    }
}
