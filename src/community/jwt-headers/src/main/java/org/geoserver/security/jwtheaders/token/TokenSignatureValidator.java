/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;

/**
 * This validates the signature of the JWT.
 *
 * <p>We cache to things (both for 1 hour) for performance reasons; 1. The JWKSet enpoint (set of
 * public keys we use to check the signature). 2. The validated token
 *
 * <p>This will ensure that the token hasn't been tampered with.
 */
public class TokenSignatureValidator {

    public static LoadingCache<String, JWKSet> jwks =
            CacheBuilder.newBuilder()
                    .maximumSize(50000)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .build(
                            new CacheLoader<String, JWKSet>() {
                                public JWKSet load(String urlStr) throws Exception {

                                    return loadJWKSet(urlStr);
                                }
                            });

    public static Cache<Object, Object> validAccessKeys =
            CacheBuilder.newBuilder()
                    .maximumSize(50000)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .build();

    GeoServerJwtHeadersFilterConfig jwtHeadersConfig;

    public static JWKSet loadJWKSet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        return JWKSet.load(url);
    }

    public TokenSignatureValidator(GeoServerJwtHeadersFilterConfig config) {
        jwtHeadersConfig = config;
    }

    public void validate(String accessToken) throws Exception {
        if (!jwtHeadersConfig.isValidateTokenSignature()) return; // don't validate

        if (validAccessKeys.getIfPresent(accessToken) != null)
            return; // we already know this is a good accessToken

        JWSObject jwsToken = JWSObject.parse(accessToken);
        String keyId = jwsToken.getHeader().getKeyID();

        var publicKeys = jwks.get(jwtHeadersConfig.getValidateTokenSignatureURL());

        RSAKey rsaKey = (RSAKey) publicKeys.getKeyByKeyId(keyId);
        validateSignature(rsaKey, jwsToken);

        // its good - put in cache, so we don't do the signature validation all the time.
        validAccessKeys.put(accessToken, Boolean.TRUE);
    }

    /**
     * Given a publickey and a (parsed) token, verify that the signature is correct. Throws if the
     * signature is bad.
     *
     * @param rsaPublicKey Public key used to validate token signature
     * @param token three part token
     * @throws Exception
     */
    public void validateSignature(RSAKey rsaPublicKey, JWSObject token) throws Exception {

        JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey);

        var valid = token.verify(verifier);
        if (!valid) {
            throw new Exception(
                    "Could not verify signature of the JWT with the given RSA Public Key");
        }
    }
}
