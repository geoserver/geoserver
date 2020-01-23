/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.urlchecker.GeoserverURLChecker;
import org.geoserver.security.urlchecker.URLEntry;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.ows.URLCheckers;
import org.junit.Before;
import org.junit.Test;
import org.vfny.geoserver.util.Requests;

public class GeoserverURLCheckerTests extends GeoServerSystemTestSupport {

    @Before
    public void setUp() throws Exception {
        // assert bean exists
        //  assertNotNull(GeoserverURLConfigService.getSingleton());
        // verify SPI Factory has the bean registered
        assertFalse(URLCheckers.getURLCheckerList().isEmpty());
    }

    @Test
    public void testBasicReadWrite() throws Exception {

        GeoserverURLChecker checker = super.geoserverURLConfigServiceBean.reload();
        assertNotNull(checker);
        // modify
        checker.setEnabled(true);
        checker = super.geoserverURLConfigServiceBean.save();
        assertTrue(checker.isEnabled());
        assertTrue(URLCheckers.getURLCheckerList().size() == 1);
    }

    @Test
    public void testEvaluation() throws Exception {
        enableGeoserverURLConfigService(true);
        // Geoserver URLChecker implementation works with REST calls only
        Dispatcher.REQUEST.set(new Request());
        URLEntry googleOnly =
                new URLEntry(
                        "google only",
                        "only allow url starting with http://www.google.com",
                        "^(http://www.google).*$");
        addURLEntryGeoserverURLConfigService(googleOnly);
        // GeoserverURLConfigService.getSingleton().addAndsave(googleOnly);
        // disable default entry

        GeoserverURLChecker checker = super.geoserverURLConfigServiceBean.reload();
        checker.get("google only").setEnable(true);

        // evaluate though Factory methods
        assertTrue(URLCheckers.evaluate("http://www.google.com/some/service"));
        try {
            assertFalse(URLCheckers.evaluate("http://www.yahoo.com/some/service"));
            fail();
        } catch (Exception e) {
            // assert the exception is coming from GeoserverURLConfigService
            assertTrue(e.getMessage().contains("Evaluation Failure"));
        }
    }

    @Test
    public void testRequestsUtilEvaluation() throws Exception {
        // tests static methods in  org.vfny.geoserver.util.Requests
        enableGeoserverURLConfigService(true);
        // Geoserver URLChecker implementation works with REST calls only
        Dispatcher.REQUEST.set(new Request());

        enableGeoserverURLConfigService(true);
        // Geoserver URLChecker implementation works with REST calls only
        Dispatcher.REQUEST.set(new Request());
        URLEntry googleOnly =
                new URLEntry(
                        "google only",
                        "only allow url starting with http://www.google.com",
                        "^(http://www.google).*$");
        // GeoserverURLConfigService.getSingleton().addAndsave(googleOnly);
        addURLEntryGeoserverURLConfigService(googleOnly);

        try {
            Requests.getInputStream(new URL("http://www.yahoo.com/some/service"));
            fail();
        } catch (Exception e) {
            // assert the exception is coming from GeoserverURLConfigService
            assertTrue(e.getMessage().contains("Evaluation Failure"));
        }
    }
}
