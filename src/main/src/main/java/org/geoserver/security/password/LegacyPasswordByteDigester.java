/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Legacy password digester using salted, iterated SHA-256 hashing.
 *
 * <p>Encodes passwords as {@code Base64(salt || hash)}.
 *
 * <p><strong>Note:</strong> This is not a modern password hashing algorithm (e.g. PBKDF2, bcrypt) and should only be
 * used for compatibility.
 *
 * @author mortenlud
 */
final class LegacyPasswordByteDigester {
    private static final String ALGORITHM = "SHA-256";
    private static final int ITERATIONS = 100_000;
    private static final int SALT_LENGTH = 16;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Encodes the given password.
     *
     * @param password password bytes
     * @return Base64-encoded salt and hash
     */
    public String encode(byte[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        byte[] hash = digest(password, salt);
        return Base64.getEncoder().encodeToString(concat(salt, hash));
    }

    /**
     * Verifies a password against a stored value.
     *
     * @param password password bytes
     * @param encodedPassword Base64(salt || hash)
     * @return {@code true} if the password matches
     * @throws IllegalArgumentException if the encoded value is invalid
     */
    public boolean matches(byte[] password, String encodedPassword) {
        byte[] decoded = Base64.getDecoder().decode(encodedPassword);
        byte[] salt = new byte[SALT_LENGTH];
        byte[] hash = new byte[decoded.length - SALT_LENGTH];

        System.arraycopy(decoded, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(decoded, SALT_LENGTH, hash, 0, hash.length);

        return MessageDigest.isEqual(hash, digest(password, salt));
    }

    private byte[] digest(byte[] password, byte[] salt) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
            byte[] result = concat(salt, password);

            for (int i = 0; i < ITERATIONS; i++) {
                result = messageDigest.digest(result);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
