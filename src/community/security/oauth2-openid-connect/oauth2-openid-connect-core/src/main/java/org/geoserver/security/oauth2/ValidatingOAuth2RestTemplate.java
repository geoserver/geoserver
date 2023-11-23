/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Optional;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkTokenStore;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Rest template that is able to make OpenID Connect REST requests with resource {@link
 * OpenIdConnectFilterConfig}.
 */
class ValidatingOAuth2RestTemplate extends OAuth2RestTemplate {

    private static final Logger LOGGER = Logging.getLogger(ValidatingOAuth2RestTemplate.class);

    OpenIdConnectFilterConfig config;

    private JwkTokenStore store;

    public ValidatingOAuth2RestTemplate(
            OAuth2ProtectedResourceDetails resource,
            OAuth2ClientContext context,
            String jwkUri,
            OpenIdConnectFilterConfig config) {
        super(resource, context);
        if (jwkUri != null) this.store = new JwkTokenStore(jwkUri);
        this.config = config;
    }

    @Override
    protected OAuth2AccessToken acquireAccessToken(OAuth2ClientContext oauth2Context)
            throws UserRedirectRequiredException {

        OAuth2AccessToken result = null;
        try {
            result = super.acquireAccessToken(oauth2Context);
            return result;
        } finally {
            // CODE shouldn't typically be displayed since it can be "handed in" for an access/id
            // token So, we don't log the CODE until AFTER it has been handed in.
            // CODE is one-time-use.
            if (config.isAllowUnSecureLogging()) {
                if ((oauth2Context != null) && (oauth2Context.getAccessTokenRequest() != null)) {
                    AccessTokenRequest accessTokenRequest = oauth2Context.getAccessTokenRequest();
                    if ((accessTokenRequest.getAuthorizationCode() != null)
                            && (!accessTokenRequest.getAuthorizationCode().isEmpty())) {
                        LOGGER.fine(
                                "OIDC: received a CODE from Identity Provider - handing it in for ID/Access Token");
                        LOGGER.fine("OIDC: CODE=" + accessTokenRequest.getAuthorizationCode());
                        if (result != null) {
                            LOGGER.fine(
                                    "OIDC: Identity Provider returned Token, type="
                                            + result.getTokenType());
                            LOGGER.fine("OIDC: SCOPES=" + String.join(" ", result.getScope()));
                            LOGGER.fine("OIDC: ACCESS TOKEN:" + saferJWT(result.getValue()));
                            if (result.getAdditionalInformation().containsKey("id_token")) {
                                String idToken =
                                        saferJWT(
                                                (String)
                                                        result.getAdditionalInformation()
                                                                .get("id_token"));
                                LOGGER.fine("OIDC: ID TOKEN:" + idToken);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * logs the string value of a token if its a JWT token - it should be in 3 parts, separated by a
     * "." These 3 sections are: header, claims, signature We only log the 2nd (claims) part. This
     * is safer because without the signature the token will not validate.
     *
     * <p>We don't log the token directly because it can be used to access protected resources.
     *
     * @param jwt
     * @return
     */
    String saferJWT(String jwt) {
        String[] JWTParts = jwt.split("\\.");
        if (JWTParts.length > 1) return JWTParts[1]; // this is the claims part
        return "NOT A JWT"; // not a JWT
    }

    @Override
    public OAuth2AccessToken getAccessToken() throws UserRedirectRequiredException {
        OAuth2AccessToken token = super.getAccessToken();
        if (token != null) validate(token);
        return token;
    }

    private void validate(OAuth2AccessToken token) {
        Object maybeIdToken = token.getAdditionalInformation().get("id_token");
        if (maybeIdToken instanceof String) {
            String idToken = (String) maybeIdToken;
            setAsRequestAttribute(OpenIdConnectAuthenticationFilter.ID_TOKEN_VALUE, idToken);
            // among other things, this verifies the token
            if (store != null) store.readAuthentication(idToken);
            // TODO: the authentication just read could contain roles, could be treated as
            // another role source... but needs to be made available to role computation
        }
    }

    private void setAsRequestAttribute(String key, String value) {
        Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ra -> ra instanceof ServletRequestAttributes)
                .map(ra -> ((ServletRequestAttributes) ra))
                .map(ServletRequestAttributes::getRequest)
                .ifPresent(r -> r.setAttribute(key, value));
    }
}
