/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
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
