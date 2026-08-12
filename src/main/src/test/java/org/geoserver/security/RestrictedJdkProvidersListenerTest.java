/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.junit.Test;

/**
 * The restriction reaches a test with no GeoServer base class, which is why it is registered as a launcher listener. It
 * runs only under the fips-test build profile; anywhere else there is nothing to check.
 */
public class RestrictedJdkProvidersListenerTest {

    @Test
    public void testRestrictionApplied() {
        assumeTrue(Boolean.getBoolean("geoserver.fips.test"));

        assertThrows(KeyStoreException.class, () -> KeyStore.getInstance("JCEKS"));
        assertThrows(NoSuchAlgorithmException.class, () -> SecureRandom.getInstance("SHA1PRNG"));
    }
}
