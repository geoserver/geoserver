/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import java.util.List;
import java.util.logging.Level;
import org.geofence.core.services.RuleReaderService;
import org.geofence.core.services.dto.RuleFilter;
import org.geofence.core.services.dto.ShortRule;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.services.RestRuleReaderService;
import org.geoserver.geofence.services.RuleReaderServiceFactory;

/**
 * Base class for tests needing a real standalone GeoFence REST server. Test methods should guard themselves with
 * {@code Assume.assumeTrue(IS_GEOFENCE_AVAILABLE)} to skip cleanly when none is reachable.
 */
public abstract class GeofenceRestBaseTest extends GeofenceBaseTest {

    protected static Boolean IS_GEOFENCE_AVAILABLE = false;

    protected static RuleReaderService geofenceService;

    /** Matches {@code applicationContext.xml}'s {@code restRuleReaderService} bean's {@code serviceUrl}. */
    private static final String GEOFENCE_REST_URL = "http://localhost:9191/geofence/rest";

    /** Re-seed once per JVM/test run, not once per test method. */
    private static boolean rulesSeeded = false;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        if (isGeoFenceAvailable()) {
            IS_GEOFENCE_AVAILABLE = true;
            System.setProperty("IS_GEOFENCE_AVAILABLE", "True");
            if (!rulesSeeded) {
                new GeofenceRestTestDataSeeder(GEOFENCE_REST_URL).seed();
                rulesSeeded = true;
            }
        } else {
            LOGGER.warning("Skipping test in "
                    + getClass().getSimpleName()
                    + " as GeoFence service is down: "
                    + "in order to run this test you need the services to be running on port 9191");
        }
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        try {
            if (System.getProperty("IS_GEOFENCE_AVAILABLE") != null) {
                System.clearProperty("IS_GEOFENCE_AVAILABLE");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not remove System ENV variable {IS_GEOFENCE_AVAILABLE}", e);
        }
    }

    /** Checks whether a standalone GeoFence REST server is actually reachable at {@link #GEOFENCE_REST_URL} */
    protected boolean isGeoFenceAvailable() {
        geofenceService = applicationContext
                .getBean("ruleReaderBackendFactory", RuleReaderServiceFactory.class)
                .getService();
        try {
            // a successful call (even with zero matches, e.g. before the fixture is seeded) proves the server is
            // reachable - only a thrown exception means it isn't
            RestRuleReaderService probe = new RestRuleReaderService();
            probe.setServiceUrl(GEOFENCE_REST_URL);
            final RuleFilter ruleFilter = new RuleFilter();
            ruleFilter.setService("WMS");
            final List<ShortRule> matchingRules = probe.getMatchingRules(ruleFilter);
            if (matchingRules != null) {
                LOGGER.log(Level.WARNING, "GeoFence is active");
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error connecting to GeoFence: " + e.getMessage());
        }

        LOGGER.log(Level.WARNING, "Not connecting to GeoFence");
        return false;
    }
}
