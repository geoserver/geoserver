/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;

/**
 * Boots a whole GeoServer on a data directory created from scratch with this module installed, which is what a fresh
 * FIPS installation does. Failing here means a FIPS deployment cannot start at all.
 */
public class FipsSecurityBootstrapTest extends GeoServerSystemTestSupport {

    /** Without the FIPS jars the module leaves the defaults alone, which the inert test covers instead. */
    @Before
    public void fipsClasspathOnly() {
        assumeTrue(FipsSetup.isFipsClasspath());
    }

    @Test
    public void testSecurityConfigUsesTheFipsCapableEncoder() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();

        String configEncoder = manager.getSecurityConfig().getConfigPasswordEncrypterName();
        assertEquals(FipsSecurityDefaults.AES_GCM_ENCODER, configEncoder);

        GeoServerPasswordEncoder encoder = manager.loadPasswordEncoder(configEncoder);
        assertNotNull(encoder);
        assertEquals("crypt3", encoder.getPrefix());
    }

    @Test
    public void testCatalogPasswordsRoundTripThroughTheConfiguredEncoder() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        String encoded = manager.getConfigPasswordEncryptionHelper().encode("s3cret");

        assertTrue(encoded.startsWith("crypt3:"));
        assertEquals("s3cret", manager.getConfigPasswordEncryptionHelper().decode(encoded));
    }
}
