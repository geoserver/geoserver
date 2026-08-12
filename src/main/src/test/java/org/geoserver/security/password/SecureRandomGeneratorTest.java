/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.salt.SaltGenerator;
import org.junit.Test;

/**
 * Guards the upgrade path. Changing the salt generator must not change the stored format, or every password already in
 * a data directory becomes unreadable.
 */
public class SecureRandomGeneratorTest {

    private static final String ALGORITHM = "PBEWITHMD5ANDDES";
    private static final char[] KEY = "masterkey".toCharArray();
    private static final byte[] SECRET = "s3cret".getBytes(StandardCharsets.UTF_8);

    @Test
    public void testSaltStaysInEncryptionResult() {
        assertTrue(SecureRandomGenerator.INSTANCE.includePlainSaltInEncryptionResults());
    }

    @Test
    public void testDecryptsValuesWrittenByPreviousGenerator() {
        assumeAlgorithmAvailable();
        byte[] legacy = encryptor(new RandomSaltGenerator()).encrypt(SECRET);
        assertArrayEquals(SECRET, encryptor(SecureRandomGenerator.INSTANCE).decrypt(legacy));
    }

    /** A downgrade has to keep working too, the format is the same in both directions. */
    @Test
    public void testPreviousGeneratorDecryptsCurrentValues() {
        assumeAlgorithmAvailable();
        byte[] current = encryptor(SecureRandomGenerator.INSTANCE).encrypt(SECRET);
        assertArrayEquals(SECRET, encryptor(new RandomSaltGenerator()).decrypt(current));
    }

    private static StandardPBEByteEncryptor encryptor(SaltGenerator saltGenerator) {
        StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
        encryptor.setPasswordCharArray(KEY);
        encryptor.setSaltGenerator(saltGenerator);
        encryptor.setAlgorithm(ALGORITHM);
        return encryptor;
    }

    /**
     * The old format uses algorithms no FIPS provider has, so this comparison only means something in a regular build.
     * That is also the only place where the upgrade path has to work.
     */
    private static void assumeAlgorithmAvailable() {
        assumeTrue(Security.getProviders("SecretKeyFactory." + ALGORITHM) != null);
    }
}
