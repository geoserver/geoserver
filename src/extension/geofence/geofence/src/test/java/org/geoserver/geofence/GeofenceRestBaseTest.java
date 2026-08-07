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
 * Base class for tests that need a real, standalone GeoFence REST server (as opposed to the embedded engine) - the ones
 * asserting end-to-end WMS/WFS access-control behavior against a rule fixture only the REST API can seed
 * ({@link GeofenceAccessManagerTest}, {@link ServicesTest}, {@link GeofenceAccessManager_WMTSLayerTest},
 * {@link CacheReaderTest}, and by extension {@code geofence-server}'s {@code InternalServicesTest}, which extends
 * {@link ServicesTest} for its shared fixtures/fields).
 *
 * <p>In order to run these tests live, start {@code geofence-web-app} (geofence_39's {@code web/app} module) via
 * {@code jetty:run} on port 9191 first - {@link GeofenceRestTestDataSeeder} takes care of (re)creating the rule fixture
 * these tests expect, once per JVM/test run, the first time {@link #isGeoFenceAvailable()} succeeds. Test methods
 * should guard themselves with {@code Assume.assumeTrue(IS_GEOFENCE_AVAILABLE)} so they skip cleanly (rather than fail)
 * when no such server is reachable, e.g. in CI.
 */
public abstract class GeofenceRestBaseTest extends GeofenceBaseTest {

    protected static Boolean IS_GEOFENCE_AVAILABLE = false;

    protected static RuleReaderService geofenceService;

    /** Matches {@code applicationContext.xml}'s {@code restRuleReaderService} bean's {@code serviceUrl}. */
    private static final String GEOFENCE_REST_URL = "http://localhost:9191/geofence/rest";

    /**
     * Guards {@link GeofenceRestTestDataSeeder} so it only re-seeds once per JVM/test run, not once per test method -
     * {@code onSetUp} runs before every {@code @Test}, but the rule fixture only needs (re)creating once for the whole
     * run against a given live server.
     */
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

    /**
     * Checks whether a standalone GeoFence REST server is actually reachable at {@link #GEOFENCE_REST_URL} - this
     * probes a throwaway {@link RestRuleReaderService} directly, rather than asking {@code ruleReaderBackendFactory}
     * for whichever {@code RuleReaderService} bean currently happens to be active. The two can disagree: in a
     * module/context where the embedded engine (see {@code geofence-server}) is also on the classpath, the active
     * backend may well be that embedded engine, which "works" trivially (no network involved) even with nothing
     * listening on port 9191 - checking the active bean would then wrongly report "available" right before the
     * REST-only {@link GeofenceRestTestDataSeeder} unconditionally tries to seed against that same unreachable port.
     *
     * <p>{@link #geofenceService} is still set to the actual active backend bean (not this probe) - some subclasses
     * (e.g. {@code InternalServicesTest}) use it to exercise whichever backend is really active, embedded or not.
     */
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
            LOGGER.log(Level.WARNING, "Error connecting to GeoFence", e);
        }

        LOGGER.log(Level.WARNING, "Not connecting to GeoFence");
        return false;
    }
}
