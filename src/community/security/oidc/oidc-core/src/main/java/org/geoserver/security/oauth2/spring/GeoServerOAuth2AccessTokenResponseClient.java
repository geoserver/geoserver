/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.geoserver.security.oauth2.common.ConfidentialLogger;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.util.Assert;

/**
 * {@link OAuth2AccessTokenResponseClient} allows to log confidential access token details to support trouble shooting
 * OIDC providers.
 *
 * @author awaterme
 */
public class GeoServerOAuth2AccessTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    /**
     * @param pDelegate
     * @param pJwtDecoderFactory
     */
    public GeoServerOAuth2AccessTokenResponseClient(
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> pDelegate,
            JwtDecoderFactory<ClientRegistration> pJwtDecoderFactory) {
        super();
        delegate = pDelegate;
        jwtDecoderFactory = pJwtDecoderFactory;
        Assert.notNull(delegate, "delegate must not be null");
        Assert.notNull(jwtDecoderFactory, "jwtDecoderFactory must not be null");
    }

    private static Logger LOGGER = Logging.getLogger(GeoServerOAuth2AccessTokenResponseClient.class);

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate;
    private JwtDecoderFactory<ClientRegistration> jwtDecoderFactory = new OidcIdTokenDecoderFactory();

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest pRequest) {
        OAuth2AccessTokenResponse lTokenResponse;
        try {
            lTokenResponse = delegate.getTokenResponse(pRequest);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Error obtaining token response.", e);
            throw e;
        }
        try {
            debugLog(pRequest, lTokenResponse);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error collecting data for logging.", e);
        }
        return lTokenResponse;
    }

    private void debugLog(OAuth2AuthorizationCodeGrantRequest pRequest, OAuth2AccessTokenResponse lTokenResponse) {
        if (!ConfidentialLogger.isLoggable(Level.FINE)) {
            return;
        }
        boolean lIsCommonProvider = false;
        ClientRegistration lClientReg = pRequest.getClientRegistration();
        String lRegId = lClientReg.getRegistrationId();
        if (!REG_ID_OIDC.equals(lRegId)) {
            lIsCommonProvider = true;
        }
        OAuth2AuthorizationExchange lExchange = pRequest.getAuthorizationExchange();
        OAuth2AuthorizationResponse lAuthResp = lExchange.getAuthorizationResponse();
        OAuth2AccessToken lAccessToken = lTokenResponse.getAccessToken();

        String lAuthCode = lAuthResp.getCode();
        OAuth2AccessToken.TokenType lType = lAccessToken.getTokenType();
        Set<String> lScopes = lAccessToken.getScopes();
        Map<String, Object> lAdditionals = new HashMap<>(lTokenResponse.getAdditionalParameters());

        String lTokenValue = lAccessToken.getTokenValue();
        Jwt lTokenJwt = null;
        if (lTokenValue != null && lTokenValue.indexOf(".") > 0) {
            lTokenJwt = parseToken(lClientReg, lTokenResponse, lTokenValue);
        }

        String lIdTokenValue = (String) lAdditionals.get(ID_TOKEN);
        Jwt lIdToken = null;
        if (lIdTokenValue != null) {
            lAdditionals.remove(ID_TOKEN);
            lIdToken = parseToken(lClientReg, lTokenResponse, lIdTokenValue);
        }

        if (lIsCommonProvider) {
            // common: omit some confidential values not required for attribute analysis
            String lMsg = "Access token received from {0} with accessTokenType={1}, "
                    + "scopes={2}, accessTokenHeaders={3}, "
                    + "accessTokenClaims={4}, additionalParameters={5}, "
                    + "idTokenHeaders={6}, idTokenClaims={7}";
            String[] lParams = new String[8];
            lParams[0] = lRegId;
            lParams[1] = lType == null ? null : lType.getValue();
            lParams[2] = lScopes.stream().collect(Collectors.joining(","));
            lParams[3] = lTokenJwt == null ? null : lTokenJwt.getHeaders().toString();
            lParams[4] = lTokenJwt == null ? null : lTokenJwt.getClaims().toString();
            lParams[5] = lAdditionals.toString();
            lParams[6] = lIdToken == null ? null : lIdToken.getHeaders().toString();
            lParams[7] = lIdToken == null ? null : lIdToken.getClaims().toString();
            ConfidentialLogger.log(Level.FINE, lMsg, lParams);
        } else {
            String lMsg = "Access token received for {0} with authorizationCode={1}, accessTokenType={2}, "
                    + "scopes={3}, accessTokenValue={4}, accessTokenHeaders={5}, "
                    + "accessTokenClaims={6}, additionalParameters={7}, idTokenValue={8}, "
                    + "idTokenHeaders={9}, idTokenClaims={10}";
            String[] lParams = new String[11];
            lParams[0] = lRegId;
            lParams[1] = lAuthCode;
            lParams[2] = lType == null ? null : lType.getValue();
            lParams[3] = lScopes.stream().collect(Collectors.joining(","));
            lParams[4] = tokenValueString(lTokenValue);
            lParams[5] = lTokenJwt == null ? null : lTokenJwt.getHeaders().toString();
            lParams[6] = lTokenJwt == null ? null : lTokenJwt.getClaims().toString();
            lParams[7] = lAdditionals.toString();
            lParams[8] = tokenValueString(lIdTokenValue);
            lParams[9] = lIdToken == null ? null : lIdToken.getHeaders().toString();
            lParams[10] = lIdToken == null ? null : lIdToken.getClaims().toString();
            ConfidentialLogger.log(Level.FINE, lMsg, lParams);
        }
    }

    private String tokenValueString(String pTokenValue) {
        if (pTokenValue == null) {
            return null;
        }
        String[] lSplitted = pTokenValue.split(Pattern.quote("."));
        if (lSplitted.length == 3) {
            return "body:" + lSplitted[1];
        }
        return "opaque:" + pTokenValue;
    }

    private Jwt parseToken(
            ClientRegistration clientRegistration, OAuth2AccessTokenResponse accessTokenResponse, String pValue) {
        try {
            JwtDecoder jwtDecoder = this.jwtDecoderFactory.createDecoder(clientRegistration);
            Jwt jwt = jwtDecoder.decode(pValue);
            return jwt;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing token data for logging.", e);
            return null;
        }
    }
}
