/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AuthenticationCachingFilter;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * A {@link Filter} which will use Keycloak to provide an {@link Authentication} to the active
 * {@link SecurityContextHolder}. This class should not be created as a Spring Bean, or it will
 * interfere with the existing GeoServer filter construction.
 */
public class GeoServerKeycloakFilter extends GeoServerPreAuthenticatedUserNameFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter, LogoutHandler {

    private static final String ID_TOKEN_HINT = "id_token_hint";

    private static final Logger LOG = Logging.getLogger(GeoServerKeycloakFilter.class);

    // used to map keycloak roles to spring-security roles
    private final KeycloakAuthenticationProvider authenticationMapper;
    // creates token stores capable of generating spring-security tokens from keycloak auth
    private final SpringSecurityAdapterTokenStoreFactory adapterTokenStoreFactory;
    // the context of the keycloak environment (realm, URL, client-secrets etc.)
    private AdapterDeploymentContext keycloakContext;

    private boolean enableRedirectEntryPoint;

    private static final String KEYCLOAK_LOGIN_BTN = "j_spring_keycloak_login";

    /** Default constructor. */
    public GeoServerKeycloakFilter() {
        this.adapterTokenStoreFactory = new SpringSecurityAdapterTokenStoreFactory();
        this.authenticationMapper = new KeycloakAuthenticationProvider();
        SimpleAuthorityMapper simpleAuthMapper = new SimpleAuthorityMapper();
        simpleAuthMapper.setPrefix("");
        authenticationMapper.setGrantedAuthoritiesMapper(simpleAuthMapper);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        LOG.log(Level.FINER, "GeoServerKeycloakFilter.initializeFromConfig ENTRY");
        super.initializeFromConfig(config);
        GeoServerKeycloakFilterConfig keycloakConfig = (GeoServerKeycloakFilterConfig) config;
        KeycloakDeployment deployment =
                KeycloakDeploymentBuilder.build(keycloakConfig.readAdapterConfig());
        deployment.setScope("openid");
        this.keycloakContext = new AdapterDeploymentContext(deployment);
        this.enableRedirectEntryPoint = keycloakConfig.isEnableRedirectEntryPoint();
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

        if (authentication
                .getDetails()
                .getClass()
                .isAssignableFrom(org.keycloak.adapters.OidcKeycloakAccount.class)) {
            // let geoserver know what to do with this
            request.setAttribute(
                    GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR,
                    deployment
                            .getLogoutUrl()
                            .queryParam(OAuth2Constants.REDIRECT_URI, refererNoParams)
                            .queryParam(
                                    ID_TOKEN_HINT,
                                    ((org.keycloak.adapters.OidcKeycloakAccount)
                                                    authentication.getDetails())
                                            .getKeycloakSecurityContext()
                                            .getIdTokenString())
                            .build()
                            .toString());
        }
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
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        // not used
        return null;
    }

