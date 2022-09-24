/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.spring.security;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.GeoServerApplication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Holds information about the user session with GeoServer, {@link #get()} to access user session
 * for the current thread.
 *
 * <p>This session object acts as a facade providing access to authentication, username and user's
 * admin privileges.
 */
@SuppressWarnings("serial")
public class GeoServerSession extends WebSession {
    public GeoServerSession(Request request) {
        super(request);
    }

    public static GeoServerSession get() {
        return (GeoServerSession) Session.get();
    }

    /**
     * Spring authentication, or {@code null} if not set or anonymous.
     *
     * @return spring authentication, or {@code null} if not set or anonymous.
     */
    public Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getAuthorities().size() == 1
                && "ROLE_ANONYMOUS".equals(auth.getAuthorities().iterator().next().getAuthority()))
            return null;

        return auth;
    }

    /**
     * Logged in user name, or {@code Nobody} if unknown or anonymous.
     *
     * @return User name, or {@code Nobody} if unknonw or anonymous.
     */
    public String getUsername() {
        Authentication user = getAuthentication();

        boolean anonymous = user == null || user instanceof AnonymousAuthenticationToken;

        return anonymous ? "anonymous" : user.getName();
    }

    /**
     * Checks if the current user is authenticated and is the administrator.
     *
     * @return {@code true} if current user authenticated and is the administrator
     */
    public boolean isAdmin() {
        Authentication auth = getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return false;
        } else {
            GeoServerSecurityManager securityManager =
                    GeoServerApplication.get().getSecurityManager();
            return securityManager.checkAuthenticationForAdminRole(auth);
        }
    }
}
