/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geotools.util.logging.Logging;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.UserDetailsServiceLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.NestedLdapAuthoritiesPopulator;

/**
 * LDAP security provider.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPSecurityProvider extends GeoServerSecurityProvider {

    static final Logger LOGGER = Logging.getLogger("org.geoserver.security.ldap");

    GeoServerSecurityManager securityManager;

    public LDAPSecurityProvider(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void configure(XStreamPersister xp) {
        xp.getXStream().alias("ldap", LDAPSecurityServiceConfig.class);
    }

    @Override
    public Class<LDAPAuthenticationProvider> getAuthenticationProviderClass() {
        return LDAPAuthenticationProvider.class;
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return LDAPUserGroupService.class;
    }

    @Override
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {
        LDAPSecurityServiceConfig ldapConfig = (LDAPSecurityServiceConfig) config;

        LdapContextSource ldapContext = LDAPUtils.createLdapContext(ldapConfig);

        GeoserverLdapBindAuthenticator authenticator =
                new GeoserverLdapBindAuthenticator(ldapContext);

        // authenticate and extract user using a filter and an optional username
        // format
        authenticator.setUserFilter(ldapConfig.getUserFilter());
        authenticator.setUserFormat(ldapConfig.getUserFormat());

        // authenticate and extract user using a distinguished name
        if (ldapConfig.getUserDnPattern() != null) {
            authenticator.setUserDnPatterns(new String[] {ldapConfig.getUserDnPattern()});
        }

        LdapAuthoritiesPopulator authPopulator = null;
        LdapAuthenticationProvider provider = null;
        String ugServiceName = ldapConfig.getUserGroupServiceName();
        if (ugServiceName != null) {
            // use local user group service for loading authorities
            GeoServerUserGroupService ugService;
            try {
                ugService = securityManager.loadUserGroupService(ugServiceName);
                authPopulator = new UserDetailsServiceLdapAuthoritiesPopulator(ugService);
                provider = new LdapAuthenticationProvider(authenticator, authPopulator);
            } catch (IOException e) {
                LOGGER.log(
                        Level.SEVERE,
                        String.format(
                                "Unable to load user group service '%s', "
                                        + "will use LDAP server for calculating roles",
                                ugServiceName),
                        e);
            }
        }

        if (authPopulator == null) {
            // fall back to looking up roles via LDAP server, choosing
            // between default and binding populator
            if (ldapConfig.isBindBeforeGroupSearch()) {
                authPopulator =
                        new BindingLdapAuthoritiesPopulator(
                                ldapContext, ldapConfig.getGroupSearchBase());
                // set hierarchical configurations
                BindingLdapAuthoritiesPopulator bindPopulator =
                        (BindingLdapAuthoritiesPopulator) authPopulator;
                bindPopulator.setUseNestedParentGroups(ldapConfig.isUseNestedParentGroups());
                bindPopulator.setMaxGroupSearchLevel(ldapConfig.getMaxGroupSearchLevel());
                bindPopulator.setNestedGroupSearchFilter(ldapConfig.getNestedGroupSearchFilter());

                if (ldapConfig.getGroupSearchFilter() != null) {
                    ((BindingLdapAuthoritiesPopulator) authPopulator)
                            .setGroupSearchFilter(ldapConfig.getGroupSearchFilter());
                }
                provider =
                        new LdapAuthenticationProvider(authenticator, authPopulator) {
                            /**
                             * We need to give authoritiesPopulator both username and password, so
                             * it can bind to the LDAP server.
                             */
                            @Override
                            protected Collection<? extends GrantedAuthority> loadUserAuthorities(
                                    DirContextOperations userData,
                                    String username,
                                    String password) {
                                return ((BindingLdapAuthoritiesPopulator) getAuthoritiesPopulator())
                                        .getGrantedAuthorities(userData, username, password);
                            }
                        };
            } else {
                ldapContext.setAnonymousReadOnly(true);
                // is hierarchical nested groups implementation required?
                if (ldapConfig.isUseNestedParentGroups()) {
                    // use nested implementation for nested groups support
                    authPopulator =
                            new NestedLdapAuthoritiesPopulator(
                                    ldapContext, ldapConfig.getGroupSearchBase());
                    ((NestedLdapAuthoritiesPopulator) authPopulator)
                            .setMaxSearchDepth(ldapConfig.getMaxGroupSearchLevel());
                } else {
                    // no hierarchical groups required, use default implementation
                    authPopulator =
                            new DefaultLdapAuthoritiesPopulator(
                                    ldapContext, ldapConfig.getGroupSearchBase());
                }

                if (ldapConfig.getGroupSearchFilter() != null) {
                    ((DefaultLdapAuthoritiesPopulator) authPopulator)
                            .setGroupSearchFilter(ldapConfig.getGroupSearchFilter());
                }
                provider = new LdapAuthenticationProvider(authenticator, authPopulator);
            }
        }

        return new LDAPAuthenticationProvider(
                provider, ldapConfig.getAdminGroup(), ldapConfig.getGroupAdminGroup());
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return LDAPRoleService.class;
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return new LDAPRoleService();
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return new LDAPUserGroupService(config);
    }
}
