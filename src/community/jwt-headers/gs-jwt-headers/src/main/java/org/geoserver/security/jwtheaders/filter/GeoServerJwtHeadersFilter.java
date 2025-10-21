/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.jwtheaders.filter.details.JwtHeadersWebAuthDetailsSource;
import org.geoserver.security.jwtheaders.filter.details.JwtHeadersWebAuthenticationDetails;
import org.geoserver.security.jwtheaders.roles.JwtHeadersRolesExtractor;
import org.geoserver.security.jwtheaders.token.TokenValidator;
import org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractor;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This is JWT Headers main class for authentication. Its just a simple subclass of
 * GeoServerPreAuthenticatedUserNameFilter that does a few things.
 *
 * <p>cf. GeoServerRequestHeaderAuthenticationFilter
 *
 * <p>1. UserName extractor to pull the username out of the request header (i.e. SIMPLESTRING, JWT, JSON). 2.
 * RoleExtractor to pull roles out of the request header (i.e. JWT, JSON, + standard geoserver ones) 3. Handles "logout"
 * (i.e. request with correct headers, then request without correct headers) 4. Handles "username" switchs (i.e. request
 * one user name, then another request with different username) 5. Authentication with this filter will "mark" the
 * Authentication object with the JWT Headers filter configuration that was used. This to support logout. cf.
 * JwtHeadersWebAuthDetailsSource
 */
public class GeoServerJwtHeadersFilter extends GeoServerPreAuthenticatedUserNameFilter {

    private static final Logger LOG = Logging.getLogger(GeoServerJwtHeadersFilter.class);

    // when we authenticate a username, we mark this with the configuration ID.
    // We do this so that we know it's ok to extract roles.
    private static final String HTTP_ATTRIBUTE_CONFIG_ID = "GeoServerJwtHeadersFilter.configid";

    protected GeoServerJwtHeadersFilterConfig filterConfig;

    protected TokenValidator tokenValidator;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        GeoServerJwtHeadersFilterConfig authConfig = (GeoServerJwtHeadersFilterConfig) config;
        filterConfig = (GeoServerJwtHeadersFilterConfig) authConfig.clone(true);
        setAuthenticationDetailsSource(new JwtHeadersWebAuthDetailsSource(filterConfig.id));
        tokenValidator = new TokenValidator(filterConfig.getJwtConfiguration());
    }

    /**
     * true - we already have an auth, and it was created by a JwtHeader Auth, and its the same config as this one. i.e.
     * we (this exact config) created existing auth.
     *
     * @param existingAuth - existing auth (from security context)
     * @return
     */
    public boolean existingAuthIsFromThisConfig(Authentication existingAuth) {
        if (existingAuth == null || existingAuth.getDetails() == null) return false; // not existing auth, or no details
        if (!(existingAuth.getDetails() instanceof JwtHeadersWebAuthenticationDetails))
            return false; // details isn't from us, so this isn't our auth

        JwtHeadersWebAuthenticationDetails details = (JwtHeadersWebAuthenticationDetails) existingAuth.getDetails();
        return details.getJwtHeadersConfigId().equals(this.filterConfig.id);
    }

    /**
     * true - the JwtHeaders (in request) will change the currently existing authentication
     *
     * @param existingAuth - from security context
     * @param requestPrincipleName
     * @return
     */
    public boolean principleHasChanged(Authentication existingAuth, String requestPrincipleName) {
        if (existingAuth == null) return false; // no existing auth, so it cannot be a change
        if (requestPrincipleName == null) return false; // request doesn't contain an auth, so it cannot change

        return !requestPrincipleName.equals(existingAuth.getPrincipal().toString());
    }

    /**
     * Almost all the real work is done by the super class (GeoServerPreAuthenticatedUserNameFilter). However, this
     * handles 2 cases; 1. logout 2. username changes
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String principalName = getPreAuthenticatedPrincipalName((HttpServletRequest) request);
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

        // cf. GeoServerRequestHeaderAuthenticationFilter#doFilter
        // if there is a preAuth, we might have to get rid of it.
        // This can happen if the user adds the JWT Headers to a request, and get a JSESSION
        // back.  If the JWT Headers are no longer attached (i.e. logout), we want the user to
        // be logged out (i.e. no existingAuth).
        // However, sometime there will be a JSESSION, and that will keep the use logged in.
        // We have to prevent this.
        if (existingAuthIsFromThisConfig(existingAuth) && principalName == null) {
            // logout current user - this was someone we previously logged on, but now they no
            // longer have the headers
            SecurityContextHolder.getContext().setAuthentication(null);
            SecurityContextHolder.clearContext();

            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            httpServletRequest.getSession(false).invalidate();
            // we re-create the session now because this request might be a redirect, and
            // tomcat does NOT like session creation during a redirect!
            httpServletRequest.getSession(true);
        }

        if (principleHasChanged(existingAuth, principalName)) {
            // logout current user - the user switched.
            SecurityContextHolder.getContext().setAuthentication(null);
            SecurityContextHolder.clearContext();

            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            httpServletRequest.getSession(false).invalidate();
            // we re-create the session now because this request might be a redirect, and
            // tomcat does NOT like session creation during a redirect!
            httpServletRequest.getSession(true);
        }

        if (request.getAttribute(UserName) == null) {
            request.removeAttribute(UserNameAlreadyRetrieved);
        }
        super.doFilter(request, response, chain);
    }

    public GeoServerJwtHeadersFilterConfig getFilterConfig() {
        return filterConfig;
    }

    public void setFilterConfig(GeoServerJwtHeadersFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /** extracts the username from the request (cf JwtHeaderUserNameExtractor) */
    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        String headerValue =
                request.getHeader(filterConfig.getJwtConfiguration().getUserNameHeaderAttributeName());
        JwtHeaderUserNameExtractor extractor =
                new JwtHeaderUserNameExtractor(getFilterConfig().getJwtConfiguration());
        String userName;

        try {
            userName = extractor.extractUserName(headerValue);
            tokenValidator.validate(headerValue);
        } catch (Exception e) {
            return null;
        }

        if (userName != null) {
            request.setAttribute(HTTP_ATTRIBUTE_CONFIG_ID, filterConfig.getId());
            LOG.fine("Extracted user name from JWT token: " + userName);
        }

        return userName;
    }

    /**
     * extracts the roles from the request (cf JwtHeadersRolesExtractor). It uses the standard Geoserver infrastructure
     * (superclass) for getting the "standard" roles (i.e. Header, UserGroupService, RoleService)
     */
    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal) throws IOException {
        // validate if we validated the user - if so process roles
        String id = (String) request.getAttribute(HTTP_ATTRIBUTE_CONFIG_ID);
        if (id == null || !id.equals(filterConfig.getId())) {
            return new ArrayList<GeoServerRole>();
        }

        if (filterConfig.getRoleSource() == GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT
                || filterConfig.getRoleSource() == GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JSON) {
            String headerValue =
                    request.getHeader(filterConfig.getJwtConfiguration().getRolesHeaderName());
            JwtHeadersRolesExtractor extractor = new JwtHeadersRolesExtractor(filterConfig.getJwtConfiguration());
            var roles = extractor.getRoles(headerValue);
            LOG.fine("Extracted roles from JWT token: " + String.join(", ", roles));
            return roles.stream().map(x -> new GeoServerRole(x)).collect(Collectors.toList());
        }

        return super.getRoles(request, principal);
    }
}
