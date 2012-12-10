/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.DelegatingAuthenticationProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

/**
 * LDAP authentication provider.
 * <p>
 * This class doesn't really do anything, it delegates fully to {@link LdapAuthenticationProvider}.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPAuthenticationProvider extends
        DelegatingAuthenticationProvider {

    public LDAPAuthenticationProvider(AuthenticationProvider authProvider) {
        super(authProvider);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config)
            throws IOException {
        super.initializeFromConfig(config);
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication,
            HttpServletRequest request) throws AuthenticationException {
     
        UsernamePasswordAuthenticationToken  auth = 
                (UsernamePasswordAuthenticationToken) super.doAuthenticate(authentication, request);
        
        if (auth==null) return null; // next provider

        Set<GeoServerRole> roles = new HashSet<GeoServerRole>();
        for (GrantedAuthority ga : auth.getAuthorities()) {
            roles.add(new GeoServerRole(ga.getAuthority()));
        }
        
        //map the roles to system roles
        new RoleCalculator(getSecurityManager().getActiveRoleService()).addMappedSystemRoles(roles);

        //add authenticated role
        if (!roles.contains(GeoServerRole.AUTHENTICATED_ROLE)) {
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        }

        auth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), auth.getCredentials(),roles);
        auth.setDetails(auth.getDetails());
        return auth;
    }


}
