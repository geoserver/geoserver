/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkTokenStore;

class ValidatingOAuth2RestTemplate extends OAuth2RestTemplate {

    private final JwkTokenStore store;

    public ValidatingOAuth2RestTemplate(
            OAuth2ProtectedResourceDetails resource, OAuth2ClientContext context, String jwkUri) {
        super(resource, context);
        this.store = new JwkTokenStore(jwkUri);
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
            // among other things, this verifies the token
            store.readAuthentication(idToken);
            // TODO: the authentication just read could contain roles, could be treated as
            // another role source... but needs to be made available to role computation
        }
    }
}
