/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.oauth2oidc;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetailsSource;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

/**
 * This is basically the parent class (OAuth2ClientAuthenticationProcessingFilter), but with a minor
 * difference: It sends the full access token (including the embedded id_token + refresh token
 * etc...) to a non-standard (GeoServerTokenServices) tokenServices. This token service has
 * #loadAuthentication(accessToken) where accessToken is the full access token (with id_token).
 * Normally, it will call #loadAuthentication(String accessToken) where the access token is just the
 * (typically) base64 encoded JWT (note - it doesnt have to be a JWT - it can be anything).
 *
 * <p>cf GeoserverTokenServices
 */
public class GeoServerAuthenticationProcessingFilter
        extends OAuth2ClientAuthenticationProcessingFilter {

    public OAuth2RestOperations restTemplate;
    public OAuth2AuthenticationDetailsSource _authenticationDetailsSource;
    private ResourceServerTokenServices tokenServices;

    public GeoServerAuthenticationProcessingFilter(
            String defaultFilterProcessesUrl,
            OAuth2RestOperations oauth2RestTemplate,
            RemoteTokenServices tokenServices) {
        super(defaultFilterProcessesUrl);
        setRestTemplate(oauth2RestTemplate);
        setTokenServices(tokenServices);
        _authenticationDetailsSource = new OAuth2AuthenticationDetailsSource();
    }

    /**
     * A rest template to be used to obtain an access token.
     *
     * @param restTemplate a rest template
     */
    public void setRestTemplate(OAuth2RestOperations restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Reference to a CheckTokenServices that can validate an OAuth2AccessToken
     *
     * @param tokenServices
     */
    public void setTokenServices(ResourceServerTokenServices tokenServices) {
        this.tokenServices = tokenServices;
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        OAuth2AccessToken accessToken;
        try {
            accessToken = restTemplate.getAccessToken();
        } catch (OAuth2Exception e) {
            BadCredentialsException bad =
                    new BadCredentialsException("Could not obtain access token", e);
            throw bad;
        }
        try {
            OAuth2Authentication result;
            // BAD - we have 2 different implementations and are handling the difference here.
            //      REASON - want to have access to the "full" access token (i.e. with the id_token
            // embedded)
            if (tokenServices instanceof GeoServerTokenServices) {
                result = ((GeoServerTokenServices) tokenServices).loadAuthentication(accessToken);
            } else {
                result = tokenServices.loadAuthentication(accessToken.getValue());
            }
            if (_authenticationDetailsSource != null) {
                request.setAttribute(
                        OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE, accessToken.getValue());
                request.setAttribute(
                        OAuth2AuthenticationDetails.ACCESS_TOKEN_TYPE, accessToken.getTokenType());
                result.setDetails(_authenticationDetailsSource.buildDetails(request));
            }
            return result;
        } catch (InvalidTokenException e) {
            BadCredentialsException bad =
                    new BadCredentialsException("Could not obtain user details from token", e);
            throw bad;
        }
    }
}
