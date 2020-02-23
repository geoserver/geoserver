/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Abstract base class for pre-authentication filters
 *
 * @author christian
 */
public abstract class GeoServerPreAuthenticationFilter extends GeoServerSecurityFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter {

    private AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails>
            authenticationDetailsSource = new WebAuthenticationDetailsSource();
    protected AuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        aep = new Http403ForbiddenEntryPoint();
    }

    /** Try to authenticate if there is no authenticated principal */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String cacheKey = authenticateFromCache(this, (HttpServletRequest) request);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response);

            Authentication postAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (postAuthentication != null && cacheKey != null) {
                if (cacheAuthentication(postAuthentication, (HttpServletRequest) request)) {
                    getSecurityManager()
                            .getAuthenticationCache()
                            .put(getName(), cacheKey, postAuthentication);
                }
            }
        }

        request.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        chain.doFilter(request, response);
    }

    /**
     * subclasses should return the principal, <code>null</code> if no principal was authenticated
     */
    protected abstract String getPreAuthenticatedPrincipal(HttpServletRequest request);

    /**
     * subclasses should return the roles for the principal obtained by {@link
     * #getPreAuthenticatedPrincipal(HttpServletRequest)}
     */
    protected abstract Collection<GeoServerRole> getRoles(
            HttpServletRequest request, String principal) throws IOException;

    /**
     * Try to authenticate and adds {@link GeoServerRole#AUTHENTICATED_ROLE} Takes care of the
     * special user named {@link GeoServerUser#ROOT_USERNAME}
     */
    protected void doAuthenticate(HttpServletRequest request, HttpServletResponse response) {

        String principal = getPreAuthenticatedPrincipal(request);
        if (principal == null || principal.trim().length() == 0) {
            return;
        }

        LOGGER.log(
                Level.FINE,
                "preAuthenticatedPrincipal = " + principal + ", trying to authenticate");

        PreAuthenticatedAuthenticationToken result = null;
        if (GeoServerUser.ROOT_USERNAME.equals(principal)) {
            result =
                    new PreAuthenticatedAuthenticationToken(
                            principal, null, Collections.singleton(GeoServerRole.ADMIN_ROLE));
        } else {
            Collection<GeoServerRole> roles = null;
            try {
                roles = getRoles(request, principal);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE) == false)
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            result = new PreAuthenticatedAuthenticationToken(principal, null, roles);
        }

        result.setDetails(authenticationDetailsSource.buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(result);
    }

    public AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails>
            getAuthenticationDetailsSource() {
        return authenticationDetailsSource;
    }

    public void setAuthenticationDetailsSource(
            AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails>
                    authenticationDetailsSource) {
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    protected boolean cacheAuthentication(Authentication auth, HttpServletRequest request) {
        // only cache if no HTTP session is available
        return request.getSession(false) == null;
    }

    @Override
    public String getCacheKey(HttpServletRequest request) {

        if (request.getSession(false) != null) // no caching if there is an HTTP session
        return null;

        String retval = getPreAuthenticatedPrincipal(request);
        if (GeoServerUser.ROOT_USERNAME.equals(retval)) return null;
        return retval;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml() */
    @Override
    public boolean applicableForHtml() {
        return true;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices() */
    @Override
    public boolean applicableForServices() {
        return true;
    }
}
