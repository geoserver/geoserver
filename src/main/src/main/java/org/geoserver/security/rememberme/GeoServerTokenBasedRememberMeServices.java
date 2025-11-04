/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rememberme;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.security.filter.GeoServerWebAuthenticationDetails;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.rememberme.RememberMeUserDetailsService.RememberMeUserDetails;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/**
 * Token based remember me services that appends a user group service name to generated tokens.
 *
 * <p>The user group service name is used by {@link RememberMeUserDetailsService} in order to delegate to the proper
 * user group service.
 *
 * <p>During logout, the cookie is remembered so it cannot be re-used. The prevents an accidental auto-login.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

    // remember-me tokens are valid for 2 weeks, so we expire after 2 weeks
    static Cache<String, String> unauthorizedRememberMeCookieCache =
            CacheBuilder.newBuilder().expireAfterWrite(14 * 24, TimeUnit.HOURS).build();

    public GeoServerTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService) {
        super(key, userDetailsService);
    }

    /**
     * We mark the token as invalid - see {link #decodeCookie()}
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param authentication the current principal details
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String rememberMeCookie = extractRememberMeCookie(request);

        // this will make the response send an empty remember-me cookie back
        super.logout(request, response, authentication);

        // mark that cookie as "do not allow"
        if (StringUtils.isNotBlank(rememberMeCookie)) {
            unauthorizedRememberMeCookieCache.put(rememberMeCookie, "");
        }
    }

    /**
     * Expired-token enabled decodeCookie().
     *
     * <p>If the cookie is no longer valid (i.e. user logged out), then this will return `new String[0]` and not let the
     * user login. See {link #logout()}
     *
     * @param cookieValue the value obtained from the submitted cookie
     * @return the array of tokens.
     * @throws InvalidCookieException see super.decodeCookie()
     */
    @Override
    protected String[] decodeCookie(String cookieValue) throws InvalidCookieException {
        if (StringUtils.isEmpty(cookieValue)) {
            return new String[0];
        }
        if (unauthorizedRememberMeCookieCache.getIfPresent(cookieValue) != null) {
            return new String[0];
        }
        return super.decodeCookie(cookieValue);
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
    @Override
    protected String retrieveUserName(Authentication authentication) {
        if (authentication.getDetails() instanceof GeoServerWebAuthenticationDetails) {
            String userGroupServiceName =
                    ((GeoServerWebAuthenticationDetails) authentication.getDetails()).getUserGroupServiceName();
            if (userGroupServiceName == null || userGroupServiceName.trim().isEmpty())
                return ""; // no service specified --> no remember me
            return encode(super.retrieveUserName(authentication), userGroupServiceName);
        } else return ""; // no remember me feature without a user group service name
    }

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
    protected Authentication createSuccessfulAuthentication(HttpServletRequest request, UserDetails user) {
        if (user instanceof RememberMeUserDetails details) user = details.getWrappedObject();

        Collection<GrantedAuthority> roles = new HashSet<>();
        if (user.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE)) {
            roles.addAll(user.getAuthorities());
        } else {
            roles = new HashSet<>();
            roles.addAll(user.getAuthorities());
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        }
        RememberMeAuthenticationToken auth = new RememberMeAuthenticationToken(getKey(), user, roles);
        auth.setDetails(getAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }
}
