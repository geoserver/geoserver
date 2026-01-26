/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geoserver.security.oauth2.common.TokenIntrospector;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * {@link Filter} supports OpenID Connect and OAuth2 based logins by delegating to the nested Spring filter
 * implementations.
 *
 * <p>The OAuth 2.0 Login feature provides an application with the capability to have users log in to the application by
 * using their existing account at an OAuth 2.0 Provider (e.g. GitHub) or OpenID Connect 1.0 Provider (such as Google).
 * OAuth 2.0 Login implements the use cases: "Login with Google" or "Login with GitHub". OAuth 2.0 Login is implemented
 * by using the Authorization Code Grant, as specified in the OAuth 2.0 Authorization Framework and OpenID Connect Core
 * 1.0.
 *
 * <p>Documentation: Diagrams exist in gs-sec-oidc/doc/diagrams, showing how to pieces belong together.
 *
 * <p>Spring OAuth2 feature matrix: https://github.com/spring-projects/spring-security/wiki/OAuth-2.0-Features-Matrix
 *
 * @see GeoServerOAuth2LoginAuthenticationProvider containing the setup
 * @author awaterme
 */
public class GeoServerOAuth2LoginAuthenticationFilter extends GeoServerCompositeFilter
        implements GeoServerAuthenticationFilter, LogoutHandler {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2LoginAuthenticationFilter.class);

    private LogoutSuccessHandler logoutSuccessHandler;

    private TokenIntrospector tokenIntrospector;

    static final String ACCESS_TOKEN_EXPIRATION = "OpenIdConnect-AccessTokenExpiration";

    public GeoServerOAuth2LoginAuthenticationFilter() {
        super();
    }

    public GeoServerOAuth2LoginAuthenticationFilter(TokenIntrospector introspector) {
        super();
        this.tokenIntrospector = introspector;
    }

    @Override
    public boolean applicableForHtml() {
        return true;
    }

    @Override
    public boolean applicableForServices() {
        return true;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authorization = httpRequest.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);

            // Prefer a properly typed API: Map<String, Object>.
            // If TokenIntrospector still returns a raw Map, update its signature accordingly.
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = (Map<String, Object>) tokenIntrospector.introspectToken(token);

            validateIntrospectedToken(request, responseMap);

            String name = resolveSubject(responseMap);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));

            Authentication auth = new PreAuthenticatedAuthenticationToken(name, "N/A", authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        super.doFilter(request, response, chain);
    }

    private static String resolveSubject(Map<String, Object> responseMap) {
        Object sub = responseMap.get("sub");
        if (sub instanceof String s && !s.isBlank()) {
            return s;
        }
        Object username = responseMap.get("username");
        if (username instanceof String u && !u.isBlank()) {
            return u;
        }
        return "introspected";
    }

    private void validateIntrospectedToken(ServletRequest servletRequest, Map<String, Object> responseMap)
            throws ServletException {
        if (!Boolean.TRUE.equals(responseMap.get("active"))) {
            throw new ServletException("Bearer token is not active");
        }
        collectExpiration(servletRequest, responseMap);
    }

    /** If an expiration attribute is set, we can use it to cache the access token */
    private static void collectExpiration(ServletRequest req, Map<String, Object> properties) {
        if (properties.get("exp") instanceof Number) {
            long exp = ((Number) properties.get("exp")).longValue();
            if (exp > Instant.now().getEpochSecond()) { // epoch is guaranteed to be in UTC like exp
                req.setAttribute(ACCESS_TOKEN_EXPIRATION, exp);
            }
        }
    }

    @Override
    public void logout(HttpServletRequest pRequest, HttpServletResponse pResponse, Authentication pAuthentication) {

        // Note: The spring handler for logout is by design a logout *success* handler rather than
        // a logout handler. Here it is treated as one of potentially many GS logoutHandlers.
        // Reason: GeoServers logout handler determination is not so flexible yet. However GS
        // OIDC logout both work, so this seems acceptable for now. The actual GS
        // logoutSuccessHandler tolerates that something else might have committed the response
        // already.
        if (logoutSuccessHandler == null) {
            return;
        }
        if (!(pAuthentication instanceof OAuth2AuthenticationToken)) {
            return; // don't do anything - user isn't signed on as oidc
        }
        try {
            logoutSuccessHandler.onLogoutSuccess(pRequest, pResponse, pAuthentication);
        } catch (IOException | ServletException e) {
            LOGGER.log(Level.SEVERE, "Logout from OAuth2/OIDC provider failed.", e);
        }
    }

    /** @param pLogoutSuccessHandler the logoutSuccessHandler to set */
    public void setLogoutSuccessHandler(LogoutSuccessHandler pLogoutSuccessHandler) {
        logoutSuccessHandler = pLogoutSuccessHandler;
    }

    /** @return the logoutSuccessHandler */
    public LogoutSuccessHandler getLogoutSuccessHandler() {
        return logoutSuccessHandler;
    }
}
