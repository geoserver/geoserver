/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import java.util.logging.Level;
import org.geofence.core.services.dto.RuleFilter;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.cache.CacheManager;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.services.RestRuleReaderService;

/**
 * Base class for tests needing a real standalone GeoFence REST server, named by the {@code geofence.test.client.url}
 * system property. Without it these tests skip; with it they seed a fixture, which <b>deletes every rule</b> on that
 * server, so point it at a dedicated instance. Test methods guard themselves with
 * {@code Assume.assumeTrue(IS_GEOFENCE_AVAILABLE)}.
 */
public abstract class GeofenceRestBaseTest extends GeofenceBaseTest {

    protected static Boolean IS_GEOFENCE_AVAILABLE = false;

    /** Names the GeoFence to test against; unset means skip, rather than hunting for one on a well-known port. */
    private static final String URL_PROPERTY = "geofence.test.client.url";

    private static final String GEOFENCE_REST_URL = System.getProperty(URL_PROPERTY);

    /** Re-seed once per JVM/test run, not once per test method. */
    private static boolean rulesSeeded = false;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        if (GEOFENCE_REST_URL == null) {
            LOGGER.warning("Skipping tests in " + getClass().getSimpleName() + ": set -D" + URL_PROPERTY
                    + "=<url> to run them against a dedicated GeoFence server (its rules get wiped)");
            return;
        }

        // Point the real reader - the one the access manager gets from the factories - at the server we are about
        // to seed, the same way a config save does. Setting it on the bean alone would not survive, since
        // RestRuleReaderService re-seeds its URL from the configuration in @PostConstruct.
        applicationContext
                .getBean(GeoFenceConfigurationManager.class)
                .getConfiguration()
                .setServicesUrl(GEOFENCE_REST_URL);
        applicationContext.getBean(RestRuleReaderService.class).setServiceUrl(GEOFENCE_REST_URL);
        // context startup already queried whatever the default URL pointed at; drop anything it cached
        applicationContext.getBean(CacheManager.class).invalidateAll();
        requireReachable();

        IS_GEOFENCE_AVAILABLE = true;
        System.setProperty("IS_GEOFENCE_AVAILABLE", "True");
        if (!rulesSeeded) {
            new GeofenceRestTestDataSeeder(GEOFENCE_REST_URL).seed();
            rulesSeeded = true;
        }
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        // a no-op on the current base class, but this way a teardown added there later won't be silently skipped
        super.onTearDown(testData);
        // static, so the next class would otherwise inherit this one's verdict instead of probing for itself
        IS_GEOFENCE_AVAILABLE = false;
        try {
            if (System.getProperty("IS_GEOFENCE_AVAILABLE") != null) {
                System.clearProperty("IS_GEOFENCE_AVAILABLE");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not remove System ENV variable {IS_GEOFENCE_AVAILABLE}", e);
        }
    }

    /** Fails rather than skips: naming a server is a claim that it is there, and a silent skip hides the mistake. */
    private void requireReachable() {
        try {
            RestRuleReaderService probe = new RestRuleReaderService();
            probe.setServiceUrl(GEOFENCE_REST_URL);
            RuleFilter ruleFilter = new RuleFilter();
            ruleFilter.setService("WMS");
            // a successful call proves reachability, even with zero matches (i.e. before the fixture is seeded)
            probe.getMatchingRules(ruleFilter);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No GeoFence server reachable at " + GEOFENCE_REST_URL + ", set with " + URL_PROPERTY, e);
        }
    }
}
