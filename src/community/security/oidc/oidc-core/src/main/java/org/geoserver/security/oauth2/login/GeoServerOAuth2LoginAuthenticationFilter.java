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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * {@link Filter} supports OpenID Connect and OAuth2 based logins by delegating to the nested Spring Security filter
 * implementations.
 *
 * <p>Interactive browser logins are handled via Spring Security's {@code oauth2Login()} configuration.
 *
 * <p>Machine-to-machine requests using {@code Authorization: Bearer <token>} are handled via Spring Security's
 * {@code oauth2ResourceServer()} configuration. In particular, opaque token introspection is performed by the resource
 * server chain (e.g. {@code BearerTokenAuthenticationFilter} + {@code OpaqueTokenAuthenticationProvider}). This filter
 * intentionally does not attempt to introspect or set authentication manually.
 *
 * <p>Documentation: Diagrams exist in gs-sec-oidc/doc/diagrams, showing how to piece belong together.
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

    public GeoServerOAuth2LoginAuthenticationFilter() {
        super();
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
        // Delegation-only: interactive login and bearer token authentication are handled by the nested Spring filters.
        super.doFilter(request, response, chain);
    }

    @Override
    public void logout(HttpServletRequest pRequest, HttpServletResponse pResponse, Authentication pAuthentication) {

        // Note: The spring handler for logout is by design a logout *success* handler rather than
        // a logout handler. Here it is treated as one of potentially many GS logoutHandlers.
        // Reason: GeoServers logout handler determination is not so flexible yet. However, GS
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
