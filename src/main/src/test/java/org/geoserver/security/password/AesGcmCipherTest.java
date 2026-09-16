/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.SecretKey;
import org.junit.BeforeClass;
import org.junit.Test;

/** Checks the key making and the encryption that the FIPS capable encoders are built on. */
public class AesGcmCipherTest {

    private static final char[] SECRET = "a stored secret long enough to derive from".toCharArray();
    private static final char[] OTHER_SECRET = "another stored secret, also long enough".toCharArray();
    private static final byte[] PLAIN_TEXT = "geoserver".getBytes(StandardCharsets.UTF_8);

    /** Making a key takes hundreds of milliseconds on purpose, so tests that only encrypt share these. */
    private static SecretKey key;

    private static SecretKey otherKey;

    @BeforeClass
    public static void deriveKeys() {
        key = AesGcmCipher.deriveKey(SECRET);
        otherKey = AesGcmCipher.deriveKey(OTHER_SECRET);
    }

    /** The salt comes from the secret, so the key can be made again with nothing else stored next to it. */
    @Test
    public void testDerivationRepeatsForTheSameSecret() {
        assertArrayEquals(
                AesGcmCipher.deriveKey(SECRET).getEncoded(),
                AesGcmCipher.deriveKey(SECRET.clone()).getEncoded());
    }

    @Test
    public void testDerivationDiffersPerSecret() {
        assertFalse(Arrays.equals(key.getEncoded(), otherKey.getEncoded()));
    }

    /** Encrypting one value twice gives two different results, each with its own initialization vector. */
    @Test
    public void testEncryptionUsesAFreshVector() {
        byte[] first = AesGcmCipher.encrypt(key, PLAIN_TEXT);
        byte[] second = AesGcmCipher.encrypt(key, PLAIN_TEXT);

        assertFalse(Arrays.equals(first, second));
        assertArrayEquals(PLAIN_TEXT, AesGcmCipher.decrypt(key, first));
        assertArrayEquals(PLAIN_TEXT, AesGcmCipher.decrypt(key, second));
    }

    @Test
    public void testDecryptionWithAnotherKeyIsRefused() {
        byte[] encrypted = AesGcmCipher.encrypt(key, PLAIN_TEXT);

        assertThrows(RuntimeException.class, () -> AesGcmCipher.decrypt(otherKey, encrypted));
    }
}
