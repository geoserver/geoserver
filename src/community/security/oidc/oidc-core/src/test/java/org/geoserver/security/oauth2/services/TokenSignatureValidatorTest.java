/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.locationtech.jts.util.Assert;

public class TokenSignatureValidatorTest {

    @Test
    public void testLoadKeys() throws Exception {
        var tokenValidator = new TokenSignatureValidator();

        String tokenBase64 = IOUtils.toString(getClass().getResourceAsStream("token"), "UTF-8");
        // id or access token - it has 3 parts
        // 1) header - has the keyid for the public key in the JWKSet
        // 2) body - Map of the claims
        // 3) footer - Signature

        JWSObject token = tokenValidator.parseToken(tokenBase64);
        String keyId = token.getHeader().getKeyID();

        // JWKSet publicKeys = JWKSet.load(new
        // URL("https://login-live-dev.geocat.live/realms/dave-realm/protocol/openid-connect/certs"));
        String certs = IOUtils.toString(getClass().getResourceAsStream("certs.json"), "UTF-8");
        JWKSet publicKeys = JWKSet.parse(certs);

        var jwk = publicKeys.getKeyByKeyId(keyId);

        // could be a different type of key, but this should be true in most cases
        Assert.isTrue(jwk instanceof RSAKey);
        var rsa = (RSAKey) jwk;
        tokenValidator.validateSignature(rsa, token);
    }

    @Test
    public void testGoodSignature() throws Exception {

        var tokenValidator = new TokenSignatureValidator();

        String tokenBase64 = IOUtils.toString(getClass().getResourceAsStream("token"), "UTF-8");

        var token = tokenValidator.parseToken(tokenBase64);
        var keyId = token.getHeader().getKeyID();

        Assert.equals("U_U7yu1N8uJOWEX4QhT3qazCcNnGNWXS3kfRJ-Tdy8M", keyId);

        String key = IOUtils.toString(getClass().getResourceAsStream("publicKey"), "UTF-8");
        var publicKey = tokenValidator.CreatePublicKey(key, "AQAB");

        tokenValidator.validateSignature(publicKey, token);
    }
}
