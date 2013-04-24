/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
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

	// optional role to be remapped to ROLE_ADMINISTRATOR
	private String adminRole;
	
    public LDAPAuthenticationProvider(AuthenticationProvider authProvider, String adminRole) {
        super(authProvider);
        this.adminRole = adminRole;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config)
            throws IOException {
        super.initializeFromConfig(config);
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication,
            HttpServletRequest request) throws AuthenticationException {
     
		UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) super
				.doAuthenticate(authentication, request);

		if (auth == null)
			return null; // next provider

		boolean hasNoAuthenticatedRole = auth.getAuthorities().contains(
				GeoServerRole.AUTHENTICATED_ROLE) == false;
		boolean hasAdminRole = adminRole != null && !adminRole.equals("")
				&& !auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE);

		if (hasNoAuthenticatedRole || hasAdminRole) {
			List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
			roles.addAll(auth.getAuthorities());
			if (hasNoAuthenticatedRole) {
				roles.add(GeoServerRole.AUTHENTICATED_ROLE);
			}
			if (hasAdminRole) {
				for (GrantedAuthority authority : auth.getAuthorities()) {
					if (authority.getAuthority().equalsIgnoreCase(
							"ROLE_" + adminRole)) {
						roles.add(GeoServerRole.ADMIN_ROLE);
					}
				}
			}
			UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
					auth.getPrincipal(), auth.getCredentials(), roles);
			newAuth.setDetails(auth.getDetails());
			return newAuth;
		}
		return auth;
    }


}
