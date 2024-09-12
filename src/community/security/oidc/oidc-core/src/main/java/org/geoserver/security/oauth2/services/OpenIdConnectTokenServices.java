/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.security.oauth2.GeoServerAccessTokenConverter;
import org.geoserver.security.oauth2.GeoServerOAuthRemoteTokenServices;
import org.geoserver.security.oauth2.GeoServerUserAuthenticationConverter;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.jwt.crypto.sign.InvalidSignatureException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkTokenStore;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** Remote Token Services for OpenId token details. */
public class OpenIdConnectTokenServices extends GeoServerOAuthRemoteTokenServices {

    private JwkTokenStore store = null;
    private JWKSet publicKeys;

    public OpenIdConnectTokenServices() {
        super(new GeoServerAccessTokenConverter());
    }

    /**
     * According to the spec, the token can be verified issuing a GET request, and putting the token
     * in the Authorization header. See
     * https://openid.net/specs/openid-connect-core-1_0.html#UserInfo
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, Object> checkToken(String accessToken) {
        if (this.checkTokenEndpointUrl != null) {
            return checkTokenEndpoint(this.checkTokenEndpointUrl, accessToken);
        } else if (this.store != null) {
            return checkTokenJWKS(accessToken);
        } else {
            throw new IllegalStateException(
                    "Unable to check access token: require check token endpoint url, or JWK URI");
        }
    }

    /**
     * According to the spec, the token can be verified issuing a GET request, and putting the token
     * in the Authorization header. See
     * https://openid.net/specs/openid-connect-core-1_0.html#UserInfo
     */
    protected Map<String, Object> checkTokenEndpoint(
            String checkTokenEndpoint, String accessToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthorizationHeader(accessToken));
        return restTemplate
                .exchange(
                        checkTokenEndpoint,
                        HttpMethod.GET,
                        new HttpEntity<>(formData, headers),
                        Map.class)
                .getBody();
    }

    /**
     * According to the spec, the token signature can be checked using public JSON Web Key set
     * document.
     */
    protected Map<String, Object> checkTokenJWKS(String rawAccessToken) {
        // use
        try {
            TokenSignatureValidator tokenValidator = new TokenSignatureValidator();
            JWSObject jwsToken = tokenValidator.parseToken(rawAccessToken);
            String keyId = jwsToken.getHeader().getKeyID();

            RSAKey rsaKey = (RSAKey) publicKeys.getKeyByKeyId(keyId);
            tokenValidator.validateSignature(rsaKey, jwsToken);
        } catch (ParseException parseException) {
            throw (InvalidSignatureException)
                    new InvalidSignatureException(
                                    "Could not verify signature of the JWT with the given RSA Public Key")
                            .initCause(parseException);
        }
        // verify access token - this did not validate signature as initially assumed
        OAuth2AccessToken accessToken = store.readAccessToken(rawAccessToken);

        // To be careful check if accessToken has expired as we do not rely on check token endpoint
        Date expiration = accessToken.getExpiration();
        if (Instant.now().isAfter(expiration.toInstant())) {
            throw new InvalidTokenException("Provided accessToken has expired");
        }

        // Extract information from token
        Map<String, Object> map = new HashMap<>();
        map.put("scope", accessToken.getScope());
        map.putAll(accessToken.getAdditionalInformation());

        return map;
    }

    @Override
    protected void verifyTokenResponse(String accessToken, Map<String, Object> checkTokenResponse) {
        if (checkTokenResponse.containsKey("message")
                && checkTokenResponse.get("message").toString().startsWith("Problems")) {
            logger.debug("check_token returned error: " + checkTokenResponse.get("message"));
            throw new InvalidTokenException(accessToken);
        }
    }

    /** Overriden to remove the assertion about remote tokens */
    @Override
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws AuthenticationException, InvalidTokenException {
        Map<String, Object> checkTokenResponse = checkToken(accessToken);

        verifyTokenResponse(accessToken, checkTokenResponse);

        transformNonStandardValuesToStandardValues(checkTokenResponse);

        return tokenConverter.extractAuthentication(checkTokenResponse);
    }

    public void setConfiguration(OpenIdConnectFilterConfig config) {
        if (config.getJwkURI() != null && !config.getJwkURI().isEmpty()) {
            this.store = new JwkTokenStore(config.getJwkURI());
            try {
                this.publicKeys = JWKSet.load(new URL(config.getJwkURI()));
            } catch (IOException e) {
                this.publicKeys = null;
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.publicKeys = null;
            this.store = null;
        }

        setAccessTokenConverter(
                new GeoServerAccessTokenConverter(
                        new GeoServerUserAuthenticationConverter(config.getPrincipalKey())));
    }
}
