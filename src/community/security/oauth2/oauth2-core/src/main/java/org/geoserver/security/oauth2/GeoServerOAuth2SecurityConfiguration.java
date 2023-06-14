/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Arrays;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultRequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.AuthenticationScheme;

/**
 * Base OAuth2 Configuration Class. Each OAuth2 specific Extension must implement its own {@link
 * OAuth2RestTemplate}
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public abstract class GeoServerOAuth2SecurityConfiguration implements OAuth2SecurityConfiguration {

    @Autowired protected Environment env;

    @Resource
    @Qualifier("accessTokenRequest")
    private AccessTokenRequest accessTokenRequest;

    /**
     * Returns the resource bean containing the Access Token Request info.
     *
     * @return the accessTokenRequest
     */
    public AccessTokenRequest getAccessTokenRequest() {
        return accessTokenRequest;
    }

    /**
     * Set the accessTokenRequest property.
     *
     * @param accessTokenRequest the accessTokenRequest to set
     */
    public void setAccessTokenRequest(AccessTokenRequest accessTokenRequest) {
        this.accessTokenRequest = accessTokenRequest;
    }

    /** Details for an OAuth2-protected resource. */
    @Override
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        AuthorizationCodeResourceDetails details = new GeoServerAuthorizationCodeResourceDetails();
        details.setId(getDetailsId());
        details.setGrantType("authorization_code");
        details.setAuthenticationScheme(AuthenticationScheme.header);
        details.setClientAuthenticationScheme(AuthenticationScheme.form);

        return details;
    }

    /** Returns the details id for the AuthorizationCodeResourceDetails. */
    protected String getDetailsId() {
        return "oauth2-client";
    }

    /**
     * Rest template that is able to make OAuth2-authenticated REST requests with the credentials of
     * the provided resource.
     */
    @Override
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {

        OAuth2RestTemplate oAuth2RestTemplate = getOAuth2RestTemplate();

        AuthorizationCodeAccessTokenProvider authorizationCodeAccessTokenProvider =
                authorizationAccessTokenProvider();

        AccessTokenProvider accessTokenProviderChain =
                new AccessTokenProviderChain(
                        Arrays.<AccessTokenProvider>asList(
                                authorizationCodeAccessTokenProvider,
                                new ImplicitAccessTokenProvider(),
                                new ResourceOwnerPasswordAccessTokenProvider(),
                                new ClientCredentialsAccessTokenProvider()));

        oAuth2RestTemplate.setAccessTokenProvider(accessTokenProviderChain);

        return oAuth2RestTemplate;
    }

    protected AuthorizationCodeAccessTokenProvider authorizationAccessTokenProvider() {
        AuthorizationCodeAccessTokenProvider authorizationCodeAccessTokenProvider =
                new AuthorizationCodeAccessTokenProvider();
        authorizationCodeAccessTokenProvider.setStateMandatory(false);
        authorizationCodeAccessTokenProvider.setTokenRequestEnhancer(new DefaultRequestEnhancer());
        return authorizationCodeAccessTokenProvider;
    }

    /**
     * Allows subclasses to return a custom {@link OAuth2RestTemplate} subclass
     *
     * @return
     */
    protected OAuth2RestTemplate getOAuth2RestTemplate() {
        return new OAuth2RestTemplate(
                geoServerOAuth2Resource(), new DefaultOAuth2ClientContext(getAccessTokenRequest()));
    }

    /** Custom {@link AuthorizationCodeResourceDetails} */
    protected static class GeoServerAuthorizationCodeResourceDetails
            extends AuthorizationCodeResourceDetails {

        /** @return true if the client is only a client and does not have any user related */
        @Override
        public boolean isClientOnly() {
            return true;
        }
    };
}
