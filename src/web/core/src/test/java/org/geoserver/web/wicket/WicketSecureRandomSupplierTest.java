/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import org.geoserver.security.CryptoProviders;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class WicketSecureRandomSupplierTest {

    /** On a normal JDK an unnamed instance would be SHA1PRNG, so the name has to be asked for. */
    @Test
    void testPrefersDrbgWhereTheJvmOffersIt() {
        assumeTrue(Security.getProviders("SecureRandom.DRBG") != null);

        assertEquals("DRBG", WicketSecureRandomSupplier.createSecureRandom().getAlgorithm());
    }

    /**
     * A FIPS system has no {@code DRBG}. Only the SUN provider offers that name, and a FIPS system takes the random
     * generators out of it, as Rocky Linux 9 does when booted with {@code fips=1}. An unnamed instance then comes from
     * the validated provider itself, so the fallback has to be quiet, not an error.
     */
    @Test
    void testFallsBackWhenDrbgIsMissing() {
        try (MockedStatic<SecureRandom> mocked = mockStatic(SecureRandom.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> SecureRandom.getInstance("DRBG")).thenThrow(new NoSuchAlgorithmException("no DRBG here"));

            byte[] bytes = new byte[16];
            WicketSecureRandomSupplier.createSecureRandom().nextBytes(bytes);

            assertFalse(Arrays.equals(new byte[16], bytes));
        }
    }

    /** The registered provider wins over the {@code DRBG} name: a FIPS one only takes keys from its own generator. */
    @Test
    void testPrefersTheRegisteredCryptoProvider() {
        SecureRandom unnamed = new SecureRandom();
        assumeTrue(Security.getProviders("SecureRandom.DRBG") != null);
        assumeTrue(!"DRBG".equals(unnamed.getAlgorithm()));

        Provider provider = unnamed.getProvider();
        try (MockedStatic<CryptoProviders> mocked = mockStatic(CryptoProviders.class)) {
            mocked.when(CryptoProviders::getProvider).thenReturn(provider);

            assertEquals(
                    unnamed.getAlgorithm(),
                    WicketSecureRandomSupplier.createSecureRandom().getAlgorithm());
        }
    }

    @Test
    void testRandomProducesBytes() {
        byte[] bytes = new byte[16];

        new WicketSecureRandomSupplier().getRandom().nextBytes(bytes);

        assertFalse(Arrays.equals(new byte[16], bytes));
    }
}
