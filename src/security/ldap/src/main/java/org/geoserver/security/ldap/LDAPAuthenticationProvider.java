/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

/**
 * LDAP authentication provider.
 *
 * <p>This class doesn't really do anything, it delegates fully to {@link
 * LdapAuthenticationProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPAuthenticationProvider extends DelegatingAuthenticationProvider {

    // optional role to be remapped to ROLE_ADMINISTRATOR
    private String adminRole;

    // optional role to be remapped to ROLE_ADMINISTRATOR
    private String groupAdminRole;

    public LDAPAuthenticationProvider(
            AuthenticationProvider authProvider, String adminRole, String groupAdminRole) {
        super(authProvider);
        this.adminRole = adminRole;
        this.groupAdminRole = groupAdminRole;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
    }

    @Override
    protected Authentication doAuthenticate(
            Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) super.doAuthenticate(authentication, request);

        if (auth == null) return null; // next provider

        Set<GrantedAuthority> roles = new HashSet<GrantedAuthority>();
        roles.addAll(auth.getAuthorities());

        // add geoserver roles
        if (getSecurityManager() != null) {
            RoleCalculator calc = new RoleCalculator(getSecurityManager().getActiveRoleService());
            try {
                roles.addAll(calc.calculateRoles(new GeoServerUser(auth.getName())));
            } catch (IOException e) {
                throw new AuthenticationServiceException(e.getLocalizedMessage(), e);
            }
        }

        if (!auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE)) {
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        }
        if (adminRole != null
                && !adminRole.equals("")
                && !roles.contains(GeoServerRole.ADMIN_ROLE)) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_" + adminRole)) {
                    roles.add(GeoServerRole.ADMIN_ROLE);
                    break;
                }
            }
        }
        if (groupAdminRole != null
                && !groupAdminRole.equals("")
                && !roles.contains(GeoServerRole.GROUP_ADMIN_ROLE)) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_" + groupAdminRole)) {
                    roles.add(GeoServerRole.GROUP_ADMIN_ROLE);
                    break;
                }
            }
        }
        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(
                        auth.getPrincipal(), auth.getCredentials(), roles);
        newAuth.setDetails(auth.getDetails());
        return newAuth;
    }
}
