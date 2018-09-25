/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AuthenticationCachingFilter;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geotools.util.logging.Logging;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.authentication.SpringSecurityRequestAuthenticator;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.adapters.springsecurity.token.SpringSecurityAdapterTokenStoreFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/**
 * A {@link Filter} which will use Keycloak to provide an {@link Authentication} to the active
 * {@link SecurityContextHolder}. This class should not be created as a Spring Bean, or it will
 * interfere with the existing GeoServer filter construction.
 */
public class GeoServerKeycloakFilter extends GeoServerSecurityFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter, LogoutHandler {

    private static final Logger LOG = Logging.getLogger(GeoServerKeycloakFilter.class);

    // used to map keycloak roles to spring-security roles
    private final KeycloakAuthenticationProvider authenticationMapper;
    // creates token stores capable of generating spring-security tokens from keycloak auth
    private final SpringSecurityAdapterTokenStoreFactory adapterTokenStoreFactory;
    // the context of the keycloak environment (realm, URL, client-secrets etc.)
    private AdapterDeploymentContext keycloakContext;

    /** Default constructor. */
    public GeoServerKeycloakFilter() {
        this.adapterTokenStoreFactory = new SpringSecurityAdapterTokenStoreFactory();
        this.authenticationMapper = new KeycloakAuthenticationProvider();
        authenticationMapper.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        LOG.log(Level.FINER, "GeoServerKeycloakFilter.initializeFromConfig ENTRY");
        super.initializeFromConfig(config);
        GeoServerKeycloakFilterConfig keycloakConfig = (GeoServerKeycloakFilterConfig) config;
        KeycloakDeployment deployment =
                KeycloakDeploymentBuilder.build(keycloakConfig.readAdapterConfig());
        this.keycloakContext = new AdapterDeploymentContext(deployment);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // we can only handle HTTP exchanges
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    /**
     * Convenience method that re-maps the request and response types for HTTP-only use.
     *
     * @param request the request being filtered
     * @param response the response to fill during this chain
     * @param chain the chain to execute assuming auth succeeds
     * @throws IOException if there is a failure deeper in the chain
     * @throws ServletException if there is a failure deeper in the chain, or we fail to set an
     *     appropriate HTTP error status when something goes wrong
     */
    protected void doFilter(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LOG.log(Level.FINER, "GeoServerKeycloakFilter.doFilter ENTRY");
        LOG.log(Level.FINEST, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        logHttpRequest(Level.FINEST, request);
        // get auth from the context or cache
        AuthResults authResults = loadAuthn(request);
        // if the cache failed, then attempt auth normally
        if (!authResults.hasAuthentication()) {
            authResults = getNewAuthn(request, response);
        }
        // put the auth into the context and cache
        saveAuthn(request, authResults);

        // use the results as the entrypoint in the event of failure
        request.setAttribute(
                GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, authResults);

        // if successful, then continue the chain
        LOG.log(Level.FINER, "continuing filter chain");
        LOG.log(Level.FINEST, chain.getClass().getCanonicalName());

        chain.doFilter(request, response);
        logHttpResponse(Level.FINEST, response);
        LOG.log(Level.FINEST, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    /** Helper for setting up GeoServer-style logout actions. */
    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        LOG.log(Level.FINER, "GeoServerKeycloakFilter.logout ENTRY");
        // do some setup and get the deployment
        HttpFacade exchange = new SimpleHttpFacade(request, response);
        KeycloakDeployment deployment = keycloakContext.resolveDeployment(exchange);

        // redirect to referer url stripping all request parameters off
        String referer = request.getHeader(HttpHeaders.REFERER);
        String refererNoParams = referer.split("\\?")[0];

        // let geoserver know what to do with this
        request.setAttribute(
                GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR,
                deployment
                        .getLogoutUrl()
                        .queryParam(OAuth2Constants.REDIRECT_URI, refererNoParams)
                        .build()
                        .toString());
    }

    /** Cache based on the "Authorization" HTTP header. */
    @Override
    public String getCacheKey(HttpServletRequest request) {
        // cache works only based on Authorization header, since if we are using session-based auth,
        // the
        // session is already acting as our cache
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && !authHeader.isEmpty()) {
            LOG.log(Level.FINEST, () -> "cache key = " + authHeader);
            return authHeader;
        }
        return null;
    }

    @Override
    public boolean applicableForHtml() {
        return true;
    }

    @Override
    public boolean applicableForServices() {
        return true;
    }

    /**
     * Perform authentication against Keycloak tokens. A challenge may be added to the response if
     * the request fails and may be re-attempted. This method guarantees that after execution, the
     * response status will be set.
     *
     * @param request the HTTP request provided
     * @param response the HTTP response that will be returned
     * @return the credentials or challenge for credentials
     */
    protected AuthResults getNewAuthn(HttpServletRequest request, HttpServletResponse response) {
        LOG.log(Level.FINER, "GeoServerKeycloakFilter.getNewAuthn ENTRY");
        // do some setup and create the authenticator
        HttpFacade exchange = new SimpleHttpFacade(request, response);
        KeycloakDeployment deployment = keycloakContext.resolveDeployment(exchange);
        deployment.setDelegateBearerErrorResponseSending(true);
        AdapterTokenStore tokenStore =
                adapterTokenStoreFactory.createAdapterTokenStore(deployment, request);
        RequestAuthenticator authenticator =
                new SpringSecurityRequestAuthenticator(
                        exchange, request, deployment, tokenStore, -1);
        // perform the authentication operation
        AuthOutcome result = authenticator.authenticate();
        AuthChallenge challenge = authenticator.getChallenge();
        LOG.log(Level.FINE, () -> "auth result is " + result.toString());
        Authentication authn = null;
        switch (result) {
            case AUTHENTICATED:
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                authn = authenticationMapper.authenticate(auth);
                return new AuthResults(authn);
            case NOT_ATTEMPTED:
                if (deployment.isBearerOnly()) {
                    // if bearer-only, then missing auth means you are forbidden
                    return new AuthResults();
                } else {
                    return new AuthResults(challenge);
                }
            case FAILED:
                return new AuthResults();
            default:
                return new AuthResults(challenge);
        }
    }

    /**
     * Put an authn in the cache and Spring context.
     *
     * @param request source of the cache key
     * @param authn the value to cache
     */
    protected void saveAuthn(HttpServletRequest request, AuthResults authResults) {
        LOG.log(Level.FINER, "GeoServerKeycloakFilter.cacheAuthn ENTRY");
        if (authResults != null && authResults.hasAuthentication()) {
            Authentication authn = authResults.getAuthentication();
            // set the auth in the cache
            GeoServerSecurityManager mgr = getSecurityManager();
            String cacheKey = getCacheKey(request);
            if (mgr != null && cacheKey != null && !cacheKey.isEmpty()) {
                if (authn != null) {
                    LOG.log(Level.FINE, () -> "cachinig auth for " + authn.getName());
                }
                mgr.getAuthenticationCache().put(getName(), cacheKey, authn);
            }
            // set the auth in the context
            if (authn != null) {
                LOG.log(Level.FINE, "adding auth to context");
            }
            SecurityContextHolder.getContext().setAuthentication(authn);
        } else {
            SecurityContextHolder.clearContext();
            if (request != null && request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
        }
    }

    /**
     * Get an authn from the Spring context or cache.
     *
     * @param request source of the cache key
     * @return the corresponding value from the cache, or {@code null} if unavailable
     */
    protected AuthResults loadAuthn(HttpServletRequest request) {
        LOG.log(Level.FINER, "GeoServerKeycloakFilter.getCachedAuthn ENTRY");
        // get auth from context
        Authentication contextAuthn = SecurityContextHolder.getContext().getAuthentication();
        if (contextAuthn != null && contextAuthn.isAuthenticated()) {
            LOG.log(Level.FINE, "auth already exists in context");
            return new AuthResults(contextAuthn);
        }
        // get auth from cache
        GeoServerSecurityManager mgr = getSecurityManager();
        String cacheKey = getCacheKey(request);
        if (mgr != null && cacheKey != null && !cacheKey.isEmpty()) {
            Authentication authn = mgr.getAuthenticationCache().get(getName(), cacheKey);
            if (authn != null) {
                LOG.log(Level.FINE, () -> "auth located in cache for " + authn.getName());
                return new AuthResults(authn);
            }
        }
        return new AuthResults();
    }

    private static void logHttpRequest(Level level, HttpServletRequest request) {
        if (LOG.isLoggable(level)) {
            LOG.log(level, "request.method   = " + request.getMethod());
            LOG.log(level, "request.uri      = " + request.getRequestURI());
            LOG.log(level, "request.headers  = ");
            for (String headerName : Collections.list(request.getHeaderNames())) {
                if (headerName == HttpHeaders.COOKIE) {
                    continue;
                }
                StringBuilder buffer = new StringBuilder(80);
                buffer.append(leftPadMessage(headerName)).append(headerName).append(" = ");
                for (String headerValue : Collections.list(request.getHeaders(headerName))) {
                    buffer.append(headerValue).append(' ');
                }
                LOG.log(level, buffer.toString());
            }
            LOG.log(level, "request.query    = ");
            for (String queryKey : Collections.list(request.getParameterNames())) {
                StringBuilder buffer = new StringBuilder(80);
                buffer.append(leftPadMessage(queryKey)).append(queryKey).append(" = ");
                for (String queryValue : request.getParameterValues(queryKey)) {
                    buffer.append(queryValue).append(' ');
                }
                LOG.log(level, buffer.toString());
            }
            LOG.log(level, "request.cookies  = ");
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    StringBuilder buffer = new StringBuilder(80);
                    buffer.append(leftPadMessage(cookie.getName()))
                            .append(cookie.getName())
                            .append(" = ")
                            .append(cookie.getValue())
                            .append("; ")
                            .append(cookie.getPath());
                    LOG.log(level, buffer.toString());
                }
            }
        }
    }

    private static void logHttpResponse(Level level, HttpServletResponse response) {
        if (LOG.isLoggable(level)) {
            LOG.log(level, "response.status  = " + response.getStatus());
            LOG.log(level, "response.headers = ");
            for (String headerName : response.getHeaderNames()) {
                if (headerName == HttpHeaders.SET_COOKIE) {
                    continue;
                }
                StringBuilder buffer = new StringBuilder(80);
                buffer.append(leftPadMessage(headerName)).append(headerName).append(" = ");
                for (String headerValue : response.getHeaders(headerName)) {
                    buffer.append(headerValue).append(' ');
                }
                LOG.log(level, buffer.toString());
            }
            LOG.log(level, "response.cookies = ");
            for (String cookie : response.getHeaders(HttpHeaders.SET_COOKIE)) {
                int split = cookie.indexOf('=');
                String cookieName = cookie.substring(0, split);
                String cookieValue = cookie.substring(split + 1);
                StringBuilder buffer = new StringBuilder(80);
                buffer.append(leftPadMessage(cookieName))
                        .append(cookieName)
                        .append(" = ")
                        .append(cookieValue);
                LOG.log(level, buffer.toString());
            }
        }
    }

    private static String leftPadMessage(String message) {
        final String thirtySpaces = "                              ";
        return thirtySpaces.substring(Math.min(message.length(), thirtySpaces.length()));
    }
}
