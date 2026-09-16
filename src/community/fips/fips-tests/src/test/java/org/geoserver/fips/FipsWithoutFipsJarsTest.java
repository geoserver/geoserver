/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

import org.geoserver.platform.security.SecurityDefaults;
import org.junit.Before;
import org.junit.Test;

/**
 * An installation with both sets of BouncyCastle jars on the classpath has to be told why nothing works. They use the
 * same package names, so no encryption runs at all, and the errors that follow do not say why. This test runs in its
 * own surefire execution, the one that keeps the regular jars, see the container pom.
 */
public class FipsWithoutFipsJarsTest {

    /** A build with the fips profile has no regular jars to add, so there is nothing to report. */
    @Before
    public void mixedClasspathOnly() {
        assumeFalse(FipsSetup.isFipsClasspath());
    }

    @Test
    public void testTheBrokenInstallationIsReported() {
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> new FipsCryptoProviderSupplier().getProvider());

        assertThat(e.getMessage(), containsString("regular BouncyCastle jars are on the classpath"));
    }

    /** A data directory created on this classpath must keep GeoServer's own defaults: BCFKS cannot be read here. */
    @Test
    public void testNoDefaultsAreImposed() {
        FipsSecurityDefaults defaults = new FipsSecurityDefaults();
        for (SecurityDefaults.Setting setting : SecurityDefaults.Setting.values()) {
            assertNull(setting.name(), defaults.get(setting));
        }
    }
}
