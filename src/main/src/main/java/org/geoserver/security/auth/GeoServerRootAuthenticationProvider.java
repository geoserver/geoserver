/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import java.util.ArrayList;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.SecurityUtils;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

/**
 * An authentication provider for the superuser called {@link #ROOTUSERNAME}. This user hat the
 * administrator role {@link GeoServerRole#ADMIN_ROLE} No other users are authenticated.
 *
 * <p>The password is checked using {@link GeoServerSecurityManager#checkMasterPassword(String)}
 *
 * <p>If the password does not match, NO {@link BadCredentialsException} is thrown. Maybe there is a
 * user in one of the {@link GeoServerUserGroupService} objects with the same name.
 *
 * @author christian
 */
public class GeoServerRootAuthenticationProvider extends GeoServerAuthenticationProvider {

    public GeoServerRootAuthenticationProvider() {
        super();
        setName("root");
    }

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {

        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) authentication;

        // check if name is root
        if (GeoServerUser.ROOT_USERNAME.equals(SecurityUtils.getUsername(token.getPrincipal()))
                == false) return null;

        // check password
        if (token.getCredentials() != null) {
            if (getSecurityManager().checkMasterPassword(token.getCredentials().toString())) {
                Collection<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
                roles.add(GeoServerRole.ADMIN_ROLE);

                UsernamePasswordAuthenticationToken result =
                        new UsernamePasswordAuthenticationToken(
                                GeoServerUser.ROOT_USERNAME, null, roles);
                result.setDetails(token.getDetails());
                return result;
            }
        }

        // not BadCredentialException is thrown, maybe there is another user with
        // the same name
        log(new BadCredentialsException("Bad credentials for: " + token.getPrincipal()));
        return null;
    }
}
