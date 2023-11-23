/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import java.text.ParseException;
import org.springframework.security.jwt.crypto.sign.InvalidSignatureException;

public class TokenSignatureValidator {

    /**
     * * Given an publickey and a (parsed) token, verify that the signature is correct. Throws if
     * the signature is bad.
     *
     * @param rsaPublicKey Public key used to validate token signature
     * @param token three part token
     * @throws InvalidSignatureException
     */
    public void validateSignature(RSAKey rsaPublicKey, JWSObject token)
            throws InvalidSignatureException {
        try {
            JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey);

            var valid = token.verify(verifier);
            if (!valid) {
                throw new InvalidSignatureException(
                        "Could not verify signature of the JWT with the given RSA Public Key");
            }
        } catch (JOSEException unableToValidate) {
            throw (InvalidSignatureException)
                    new InvalidSignatureException(
                                    "Could not verify signature of the JWT with the given RSA Public Key")
                            .initCause(unableToValidate);
        }
    }

    /**
     * * Given Base64 encoded N and E, create an RSA public key.
     *
     * @param n
     * @param e
     * @return RSA public key
     */
    public RSAKey CreatePublicKey(String n, String e) {
        var publicKeyBuilder = new RSAKey.Builder(new Base64URL(n), new Base64URL(e));
        RSAKey rsaPublicKey = publicKeyBuilder.build();
        return rsaPublicKey;
    }

    /**
     * * Given a base64 token, parse it.
     *
     * <p>NOTE: this should be a 3 part token (i.e. with three parts seperated by {@code "."} in the
     * base64).
     *
     * @param tokenBase64 JWS string encoded in base64
     * @return JWSObject with three parts seperated by {@code "."} in tokenBase64
     * @throws ParseException
     */
    public JWSObject parseToken(String tokenBase64) throws ParseException {
        var jwsObject = JWSObject.parse(tokenBase64);
        return jwsObject;
    }
}
