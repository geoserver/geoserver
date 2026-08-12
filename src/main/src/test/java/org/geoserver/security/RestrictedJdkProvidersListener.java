/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Hides the algorithms a FIPS system lacks before any test runs, when the fips-test build profile asks for it.
 *
 * <p>It is registered as a service, so it covers every test in a module that has the gs-main test jar, whatever base
 * class the test uses. Covering only the tests below {@link org.geoserver.test.GeoServerBaseTestSupport} once let an
 * importer-web failure pass unseen on an ordinary machine.
 *
 * <p>It does not reach gs-wcs2_0, the one module that asks surefire for the JUnit 4 provider. A listener like this one
 * needs a JUnit Platform session, and that provider starts none. The static block in
 * {@link org.geoserver.test.GeoServerBaseTestSupport} covers that module. Having both is safe, because
 * {@link RestrictedJdkProviders#apply()} leaves a provider it has already wrapped alone.
 *
 * @see RestrictedJdkProviders
 */
public class RestrictedJdkProvidersListener implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        if (Boolean.getBoolean("geoserver.fips.test")) RestrictedJdkProviders.apply();
    }
}
