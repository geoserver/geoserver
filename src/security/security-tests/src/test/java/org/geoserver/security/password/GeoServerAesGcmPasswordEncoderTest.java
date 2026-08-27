/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.SecretKey;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.KeyStoreProviderImpl;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;

/** Checks the encoder that replaces the password based ones where those cannot run, most notably under FIPS. */
public class GeoServerAesGcmPasswordEncoderTest extends GeoServerSystemTestSupport {

    private static final String PASSWORD = "geoserver";
    private static final char[] PASSWORD_ARRAY = PASSWORD.toCharArray();

    private GeoServerAesGcmPasswordEncoder encoder;

    @Before
    public void loadEncoder() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        encoder = manager.loadPasswordEncoder(GeoServerAesGcmPasswordEncoder.class);
        encoder.initialize(manager);
    }

    @Test
    public void testEncodingTypeAndPrefix() {
        assertEquals(PasswordEncodingType.ENCRYPT, encoder.getEncodingType());
        assertEquals("crypt3", encoder.getPrefix());
        assertTrue(encoder.encodePassword(PASSWORD, null)
                .startsWith("crypt3" + AbstractGeoserverPasswordEncoder.PREFIX_DELIMTER));
    }

    @Test
    public void testStringRoundTrip() {
        String encoded = encoder.encodePassword(PASSWORD, null);
        assertTrue(encoder.isPasswordValid(encoded, PASSWORD, null));
        assertEquals(PASSWORD, encoder.decode(encoded));
    }

    @Test
    public void testCharArrayRoundTrip() {
        String encoded = encoder.encodePassword(PASSWORD_ARRAY, null);
        assertTrue(encoder.isPasswordValid(encoded, PASSWORD_ARRAY, null));
        assertArrayEquals(PASSWORD_ARRAY, encoder.decodeToCharArray(encoded));
    }

    @Test
    public void testStringAndCharArrayFormsInterchange() {
        assertTrue(encoder.isPasswordValid(encoder.encodePassword(PASSWORD, null), PASSWORD_ARRAY, null));
        assertTrue(encoder.isPasswordValid(encoder.encodePassword(PASSWORD_ARRAY, null), PASSWORD, null));
    }

    @Test
    public void testWrongPasswordRejected() {
        String encoded = encoder.encodePassword(PASSWORD, null);
        assertFalse(encoder.isPasswordValid(encoded, "geoserve", null));
        assertFalse(encoder.isPasswordValid(encoded, "geoserverr", null));
        assertFalse(encoder.isPasswordValid(encoded, "", null));
    }

    @Test
    public void testEmptyPassword() {
        String encoded = encoder.encodePassword("", null);
        assertTrue(encoder.isPasswordValid(encoded, "", null));
        assertFalse(encoder.isPasswordValid(encoded, PASSWORD, null));
    }

    /** A new initialization vector for every value, or equal passwords would look equal in the catalog. */
    @Test
    public void testRepeatedEncodingDiffers() {
        assertNotEquals(encoder.encodePassword(PASSWORD, null), encoder.encodePassword(PASSWORD, null));
    }

    /** What GCM adds over the older encoders: a changed value is refused instead of decoding into nonsense. */
    @Test
    public void testTamperedValueRejected() {
        String encoded = encoder.encodePassword(PASSWORD, null);
        byte[] raw = Base64.getDecoder().decode(encoded.substring("crypt3:".length()));
        raw[raw.length - 1] ^= 0x01;
        String tampered = "crypt3:" + Base64.getEncoder().encodeToString(raw);

        assertThrows(RuntimeException.class, () -> encoder.decode(tampered));
    }

    @Test
    public void testTruncatedValueRejected() {
        assertThrows(
                RuntimeException.class,
                () -> encoder.decode("crypt3:" + Base64.getEncoder().encodeToString(new byte[4])));
    }

    /**
     * Making the key takes hundreds of milliseconds, and a new encoder is built for every operation, so the key has to
     * be kept in the keystore and not in the encoder.
     */
    @Test
    public void testEncodingLeavesTheDerivedKeyCached() throws Exception {
        encoder.encodePassword(PASSWORD, null);

        AtomicInteger derivations = new AtomicInteger();
        SecretKey cached = getSecurityManager()
                .getKeyStoreProvider()
                .getDerivedKey(KeyStoreProviderImpl.CONFIGPASSWORDKEY, secret -> {
                    derivations.incrementAndGet();
                    return AesGcmCipher.deriveKey(secret);
                });

        assertEquals(0, derivations.get());
        assertEquals("AES", cached.getAlgorithm());
    }

    /**
     * Encoding happens while serving a request, and nothing puts those calls in order. A keystore write there could
     * happen twice, one of the two salts would win, and whatever was encrypted with the other could never be decrypted.
     */
    @Test
    public void testEncodingDoesNotWriteToTheKeyStore() throws Exception {
        KeyStoreProvider keyStore = getSecurityManager().getKeyStoreProvider();
        long modified = keyStore.getResource().lastmodified();

        String encoded = encoder.encodePassword(PASSWORD, null);
        encoder.decode(encoded);

        assertEquals(modified, keyStore.getResource().lastmodified());
    }

    /** The encoder that dispatches by prefix has to know this one, that is how stored values are read back. */
    @Test
    public void testReadableThroughMultiplexingEncoder() throws Exception {
        GeoServerMultiplexingPasswordEncoder multiplexing =
                new GeoServerMultiplexingPasswordEncoder(getSecurityManager());
        String encoded = encoder.encodePassword(PASSWORD, null);

        assertTrue(multiplexing.isPasswordValid(encoded, PASSWORD, null));
        assertEquals(PASSWORD, multiplexing.decode(encoded));
        assertArrayEquals(PASSWORD_ARRAY, multiplexing.decodeToCharArray(encoded));
    }
}
