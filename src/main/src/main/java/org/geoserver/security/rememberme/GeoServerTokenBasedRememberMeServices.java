/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rememberme;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.filter.GeoServerWebAuthenticationDetails;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.rememberme.RememberMeUserDetailsService.RememberMeUserDetails;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/**
 * Token based remember me services that appends a user group service name to generated tokens.
 *
 * <p>The user group service name is used by {@link RememberMeUserDetailsService} in order to
 * delegate to the proper user group service.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

    public GeoServerTokenBasedRememberMeServices(
            String key, UserDetailsService userDetailsService) {
        super(key, userDetailsService);
    }

    /** Create the signature by removing the user group service name suffix from the user name */
    @Override
    protected String makeTokenSignature(long tokenExpiryTime, String username, String password) {

        Matcher m = RememberMeUserDetailsService.TOKEN_PATTERN.matcher(username);
        String uName;
        if (!m.matches()) {
            uName = username;
        } else {
            uName = m.group(1).replace("\\@", "@");
            // String service = m.group(2);
        }
        return super.makeTokenSignature(tokenExpiryTime, uName, password);
    }

    /** A proper {@link GeoServerWebAuthenticationDetails} object must be present */
    protected String retrieveUserName(Authentication authentication) {
        if (authentication.getDetails() instanceof GeoServerWebAuthenticationDetails) {
            String userGroupServiceName =
                    ((GeoServerWebAuthenticationDetails) authentication.getDetails())
                            .getUserGroupServiceName();
            if (userGroupServiceName == null || userGroupServiceName.trim().length() == 0)
                return ""; // no service specified --> no remember me
            return encode(super.retrieveUserName(authentication), userGroupServiceName);
        } else return ""; // no remember me feature without a user group service name
    };

    String encode(String username, String userGroupServiceName) {
        if (userGroupServiceName == null) {
            return username;
        }
        if (username.endsWith("@" + userGroupServiceName)) {
            return username;
        }

        // escape any @ symboles present in the username, and append '@userGroupServiceName')
        return username.replace("@", "\\@") + "@" + userGroupServiceName;
    }

    @Override
    protected Authentication createSuccessfulAuthentication(
            HttpServletRequest request, UserDetails user) {
        if (user instanceof RememberMeUserDetails)
            user = ((RememberMeUserDetails) user).getWrappedObject();

        Collection<GrantedAuthority> roles = new HashSet<GrantedAuthority>();
        if (user.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE)) {
            roles.addAll(user.getAuthorities());
        } else {
            roles = new HashSet<GrantedAuthority>();
            roles.addAll(user.getAuthorities());
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        }
        RememberMeAuthenticationToken auth =
                new RememberMeAuthenticationToken(getKey(), user, roles);
        auth.setDetails(getAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }
}
