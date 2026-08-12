/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * Checks that the module shows up at /rest/about/status. An admin looks there, not at /rest/about/manifest, which lists
 * the jar whether the module registers itself or not.
 */
public class FipsModuleStatusTest extends GeoServerSystemTestSupport {

    @Test
    public void testModuleIsReportedAmongTheModuleStatuses() {
        List<String> modules = GeoServerExtensions.extensions(ModuleStatus.class).stream()
                .map(ModuleStatus::getModule)
                .toList();
        assertThat(modules, hasItem("gs-fips-provider"));
    }

    @Test
    public void testModuleStatusFields() {
        assumeTrue(FipsSetup.isFipsClasspath());
        ModuleStatus status = fipsStatus();

        assertEquals("GeoServer FIPS 140-3 support", status.getName());
        assertEquals(Optional.of("crypto-provider"), status.getComponent());
        assertEquals(ModuleStatus.Category.COMMUNITY, status.getCategory());
        assertTrue("the FIPS self tests must pass in a FIPS build", status.isAvailable());
    }

    @Test
    public void testMessageReportsTheCryptoSetup() {
        assumeTrue(FipsSetup.isFipsClasspath());
        String message = fipsStatus().getMessage().orElseThrow();

        assertThat(message, containsString("Crypto module: READY"));
        assertThat(message, containsString("Approved-only mode: "));
        assertThat(message, containsString("Operating system FIPS mode: "));
        assertThat(message, containsString("Keystore format: BCFKS"));
        assertThat(message, containsString("Config password encoder: AES-GCM"));
        assertThat(message, containsString("KeyStore BCFKS: yes"));
    }

    /** The keystore type is the one setting that stops a deployment dead when the provider does not offer it. */
    @Test
    public void testRequiredAlgorithmsCoverTheConfiguredKeystoreType() {
        assumeTrue(FipsSetup.isFipsClasspath());
        FipsSetup.AlgorithmStatus keyStore = FipsSetup.getRequiredAlgorithms().stream()
                .filter(algorithm -> "KeyStore".equals(algorithm.type()))
                .findFirst()
                .orElseThrow();

        assertEquals(FipsSecurityDefaults.BCFKS_KEYSTORE, keyStore.algorithm());
        assertTrue("BCFKS must be available with the FIPS provider registered", keyStore.available());
    }

    private static ModuleStatus fipsStatus() {
        return GeoServerExtensions.extensions(ModuleStatus.class).stream()
                .filter(status -> "gs-fips-provider".equals(status.getModule()))
                .findFirst()
                .orElseThrow();
    }
}
