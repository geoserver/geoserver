/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class AesCbcCryptTest {

    private static final String URL = "/web/wicket/bookmarkable/org.geoserver.web.demo.MapPreviewPage?0";

    private static final SecretKey KEY = key(1);
    private static final SecretKey OTHER_KEY = key(2);

    /** A key whose bytes depend only on the seed, so the tests can compare two sessions. */
    private static SecretKey key(int seed) {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * seed);
        }
        return new SecretKeySpec(bytes, "AES");
    }

    @Test
    void testRoundTrip() {
        AesCbcCrypt crypt = new AesCbcCrypt(KEY);

        String encrypted = crypt.encryptUrlSafe(URL);

        assertNotEquals(URL, encrypted);
        assertEquals(URL, crypt.decryptUrlSafe(encrypted));
    }

    /** Wicket's {@code CryptoMapper} encrypts a URL again to compare it, so the same key must give the same text. */
    @Test
    void testSameKeyGivesSameText() {
        assertEquals(new AesCbcCrypt(KEY).encryptUrlSafe(URL), new AesCbcCrypt(KEY).encryptUrlSafe(URL));
    }

    /** The vector comes from the key, so the same URL in two sessions does not encrypt to the same text. */
    @Test
    void testDifferentKeysGiveDifferentText() {
        assertNotEquals(new AesCbcCrypt(KEY).encryptUrlSafe(URL), new AesCbcCrypt(OTHER_KEY).encryptUrlSafe(URL));
    }

    /**
     * The fixed vector makes two URLs with the same start share their first blocks, 16 bytes at a time. Base64 turns
     * each 3 bytes into 4 characters, so the first shared block is 20 shared characters.
     */
    @Test
    void testSharedPrefixSharesWholeBlocks() {
        AesCbcCrypt crypt = new AesCbcCrypt(KEY);

        String one = crypt.encryptUrlSafe("/web/wicket/resource/one");
        String two = crypt.encryptUrlSafe("/web/wicket/resource/two");

        assertEquals(one.substring(0, 20), two.substring(0, 20));
        assertNotEquals(one, two);
    }

    /** Wicket answers null when a text does not decrypt, and another session's key does not decrypt this one. */
    @Test
    void testDecryptsOnlyWithTheSameKey() {
        String encrypted = new AesCbcCrypt(KEY).encryptUrlSafe(URL);

        assertNull(new AesCbcCrypt(OTHER_KEY).decryptUrlSafe(encrypted));
    }
}
