/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import javax.crypto.Cipher;
import org.geoserver.security.CryptoProviders;
import org.junit.Before;
import org.junit.Test;

/** Checks that putting this module on the classpath is all it takes to move GeoServer to the FIPS provider. */
public class FipsCryptoProviderTest {

    private static final String BC_FIPS = "BCFIPS";

    /** Without the FIPS jars there is no FIPS provider to look at. That is an ordinary build, not a failure. */
    @Before
    public void fipsClasspathOnly() {
        assumeTrue(FipsSetup.isFipsClasspath());
    }

    @Test
    public void testSuppliedProviderIsUsed() {
        assertEquals(BC_FIPS, CryptoProviders.getProvider().getName());
    }

    @Test
    public void testProviderIsPreferred() {
        CryptoProviders.getProvider();
        assertEquals(BC_FIPS, Security.getProviders()[0].getName());
    }

    @Test
    public void testApprovedAlgorithmsResolveToFipsProvider() throws Exception {
        CryptoProviders.getProvider();
        assertEquals(BC_FIPS, MessageDigest.getInstance("SHA-256").getProvider().getName());
        assertEquals(
                BC_FIPS,
                Cipher.getInstance("AES/CBC/PKCS5Padding").getProvider().getName());
    }

    /**
     * Approved-only mode decides what this provider will do, not what the whole JVM offers. The JDK providers stay
     * registered behind it, and on a FIPS operating system they still answer for MD5 and DES. What changes is that
     * nothing going through the FIPS provider can use them.
     */
    @Test
    public void testNonApprovedAlgorithmsNotServedByFipsProvider() {
        CryptoProviders.getProvider();
        assertThrows(NoSuchAlgorithmException.class, () -> MessageDigest.getInstance("MD5", BC_FIPS));
        assertThrows(NoSuchAlgorithmException.class, () -> Cipher.getInstance("DES/ECB/PKCS5Padding", BC_FIPS));
    }
}
