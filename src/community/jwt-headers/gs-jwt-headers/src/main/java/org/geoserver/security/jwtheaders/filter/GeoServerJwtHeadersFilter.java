/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource;
import org.geoserver.security.jwtheaders.filter.details.JwtHeadersWebAuthDetailsSource;
import org.geoserver.security.jwtheaders.filter.details.JwtHeadersWebAuthenticationDetails;
import org.geoserver.security.jwtheaders.roles.JwtHeadersRolesExtractor;
import org.geoserver.security.jwtheaders.token.TokenIssuerValidator;
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

    // claims of the validated identity token, suffixed with the configuration ID
    private static final String HTTP_ATTRIBUTE_VALIDATED_CLAIMS = "GeoServerJwtHeadersFilter.validatedClaims.";

    // configuration diagnostics already logged (filter name, kind and the settings involved), so that loading the
    // filter again, e.g. from the admin pages, does not repeat them
    private static final Set<String> LOGGED_DIAGNOSTICS = ConcurrentHashMap.newKeySet();

    protected GeoServerJwtHeadersFilterConfig filterConfig;

    protected TokenValidator tokenValidator;

    // the first refused built-in account is logged as a warning, the following ones at FINE
    private final AtomicBoolean refusalLogged = new AtomicBoolean();

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        GeoServerJwtHeadersFilterConfig authConfig = (GeoServerJwtHeadersFilterConfig) config;
        filterConfig = (GeoServerJwtHeadersFilterConfig) authConfig.clone(true);
        setAuthenticationDetailsSource(new JwtHeadersWebAuthDetailsSource(filterConfig.id));
        tokenValidator = new TokenValidator(filterConfig.getJwtConfiguration());
        logConfigurationWarnings();
    }

    /**
     * Reports stored settings that the save-time validation now rejects, or that weaken the validation. Each one is
     * logged once per filter and setting.
     */
    private void logConfigurationWarnings() {
        var jwtConfig = filterConfig.getJwtConfiguration();
        if (!jwtConfig.isValidateToken()) return;

        if (!jwtConfig.isValidateTokenSignature() && !jwtConfig.isValidateTokenAgainstURL()) {
            logOnce(
                    Level.WARNING,
                    "no-check",
                    "' validates tokens without checking their signature or asking the endpoint, so their claims "
                            + "can be forged. Enable the signature or the endpoint check.");
        }
        if (jwtConfig.isValidateTokenSignature()
                && TokenIssuerValidator.acceptedIssuers(jwtConfig.getValidateTokenIssuer())
                        .isEmpty()) {
            logOnce(
                    Level.WARNING,
                    "any-issuer",
                    "' validates token signatures but accepts any issuer. Configure the accepted issuers, "
                            + "since a signing key set can be shared by several issuers.");
        }
        if (filterConfig.readsRolesFromUnvalidatedHeader()) {
            logOnce(
                    Level.SEVERE,
                    "unvalidated-roles",
                    "' validates the identity token but is configured to read roles from a source that is not "
                            + "that token. No roles will be taken from it. Take the roles from the validated token, a "
                            + "role service or a user/group service, or explicitly trust the roles header.");
        }
    }

    private void logOnce(Level level, String kind, String message) {
        var jwtConfig = filterConfig.getJwtConfiguration();
        String key = String.join(
                "|",
                filterConfig.getName(),
                kind,
                String.valueOf(jwtConfig.isValidateTokenSignature()),
                String.valueOf(jwtConfig.isValidateTokenAgainstURL()),
                String.valueOf(jwtConfig.getValidateTokenIssuer()),
                String.valueOf(filterConfig.getRoleSource()),
                String.valueOf(jwtConfig.getRolesHeaderName()),
                String.valueOf(filterConfig.getTrustUnvalidatedRolesHeader()));
        if (LOGGED_DIAGNOSTICS.add(key)) {
            LOG.log(level, "JWT Headers filter '" + filterConfig.getName() + message);
        }
    }

    private String identityHeaderName() {
        return filterConfig.getJwtConfiguration().getUserNameHeaderAttributeName();
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
        String headerValue = request.getHeader(identityHeaderName());
        if (headerValue == null) {
            return null;
        }
        JwtHeaderUserNameExtractor extractor =
                new JwtHeaderUserNameExtractor(getFilterConfig().getJwtConfiguration());
        String userName;
        Map<String, Object> validatedClaims;

        try {
            userName = extractor.extractUserName(headerValue);
            // null when validation is off
            validatedClaims = tokenValidator.validateAndParse(headerValue);
        } catch (Exception e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("JWT Headers filter '" + filterConfig.getName() + "' rejected the request header: "
                        + e.getMessage());
            }
            return null;
        }

        if (userName == null) {
            return null;
        }
        if (filterConfig.isPrincipalBlocked(userName)) {
            String message = "JWT Headers filter '" + filterConfig.getName()
                    + "' refused a built-in administrator account (root, or admin when not allowed) asserted by the "
                    + "request header";
            if (refusalLogged.compareAndSet(false, true)) {
                LOG.warning(message + "; further refusals by this filter are logged at FINE level");
            } else {
                LOG.fine(message);
            }
            return null;
        }

        request.setAttribute(HTTP_ATTRIBUTE_CONFIG_ID, filterConfig.getId());
        if (validatedClaims != null) {
            request.setAttribute(HTTP_ATTRIBUTE_VALIDATED_CLAIMS + filterConfig.getId(), validatedClaims);
        }
        if (LOG.isLoggable(Level.FINE)) {
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
        // normally refused already when the principal was extracted; this covers a principal handed over by another
        // pre-authentication filter in the same chain
        if (filterConfig.isPrincipalBlocked(principal)) {
            return new ArrayList<GeoServerRole>();
        }

        RoleSource roleSource = filterConfig.getRoleSource();
        JwtHeadersRolesExtractor extractor = new JwtHeadersRolesExtractor(filterConfig.getJwtConfiguration());

        if (filterConfig.getJwtConfiguration().isValidateToken() && !filterConfig.getTrustUnvalidatedRolesHeader()) {
            // the token was validated: roles may only come from that same token
            if (JWTHeaderRoleSource.JWT.equals(roleSource) && filterConfig.rolesHeaderIsUserNameHeader()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> claims =
                        (Map<String, Object>) request.getAttribute(HTTP_ATTRIBUTE_VALIDATED_CLAIMS + id);
                try {
                    return toGeoServerRoles(extractor.getRolesFromClaims(claims));
                } catch (RuntimeException e) {
                    LOG.log(Level.FINE, "Could not extract roles from the validated token", e);
                    return new ArrayList<GeoServerRole>();
                }
            }
            if (filterConfig.readsRolesFromUnvalidatedHeader()) {
                // reported when the filter is loaded, see logConfigurationWarnings()
                return new ArrayList<GeoServerRole>();
            }
        }

        if (JWTHeaderRoleSource.JWT.equals(roleSource) || JWTHeaderRoleSource.JSON.equals(roleSource)) {
            String rolesHeader = filterConfig.rolesHeaderIsUserNameHeader()
                    ? identityHeaderName()
                    : filterConfig.getJwtConfiguration().getRolesHeaderName();
            try {
                return toGeoServerRoles(extractor.getRoles(request.getHeader(rolesHeader)));
            } catch (RuntimeException e) {
                LOG.log(Level.FINE, "Could not extract roles from the roles header", e);
                return new ArrayList<GeoServerRole>();
            }
        }

        return super.getRoles(request, principal);
    }

    private static List<GeoServerRole> toGeoServerRoles(Collection<String> roles) {
        if (roles == null) {
            return new ArrayList<>();
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Extracted roles from JWT token: " + String.join(", ", roles));
        }
        return roles.stream().map(GeoServerRole::new).collect(Collectors.toList());
    }
}