    protected Authentication getPreAuthentication(
            HttpServletRequest request, HttpServletResponse response) {
        AuthResults authResults = loadAuthn(request);
        Authentication result = null;
        // if the cache failed, then attempt auth normally
        if (!authResults.hasAuthentication()) {
            authResults = getNewAuthn(request, response);
        }
        if (authResults != null && authResults.hasAuthentication()) {
            result = authResults.getAuthentication();
        }
        // use the results as the entrypoint in the event of failure
        return result;
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
                LOG.log(Level.FINER, "GeoServerKeycloakFilter.cacheAuthn ENTRY");
                if (cacheAuthentication(postAuthentication, (HttpServletRequest) request)) {
                    getSecurityManager()
                            .getAuthenticationCache()
                            .put(getName(), cacheKey, postAuthentication);
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected void doAuthenticate(HttpServletRequest request, HttpServletResponse response) {

        LOG.log(Level.FINER, "GeoServerKeycloakFilter.doFilter ENTRY");
        LOG.log(Level.FINEST, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        logHttpRequest(Level.FINEST, request);

        Authentication auth = getPreAuthentication(request, response);
        if (auth != null) {
            Authentication authentication = buildAuthentication(request, auth);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        logHttpResponse(Level.FINEST, response);
        LOG.log(Level.FINEST, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    private Authentication buildAuthentication(
            HttpServletRequest request, Authentication keycloakAuth) {
        PreAuthenticatedAuthenticationToken result = null;
        Object principal = keycloakAuth.getPrincipal();
        String userName = null;
        if (principal instanceof UserDetails) userName = ((UserDetails) principal).getUsername();
        else userName = principal.toString();
        if (GeoServerUser.ROOT_USERNAME.equals(principal)) {
            result =
                    new PreAuthenticatedAuthenticationToken(
                            principal, null, Collections.singleton(GeoServerRole.ADMIN_ROLE));
        } else {
            Collection<GeoServerRole> roles = null;
            try {
                roles = new ArrayList<>(getRoles(request, userName));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error while retrieving roles for user.", e);
                throw new RuntimeException(e);
            }
            if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE) == false)
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            enrichWithKeycloakRoles(keycloakAuth, roles);
            result = new PreAuthenticatedAuthenticationToken(principal, null, roles);
        }
        result.setDetails(keycloakAuth.getDetails());
        return result;
    }

    private void enrichWithKeycloakRoles(
            Authentication keycloakAuth, Collection<GeoServerRole> roles) {
        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();

        for (GrantedAuthority authoritity : keycloakAuth.getAuthorities()) {
            GeoServerRole role = null;
            try {
                role = roleService.getRoleByName(authoritity.getAuthority());
            } catch (IOException e) {
                LOG.log(
                        Level.WARNING,
                        "Error while trying to get geoserver roles with following Exception cause:",
                        e.getCause());
            }
            if (role != null) {
                roles.add(role);
            } else {
                roles.add(new GeoServerRole(authoritity.getAuthority()));
            }
        }
        RoleCalculator calc = new RoleCalculator(roleService);
        try {
            calc.addInheritedRoles(roles);
        } catch (IOException e) {
            LOG.log(
                    Level.WARNING,
                    "Error while trying to get geoserver roles with following Exception cause:",
                    e.getCause());
        }
        calc.addMappedSystemRoles(roles);
    }

    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal)
            throws IOException {
        Collection<GeoServerRole> roles = super.getRoles(request, principal);
        if (!roles.contains(GeoServerRole.AUTHENTICATED_ROLE))
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);

        RoleCalculator calc = new RoleCalculator(getSecurityManager().getActiveRoleService());
        if (calc != null) {
            try {
                roles.addAll(calc.calculateRoles(principal));
            } catch (IOException e) {
                LOG.log(
                        Level.WARNING,
                        "Error while trying to fetch default Roles with the following Exception cause:",
                        e.getCause());
            }
        }
        return roles;
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
        request =
                new HttpServletRequestWrapper(request) {
                    @Override
                    public StringBuffer getRequestURL() {
                        String url = super.getRequestURL().toString();
                        String proto = super.getHeader("x-forwarded-proto");

                        if (proto != null && url.startsWith("http://") && proto.equals("https")) {
                            url = url.replaceAll("^http", "https");
                        }
                        return new StringBuffer(url);
                    }
                };
        HttpFacade exchange = new SimpleHttpFacade(request, response);
        KeycloakDeployment deployment = keycloakContext.resolveDeployment(exchange);
        deployment.setDelegateBearerErrorResponseSending(true);
        AdapterTokenStore tokenStore =
                adapterTokenStoreFactory.createAdapterTokenStore(deployment, request, response);
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
                    return setAndReturnChallengeAep(request, response, null);
                } else {
                    return setAndReturnChallengeAep(request, response, challenge);
                }
            case FAILED:
                return setAndReturnChallengeAep(request, response, null);
            default:
                return setAndReturnChallengeAep(request, response, challenge);
        }
    }

    private AuthResults setAndReturnChallengeAep(
            HttpServletRequest request, HttpServletResponse response, AuthChallenge challenge) {
        AuthResults results = new AuthResults(challenge);
        if (enableRedirectEntryPoint) {
            request.setAttribute(
                    GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, results);
        } else if (redirectFromLoginBtn(request)) {
            try {
                results.commence(request, response, null);
            } catch (IOException | ServletException e) {
                LOG.log(Level.SEVERE, "Error while sending redirect to keycloak login page.", e);
            }
        }
        return results;
    }

    private boolean redirectFromLoginBtn(HttpServletRequest request) {
        // unlike openid google etc. uses a parameter because keycloak will redirect by default
        // to the url from which the user land on the keycloak login page.
        // in this way we ensure the user will be redirect to the /web and not to a not existing
        // page.
        String keycloakParam = request.getParameter(KEYCLOAK_LOGIN_BTN);
        return keycloakParam != null && "true".equals(keycloakParam);
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

    @Override
    public RoleSource getRoleSource() {
        RoleSource roleSource = super.getRoleSource();
        if (roleSource == null)
            roleSource =
                    PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource
                            .RoleService;
        return roleSource;
    }
}
