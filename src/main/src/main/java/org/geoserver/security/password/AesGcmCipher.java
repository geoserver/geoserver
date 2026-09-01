/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.scramble;
import static org.geoserver.security.SecurityUtils.toBytes;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Makes a key from a password, then encrypts with it. Two steps, because no FIPS provider offers a {@code Cipher} that
 * takes a password directly. Every algorithm used here works on any provider GeoServer runs with, FIPS included.
 *
 * <p>The result is the initialization vector followed by the encrypted bytes, which already include the GCM
 * authentication tag.
 */
public final class AesGcmCipher {

    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    /** Length of the derivation salt, the 16 bytes NIST SP 800-132 section 5.1 asks for. */
    static final int SALT_LENGTH = 16;

    /** The initialization vector length GCM is meant to use; a longer one gets hashed down by the cipher anyway. */
    private static final int IV_LENGTH = 12;

    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BITS = 256;

    /**
     * The count OWASP recommends for PBKDF2 with HMAC-SHA256, see <a
     * href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">the password storage
     * cheat sheet</a>. NIST SP 800-132 section 5.2 only sets a floor of 1000, which no longer costs an attacker
     * anything. It takes about 100ms, and callers cache the key they get, so it is not paid per encrypted value.
     */
    private static final int ITERATIONS = 600_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private AesGcmCipher() {}

    /** Random bytes from the JVM default source, for salts and other non secret material. */
    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Derives an AES key from a stored secret, salting it with the secret itself.
     *
     * <p>A salt has to be different for each key, but it does not have to be secret or random. GeoServer stores random
     * passwords of about 80 characters, one per installation and per user group service, so a digest of one is already
     * unique.
     *
     * <p>A random salt would have to be stored next to the secret it salts, which helps nothing against someone who can
     * read the keystore. It would also have to be created while encoding, where two threads can create two different
     * salts, and then whatever the losing thread encrypted can never be decrypted.
     */
    public static SecretKey deriveKey(char[] secret) {
        return deriveKey(secret, salt(secret));
    }

    private static byte[] salt(char[] secret) {
        byte[] bytes = toBytes(secret);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return Arrays.copyOf(digest, SALT_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not derive the encryption salt", e);
        } finally {
            scramble(bytes);
        }
    }

    /** Derives an AES key with a caller supplied salt, for the master password, which keeps its own. */
    static SecretKey deriveKey(char[] password, byte[] salt) {
        try {
            byte[] derived = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
                    .generateSecret(new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS))
                    .getEncoded();
            return new SecretKeySpec(derived, "AES");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not derive an encryption key", e);
        }
    }

    /** Encrypts with a fresh initialization vector, returned as a prefix of the result. */
    public static byte[] encrypt(SecretKey key, byte[] plainText) {
        byte[] iv = randomBytes(IV_LENGTH);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText);

            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not encrypt", e);
        }
    }

    /**
     * Reverses {@link #encrypt(SecretKey, byte[])} and throws when the input was modified. GCM checks the data along
     * with decrypting it, so a changed value is refused instead of turning into meaningless bytes.
     */
    public static byte[] decrypt(SecretKey key, byte[] ivAndCipherText) {
        if (ivAndCipherText.length <= IV_LENGTH) {
            throw new RuntimeException("Encrypted value is too short to hold an initialization vector");
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, ivAndCipherText, 0, IV_LENGTH));
            return cipher.doFinal(ivAndCipherText, IV_LENGTH, ivAndCipherText.length - IV_LENGTH);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not decrypt", e);
        }
    }
}
