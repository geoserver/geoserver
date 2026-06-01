/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Jasypt compatible password digester using random salted, iterated SHA-256 hashing.
 *
 * <p>Encodes passwords as {@code Base64(salt || hash)}.
 *
 * <p><strong>Note:</strong> This is not a modern password hashing algorithm and should only be used for compatibility.
 */
final class LegacyPasswordByteDigester {
    private static final Logger LOGGER = Logging.getLogger(LegacyPasswordByteDigester.class);
    private static final String ALGORITHM = "SHA-256";
    private static final int ITERATIONS = 100_000;
    private static final int SALT_LENGTH = 16;

    private static SecureRandom random() {
        return SecureRandomHolder.INSTANCE;
    }

    private static final class SecureRandomHolder {
        private static final SecureRandom INSTANCE = new SecureRandom();
    }

    /**
     * Encodes the given password.
     *
     * @param password password bytes
     * @return Base64-encoded salt and hash
     */
    public String encode(byte[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        random().nextBytes(salt);

        byte[] hash = digest(password, salt);
        return Base64.getEncoder().encodeToString(concat(salt, hash));
    }

    /**
     * Verifies a password against an encoded password
     *
     * @param password password bytes
     * @param encodedPassword Base64(salt || hash)
     * @return {@code true} if the password matches
     */
    public boolean matches(byte[] password, String encodedPassword) {
        if (password == null || encodedPassword == null) {
            LOGGER.fine("Password or encodedPassword is null");
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encodedPassword);

            if (decoded.length <= SALT_LENGTH) {
                LOGGER.fine("Encoded password has invalid length");
                return false;
            }

            byte[] salt = Arrays.copyOfRange(decoded, 0, SALT_LENGTH);
            byte[] hash = Arrays.copyOfRange(decoded, SALT_LENGTH, decoded.length);

            return MessageDigest.isEqual(hash, digest(password, salt));
        } catch (IllegalArgumentException e) {
            LOGGER.fine("Invalid Base64 in encoded password");
            return false;
        }
    }

    private byte[] digest(byte[] password, byte[] salt) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
            byte[] result = messageDigest.digest(concat(salt, password));

            for (int i = 1; i < ITERATIONS; i++) {
                result = messageDigest.digest(result);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing hashing algorithm: " + ALGORITHM, e);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
