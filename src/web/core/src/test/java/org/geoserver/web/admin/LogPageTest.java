/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.io.File;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * Tests default logging functionality, requires use of {@link #cleanupLogs()} after test is
 * completed.
 */
public class LogPageTest extends GeoServerWicketTestSupport {

    protected static File logsDir = null;

    @AfterClass
    public static void cleanupLogs() {
        if (logsDir != null) {
            Files.delete(logsDir);
            logsDir = null;
        }
    }

    @After
    public void after() {
        Resource logs = getDataDirectory().get("logs");
        if (logs.getType() == Resource.Type.DIRECTORY) {
            logsDir = logs.dir();
        }
    }

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
