package org.geoserver.security.oauth2.services;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

import java.text.ParseException;

public class TokenSignatureValidator {

    /***
     * Given an publickey and a (parsed) token, verify that the signature is correct.
     * Throws if the signature is bad.
     *
     * @param rsaPublicKey
     * @param token
     * @throws Exception
     */
    public void ValidateSignature(RSAKey rsaPublicKey, JWSObject token) throws  Exception {
        JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey);
        var ok = token.verify(verifier);
        if (!ok) {
            throw new Exception("Could not verify signature of the JWT with the given RSA Public Key");
        }
    }

    /***
     * Given Base64 encoded N and E, create an RSA public key.
     * @param n
     * @param e
     * @return
     */
    public RSAKey CreatePublicKey(String n, String e){
        var publicKeyBuilder =  new  RSAKey.Builder(
                new Base64URL(n),
                new Base64URL(e)
        );
        RSAKey rsaPublicKey = publicKeyBuilder.build();
        return rsaPublicKey;
    }

    /***
     * given a base64 token, parse it.
     * NOTE: this should be a 3 part token (i.e. with 3 "." in the base64).
     *
     * @param tokenBase64
     * @return
     * @throws ParseException
     */
    public JWSObject ParseToken(String tokenBase64) throws ParseException {
        var jwsObject = JWSObject.parse(tokenBase64);
        return jwsObject;
    }


}
