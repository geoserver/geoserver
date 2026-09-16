/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNoException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks that changing the label of the keys in the GeoServer keystore breaks nothing.
 *
 * <p>Every secret key has an algorithm name. {@link KeyStoreProviderImpl} used to write {@code PBE} and now writes
 * {@code HmacSHA256}. The FIPS keystore refuses labels it does not recognize, so the old name had to go.
 *
 * <p>A JCEKS keystore only saves the label along with the bytes. It never asks a crypto provider whether the label
 * makes sense. These tests show that, so an old data directory still reads after the upgrade, and a new one still reads
 * on an older GeoServer.
 */
public class KeyStoreKeyFormatTest {

    private static final char[] PASSWORD = "geoserver".toCharArray();

    /** The label GeoServer wrote before the change. */
    private static final String LEGACY_ALGORITHM = "PBE";

    private static final byte[] MATERIAL = material();

    /** Same size as a real entry: a 40 character password, two bytes per character. */
    private static byte[] material() {
        byte[] material = new byte[80];
        for (int i = 0; i < material.length; i++) {
            material[i] = (byte) i;
        }
        return material;
    }

    /** Skips the tests where JCEKS is missing, which is the case on a FIPS machine. */
    @Before
    public void assumeLegacyKeyStoreAvailable() {
        try {
            KeyStore.getInstance(KeyStoreProviderImpl.KEYSTORETYPE);
        } catch (KeyStoreException e) {
            assumeNoException(e);
        }
    }

    /** Even a made up label reads back, which shows the keystore does not check it against a provider. */
    @Test
    public void testLabelIsCarriedAsData() throws Exception {
        String unknown = "NoSuchAlgorithmAnywhere";
        assertNull(
                "test is meaningless if something implements it", Security.getProviders("SecretKeyFactory." + unknown));

        Key key = roundTrip(unknown);
        assertEquals(unknown, key.getAlgorithm());
        assertArrayEquals(MATERIAL, key.getEncoded());
    }

    /** The old {@code PBE} label still reads, so a keystore written before the change keeps working. */
    @Test
    public void testLegacyLabelStillReads() throws Exception {
        Key key = roundTrip(LEGACY_ALGORITHM);
        assertEquals(LEGACY_ALGORITHM, key.getAlgorithm());
        assertArrayEquals(MATERIAL, key.getEncoded());
    }

    /** The new label reads through the same code an older GeoServer runs. */
    @Test
    public void testCurrentLabelReads() throws Exception {
        Key key = roundTrip(KeyStoreProviderImpl.KEY_ALGORITHM);
        assertEquals(KeyStoreProviderImpl.KEY_ALGORITHM, key.getAlgorithm());
        assertArrayEquals(MATERIAL, key.getEncoded());
    }

    /**
     * The entries hold random passwords, not cipher keys, so their length matches no cipher. This is why the label
     * cannot be {@code AES}: a checking keystore only takes 16, 24 or 32 bytes under that name.
     */
    @Test
    public void testKeyLengthIsNotConstrained() throws Exception {
        assertEquals(80, MATERIAL.length);
        assertArrayEquals(
                MATERIAL, roundTrip(KeyStoreProviderImpl.KEY_ALGORITHM).getEncoded());
    }

    private static Key roundTrip(String algorithm) throws Exception {
        KeyStore written = KeyStore.getInstance(KeyStoreProviderImpl.KEYSTORETYPE);
        written.load(null, PASSWORD);
        written.setEntry(
                "alias",
                new KeyStore.SecretKeyEntry(new SecretKeySpec(MATERIAL, algorithm)),
                new KeyStore.PasswordProtection(PASSWORD));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        written.store(bytes, PASSWORD);

        KeyStore read = KeyStore.getInstance(KeyStoreProviderImpl.KEYSTORETYPE);
        read.load(new ByteArrayInputStream(bytes.toByteArray()), PASSWORD);
        return read.getKey("alias", PASSWORD);
    }
}
