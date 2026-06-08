/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login.builder;

import static org.geoserver.security.filter.GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * Builds the OIDC RP-initiated logout success handler. The handler is wired with a trivial {@link RedirectStrategy}
 * that defers the actual redirect to GeoServer's main logout system (via the {@code LOGOUT_REDIRECT_ATTR} request
 * attribute) rather than performing it directly.
 *
 * <p>Extracted from {@code GeoServerOAuth2LoginAuthenticationFilterBuilder}.
 */
public class LogoutHandlerFactory {

    /**
     * @param repo client registration repository to look up the end_session_endpoint against; must not be null
     * @param postLogoutRedirectUri the URI Spring will send to the IdP as the {@code post_logout_redirect_uri}; may be
     *     null, in which case Spring falls back to its default
     * @return a {@link LogoutSuccessHandler} that hands the resolved logout URL back to GeoServer's logout filter via
     *     request attribute
     */
    public LogoutSuccessHandler create(ClientRegistrationRepository repo, String postLogoutRedirectUri) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(repo);
        handler.setPostLogoutRedirectUri(postLogoutRedirectUri);
        handler.setRedirectStrategy(new GeoServerLogoutRedirectStrategy());
        return handler;
    }

    /**
     * Redirect strategy that does NOT perform the redirect itself but instead stashes the target URL on the request via
     * {@code LOGOUT_REDIRECT_ATTR}. GeoServer's {@code GeoServerLogoutFilter} reads that attribute and performs the
     * actual redirect downstream, after its own logout post-processing is done.
     */
    private static final class GeoServerLogoutRedirectStrategy implements RedirectStrategy {
        @Override
        public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) {
            request.setAttribute(LOGOUT_REDIRECT_ATTR, url);
        }
    }
}
