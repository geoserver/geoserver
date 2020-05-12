/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.SecurityUtils;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.client.ResourceAccessException;

/**
 * OAuth2 Authentication filter receiving/validating proxy tickets and service tickets.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class GeoServerOAuthAuthenticationFilter
        extends GeoServerPreAuthenticatedUserNameFilter
        implements GeoServerAuthenticationFilter, LogoutHandler {

    public static final String SESSION_COOKIE_NAME = "sessionid";

    OAuth2FilterConfig filterConfig;

    OAuth2RestOperations restTemplate;

    OAuth2ClientAuthenticationProcessingFilter filter =
            new OAuth2ClientAuthenticationProcessingFilter("/");

    ResourceServerTokenServices tokenServices;

    GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration;

    public GeoServerOAuthAuthenticationFilter(
            SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate) {
        this.filterConfig = (OAuth2FilterConfig) config;
        this.tokenServices = tokenServices;
        this.oauth2SecurityConfiguration = oauth2SecurityConfiguration;
        this.restTemplate = oauth2RestTemplate;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        aep = filterConfig.getAuthenticationEntryPoint();
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return this.aep;
    }

    /** Try to authenticate if there is no authenticated principal */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Search for an access_token on the request (simulating SSO)
        String accessToken = getAccessTokenFromRequest(request);

        OAuth2AccessToken token = restTemplate.getOAuth2ClientContext().getAccessToken();

        if (accessToken != null && token != null && !token.getValue().equals(accessToken)) {
            restTemplate.getOAuth2ClientContext().setAccessToken(null);
            token = restTemplate.getOAuth2ClientContext().getAccessToken();
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        /*
         * This cookie works only locally, when accessing the GeoServer GUI and on the same domain. For remote access you need to logout from the
         * GeoServer GUI.
         */
        final String customSessionCookie = getCustomSessionCookieValue(httpRequest);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Collection<? extends GrantedAuthority> authorities =
                (authentication != null ? authentication.getAuthorities() : null);

        if (accessToken == null
                && customSessionCookie == null
                && (authentication != null
                        && (authentication instanceof PreAuthenticatedAuthenticationToken)
                        && !(authorities.size() == 1
                                && authorities.contains(GeoServerRole.ANONYMOUS_ROLE)))) {
            final AccessTokenRequest accessTokenRequest =
                    restTemplate.getOAuth2ClientContext().getAccessTokenRequest();
            if (accessTokenRequest != null && accessTokenRequest.getStateKey() != null) {
                restTemplate
                        .getOAuth2ClientContext()
                        .removePreservedState(accessTokenRequest.getStateKey());
            }

            try {
                accessTokenRequest.remove("access_token");
            } finally {
                SecurityContextHolder.clearContext();
                httpRequest.getSession(false).invalidate();
                try {
                    httpRequest.logout();
                    authentication = null;
                } catch (ServletException e) {
                    LOGGER.fine(e.getLocalizedMessage());
                }
                LOGGER.fine("Cleaned out Session Access Token Request!");
            }
        }

        if ((authentication == null && accessToken != null)
                || (accessToken != null && token == null)
                || authentication == null
                || (authentication != null
                        && authorities.size() == 1
                        && authorities.contains(GeoServerRole.ANONYMOUS_ROLE))) {

            doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response);

            Authentication postAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (postAuthentication != null) {
                if (cacheAuthentication(postAuthentication, (HttpServletRequest) request)) {
                    getSecurityManager()
                            .getAuthenticationCache()
                            .put(
                                    getName(),
                                    getCacheKey((HttpServletRequest) request),
                                    postAuthentication);
                }
            }
        }

        chain.doFilter(request, response);
    }

    protected String getBearerToken(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            Authentication auth = new BearerTokenExtractor().extract((HttpServletRequest) request);
            if (auth != null) return SecurityUtils.getUsername(auth.getPrincipal());
        }

        return null;
    }

    protected String getParameterValue(String paramName, ServletRequest request) {
        for (Enumeration<String> iterator = request.getParameterNames();
                iterator.hasMoreElements(); ) {
            final String param = iterator.nextElement();
            if (paramName.equalsIgnoreCase(param)) {
                return request.getParameter(param);
            }
        }

        return null;
    }

    /** The cache key is the authentication key (global identifier) */
    @Override
    public String getCacheKey(HttpServletRequest request) {
        final String access_token = getAccessTokenFromRequest(request);
        return access_token != null ? access_token : getCustomSessionCookieValue(request);
    }

    protected String getCustomSessionCookieValue(HttpServletRequest request) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Inspecting the http request looking for the Custom Session ID.");
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Found " + cookies.length + " cookies!");
            }
            for (Cookie c : cookies) {
                if (c.getName().equalsIgnoreCase(SESSION_COOKIE_NAME)) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Found Custom Session cookie: " + c.getValue());
                    }
                    return c.getValue();
                }
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Found no cookies!");
            }
        }

        return null;
    }

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        OAuth2AccessToken token = restTemplate.getOAuth2ClientContext().getAccessToken();
        if ((token != null && token.getTokenType().equalsIgnoreCase(OAuth2AccessToken.BEARER_TYPE))
                || request.getRequestURI().endsWith(filterConfig.getLogoutEndpoint())) {

            final AccessTokenRequest accessTokenRequest =
                    restTemplate.getOAuth2ClientContext().getAccessTokenRequest();
            if (accessTokenRequest != null && accessTokenRequest.getStateKey() != null) {
                restTemplate
                        .getOAuth2ClientContext()
                        .removePreservedState(accessTokenRequest.getStateKey());
            }

            try {
                accessTokenRequest.remove("access_token");
            } finally {
                SecurityContextHolder.clearContext();
                request.getSession(false).invalidate();
                try {
                    request.logout();
                } catch (ServletException e) {
                    LOGGER.fine(e.getLocalizedMessage());
                }
                LOGGER.fine("Cleaned out Session Access Token Request!");
            }

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            Cookie[] allCookies = request.getCookies();

            for (int i = 0; i < allCookies.length; i++) {
                String name = allCookies[i].getName();
                if (name.equalsIgnoreCase("JSESSIONID")) {
                    Cookie cookieToDelete = allCookies[i];
                    cookieToDelete.setMaxAge(-1);
                    cookieToDelete.setPath("/");
                    cookieToDelete.setComment("EXPIRING COOKIE at " + System.currentTimeMillis());
                    response.addCookie(cookieToDelete);
                }
            }

            request.setAttribute(
                    GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR, filterConfig.getLogoutUri());
        }
    }

    @Override
    protected void doAuthenticate(HttpServletRequest request, HttpServletResponse response) {

        String principal = null;
        try {
            principal = getPreAuthenticatedPrincipal(request, response);
        } catch (IOException e1) {
            LOGGER.log(Level.FINE, e1.getMessage(), e1);
            principal = null;
        } catch (ServletException e1) {
            LOGGER.log(Level.FINE, e1.getMessage(), e1);
            principal = null;
        }

        LOGGER.log(
                Level.FINE,
                "preAuthenticatedPrincipal = " + principal + ", trying to authenticate");

        PreAuthenticatedAuthenticationToken result = null;

        if (principal == null || principal.trim().length() == 0) {
            result =
                    new PreAuthenticatedAuthenticationToken(
                            principal, null, Collections.singleton(GeoServerRole.ANONYMOUS_ROLE));
        } else {
            if (GeoServerUser.ROOT_USERNAME.equals(principal)) {
                result =
                        new PreAuthenticatedAuthenticationToken(
                                principal,
                                null,
                                Arrays.asList(
                                        GeoServerRole.ADMIN_ROLE,
                                        GeoServerRole.GROUP_ADMIN_ROLE,
                                        GeoServerRole.AUTHENTICATED_ROLE));
            } else {
                Collection<GeoServerRole> roles = null;
                try {
                    roles = getRoles(request, principal);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE) == false)
                    roles.add(GeoServerRole.AUTHENTICATED_ROLE);

                RoleCalculator calc =
                        new RoleCalculator(getSecurityManager().getActiveRoleService());
                if (calc != null) {
                    try {
                        roles.addAll(calc.calculateRoles(principal));
                    } catch (IOException e) {
                        LOGGER.log(
                                Level.WARNING,
                                "Error while trying to fetch default Roles with the following Exception cause:",
                                e.getCause());
                    }
                }

                result = new PreAuthenticatedAuthenticationToken(principal, null, roles);
            }
        }

        result.setDetails(getAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(result);
    }

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
        try {
            return getPreAuthenticatedPrincipal(request, null);
        } catch (IOException e) {
            return null;
        } catch (ServletException e) {
            return null;
        }
    }

    protected String getPreAuthenticatedPrincipal(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        // Make sure the REST Resource Template has been correctly configured
        configureRestTemplate();

        // Avoid retrieving the user name more than once
        /*
         * if (req.getAttribute(UserNameAlreadyRetrieved) != null) return (String) req.getAttribute(UserName);
         */

        // Search for an access_token on the request (simulating SSO)
        String accessToken = getAccessTokenFromRequest(req);

        if (accessToken != null) {
            restTemplate
                    .getOAuth2ClientContext()
                    .setAccessToken(new DefaultOAuth2AccessToken(accessToken));
        }

        // Setting up OAuth2 Filter services and resource template
        filter.setRestTemplate(restTemplate);
        filter.setTokenServices(tokenServices);

        // Validating the access_token
        Authentication authentication = null;
        try {
            authentication = filter.attemptAuthentication(req, null);
            LOGGER.log(
                    Level.FINE,
                    "Authenticated OAuth request for principal {0}",
                    authentication.getPrincipal());
        } catch (Exception e) {
            if (e instanceof UserRedirectRequiredException) {
                if (filterConfig.getEnableRedirectAuthenticationEntryPoint()
                        || req.getRequestURI().endsWith(filterConfig.getLoginEndpoint())) {
                    // Intercepting a "UserRedirectRequiredException" and redirect to the OAuth2
                    // Provider login URI
                    this.aep.commence(req, resp, null);
                } else {
                    if (resp.getStatus() != 302) {
                        // AEP redirection failed
                        final AccessTokenRequest accessTokenRequest =
                                restTemplate.getOAuth2ClientContext().getAccessTokenRequest();
                        if (accessTokenRequest.getPreservedState() != null
                                && accessTokenRequest.getStateKey() != null) {
                            // restTemplate.getOAuth2ClientContext().removePreservedState(accessTokenRequest.getStateKey());
                            accessTokenRequest.remove("state");
                            accessTokenRequest.remove(accessTokenRequest.getStateKey());
                            accessTokenRequest.setPreservedState(null);
                        }
                    }
                }
            } else if (e instanceof BadCredentialsException
                    || e instanceof ResourceAccessException) {
                if (e.getCause() instanceof OAuth2AccessDeniedException) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error while trying to authenticate to OAuth2 Provider with the following Exception cause:",
                            e.getCause());
                } else if (e instanceof ResourceAccessException) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Could not Authorize OAuth2 Resource due to the following exception:",
                            e);
                } else if (e instanceof ResourceAccessException
                        || e.getCause() instanceof OAuth2AccessDeniedException) {
                    LOGGER.log(
                            Level.WARNING,
                            "It is worth notice that if you try to validate credentials against an SSH protected Endpoint, you need either your server exposed on a secure SSL channel or OAuth2 Provider Certificate to be trusted on your JVM!");
                    LOGGER.info(
                            "Please refer to the GeoServer OAuth2 Plugin Documentation in order to find the steps for importing the SSH certificates.");
                } else {
                    LOGGER.log(
                            Level.SEVERE,
                            "Could not Authorize OAuth2 Resource due to the following exception:",
                            e.getCause());
                }
            }
        }

        String username =
                (authentication != null
                        ? SecurityUtils.getUsername(authentication.getPrincipal())
                        : null);
        if (username != null && username.trim().length() == 0) username = null;
        try {
            if (username != null
                    && PreAuthenticatedUserNameRoleSource.UserGroupService.equals(
                            getRoleSource())) {
                GeoServerUserGroupService service =
                        getSecurityManager().loadUserGroupService(getUserGroupServiceName());
                GeoServerUser u = service.getUserByUsername(username);
                if (u != null && u.isEnabled() == false) {
                    username = null;
                    handleDisabledUser(u, req);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        req.setAttribute(UserNameAlreadyRetrieved, Boolean.TRUE);
        if (username != null) req.setAttribute(UserName, username);
        return username;
    }

    private String getAccessTokenFromRequest(ServletRequest req) {
        String accessToken = getParameterValue("access_token", req);
        if (accessToken == null) {
            accessToken = getBearerToken(req);
        }
        return accessToken;
    }

    protected void configureRestTemplate() {
        AuthorizationCodeResourceDetails details =
                (AuthorizationCodeResourceDetails) restTemplate.getResource();

        details.setClientId(filterConfig.getCliendId());
        details.setClientSecret(filterConfig.getClientSecret());
        ((GeoServerOAuthRemoteTokenServices) this.tokenServices)
                .setClientId(filterConfig.getCliendId());
        ((GeoServerOAuthRemoteTokenServices) this.tokenServices)
                .setClientSecret(filterConfig.getClientSecret());

        details.setAccessTokenUri(filterConfig.getAccessTokenUri());
        details.setUserAuthorizationUri(filterConfig.getUserAuthorizationUri());
        details.setPreEstablishedRedirectUri(filterConfig.getRedirectUri());
        ((GeoServerOAuthRemoteTokenServices) this.tokenServices)
                .setCheckTokenEndpointUrl(filterConfig.getCheckTokenEndpointUrl());

        details.setScope(parseScopes(filterConfig.getScopes()));
    }

    protected List<String> parseScopes(String commaSeparatedScopes) {
        List<String> scopes = newArrayList();
        Collections.addAll(scopes, commaSeparatedScopes.split(","));
        return scopes;
    }

    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        // Nothing to do; everything is handled by {@link getPreAuthenticatedPrincipal}
        return null;
    }
}
