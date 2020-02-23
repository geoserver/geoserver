/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.xml;

import java.io.IOException;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Security provider for default XML-based implementation.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class XMLSecurityProvider extends GeoServerSecurityProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("userGroupService", XMLUserGroupServiceConfig.class);
        xp.getXStream().alias("roleService", XMLRoleServiceConfig.class);
        xp.getXStream().alias("passwordPolicy", PasswordPolicyConfig.class);
        xp.getXStream()
                .alias("usernamePassword", UsernamePasswordAuthenticationProviderConfig.class);
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return XMLUserGroupService.class;
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return new XMLUserGroupService();
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return XMLRoleService.class;
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return new XMLRoleService();
    }

    /** Create the standard password validator */
    public PasswordValidator createPasswordValidator(
            PasswordPolicyConfig config, GeoServerSecurityManager securityManager) {
        return new PasswordValidatorImpl(securityManager);
    }

    /**
     * Returns the specific class of the password validator created by {@link
     * #createPasswordValidator(PasswordPolicyConfig)}.
     *
     * <p>If the extension does not provide a user group service this method should simply return
     * <code>null</code>.
     */
    public Class<? extends PasswordValidator> getPasswordValidatorClass() {
        return PasswordValidatorImpl.class;
    }

    /**
     * Creates an authentication provider.
     *
     * <p>If the extension does not provide an authentication provider this method should simply
     * return <code>null</code>.
     */
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {
        return new UsernamePasswordAuthenticationProvider();
    }

    /**
     * Returns the concrete class of authentication provider created by {@link
     * #createAuthenticationProvider(SecurityNamedServiceConfig)}.
     *
     * <p>If the extension does not provide an authentication provider this method should simply
     * return <code>null</code>.
     */
    public Class<? extends GeoServerAuthenticationProvider> getAuthenticationProviderClass() {
        return UsernamePasswordAuthenticationProvider.class;
    }

    @Override
    public boolean roleServiceNeedsLockProtection() {
        return true;
    }

    @Override
    public boolean userGroupServiceNeedsLockProtection() {
        return true;
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new XMLSecurityConfigValidator(securityManager);
    }
}
