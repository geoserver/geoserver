/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geotools.util.logging.Logging;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.SpringSecurityAuthenticationSource;
import org.springframework.security.ldap.authentication.UserDetailsServiceLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

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
    public GeoServerAuthenticationProvider createAuthenticationProvider(SecurityNamedServiceConfig config) {
        LDAPSecurityServiceConfig ldapConfig = (LDAPSecurityServiceConfig) config;
        
        DefaultSpringSecurityContextSource ldapContext = 
                new DefaultSpringSecurityContextSource(ldapConfig.getServerURL());
        ldapContext.setCacheEnvironmentProperties(false);
        ldapContext.setAuthenticationSource(new SpringSecurityAuthenticationSource());
        
        if (ldapConfig.isUseTLS()) {
            //TLS does not play nicely with pooled connections 
            ldapContext.setPooled(false);

            DefaultTlsDirContextAuthenticationStrategy tls = 
                new DefaultTlsDirContextAuthenticationStrategy();
            tls.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            ldapContext.setAuthenticationStrategy(tls);
        }

        BindAuthenticator authenticator = new BindAuthenticator(ldapContext);
        authenticator.setUserDnPatterns(new String[]{ldapConfig.getUserDnPattern()});

        LdapAuthoritiesPopulator authPopulator = null;
        String ugServiceName = ldapConfig.getUserGroupServiceName();
        if (ugServiceName != null) {
            //use local user group service for loading authorities 
            GeoServerUserGroupService ugService;
            try {
                ugService = securityManager.loadUserGroupService(ugServiceName);
                authPopulator = new UserDetailsServiceLdapAuthoritiesPopulator(ugService);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, String.format("Unable to load user group service '%s', "
                    + "will use LDAP server for calculating roles", ugServiceName), e); 
            }
        }

        if (authPopulator == null) {
            //fall back to looking up roles via LDAP server
            authPopulator =
              new DefaultLdapAuthoritiesPopulator(ldapContext, ldapConfig.getGroupSearchBase());
            if (ldapConfig.getGroupSearchFilter() != null) {
                ((DefaultLdapAuthoritiesPopulator)authPopulator).setGroupSearchFilter(ldapConfig.getGroupSearchFilter());
            }
        }

        return new LDAPAuthenticationProvider(new LdapAuthenticationProvider(authenticator, authPopulator));
    }
}
