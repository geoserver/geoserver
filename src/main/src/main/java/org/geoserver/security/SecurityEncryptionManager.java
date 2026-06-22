/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geotools.util.logging.Logging;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for decrypting browser-encrypted login credentials before authentication.
 *
 * @author ziyu
 */
public class SecurityEncryptionManager {

    public static final String CRYPT_PREFIX = "CRYPT:";
    private static final Logger LOGGER = Logging.getLogger(SecurityEncryptionManager.class);

    private final KeyPair keyPair;

    public SecurityEncryptionManager() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to initialize RSA login encryption", e);
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public String decryptLoginCredential(String credential) {
        if (credential == null || !credential.startsWith(CRYPT_PREFIX)) {
            return credential;
        }

        try {
            String encrypted = credential.substring(CRYPT_PREFIX.length());
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to decrypt RSA login credential", e);
            return credential;
        }
    }
}
