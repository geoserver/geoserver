/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoserver.authentication.filter;

import java.util.logging.Logger;
import org.geoserver.geofence.GeofenceBaseTest;
import org.geoserver.geoserver.authentication.auth.GeoFenceSecurityProvider;
import org.geotools.util.logging.Logging;
import org.junit.Assume;
import org.junit.Test;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *     <p>Validates {@link GeoFenceAuthFilterConfig} objects.
 */
public class GeofenceFilterConfigTest extends GeofenceBaseTest {

    protected static Logger LOGGER =
            Logging.getLogger("org.geoserver.geoserver.authentication.auth");

    private String geofenceUrl = "http://localhost:9191/geofence";

    private String geoserverName = "geoserver";

    @Test
    public void testGeofenceFilterConfigValidation() throws Exception {
        GeoFenceAuthFilterConfig config = new GeoFenceAuthFilterConfig();
        config.setClassName(GeoFenceAuthFilter.class.getName());
        config.setName("testGeoFence");

        check(config);
    }

    public void check(GeoFenceAuthFilterConfig config) throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        config.setGeofenceUrl(geofenceUrl);
        config.setGeoserverName(geoserverName);

        GeoFenceAuthFilter filter = new GeoFenceAuthFilter();
        filter.setSecurityManager(getSecurityManager());
        GeoFenceSecurityProvider geofenceAuth = new GeoFenceSecurityProvider();
        geofenceAuth.setSecurityManager(getSecurityManager());
        geofenceAuth.setRuleReaderService(geofenceService);
        filter.setGeofenceAuth(geofenceAuth);

        filter.initializeFromConfig(config);
    }
}
