/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.DelegatingAuthenticationProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
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
        // TODO Auto-generated method stub
        super.initializeFromConfig(config);
    }
    
    /** TODO, Justin
     *  Added the GeoServerRole.AUTHENTICATED_ROLE system role 
     */
    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {
        
        UsernamePasswordAuthenticationToken  auth = 
                (UsernamePasswordAuthenticationToken) super.authenticate(authentication, request);
        
        if (auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE)==false) {
            List<GrantedAuthority> roles= new ArrayList<GrantedAuthority>();
            roles.addAll(auth.getAuthorities());
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    auth.getPrincipal(), auth.getCredentials(),roles);
            newAuth.setDetails(auth.getDetails());
            return newAuth;
        }
        return auth;
    }


}
