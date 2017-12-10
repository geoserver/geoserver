/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class LogPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testDefaultLocation() {
        GeoServer gs = getGeoServerApplication().getGeoServer();
        LoggingInfo logging = gs.getLogging();
        logging.setLocation("logs/geoserver.log");
        gs.save(logging);

        login();
        tester.startPage(LogPage.class);
        tester.assertRenderedPage(LogPage.class);
    }

    @Test
    public void testNullLocation() {
        GeoServer gs = getGeoServerApplication().getGeoServer();
        LoggingInfo logging = gs.getLogging();
        logging.setLocation(null);
        gs.save(logging);

        login();
        tester.startPage(LogPage.class);
        tester.assertRenderedPage(LogPage.class);
    }
}
