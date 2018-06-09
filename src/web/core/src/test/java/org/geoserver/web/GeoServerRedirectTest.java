/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInfo.WebUIMode;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.web.admin.StatusPage;
import org.junit.Before;
import org.junit.Test;

/** @author Niels Charlier */
public class GeoServerRedirectTest extends GeoServerSecurityTestSupport {

    public GeoServerApplication getGeoServerApplication() {
        return (GeoServerApplication) applicationContext.getBean("webApplication");
    }

    @Before
    public void init() {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testRedirect() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setWebUIMode(WebUIMode.REDIRECT);
        getGeoServer().save(global);
        GeoServerApplication app = getGeoServerApplication();
        app.init();
        WicketTester tester = new WicketTester(app, false);

        tester.startPage(StatusPage.class);

        assertEquals(2, tester.getPreviousResponses().size());
        assertEquals(302, tester.getPreviousResponses().get(0).getStatus());
    }

    @Test
    public void testDoNotRedirect() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setWebUIMode(WebUIMode.DO_NOT_REDIRECT);
        getGeoServer().save(global);
        GeoServerApplication app = getGeoServerApplication();
        app.init();
        WicketTester tester = new WicketTester(app, false);

        tester.startPage(StatusPage.class);

        assertEquals(1, tester.getPreviousResponses().size());
        assertEquals(200, tester.getPreviousResponses().get(0).getStatus());
    }

    @Test
    public void testDefaultRedirect() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setWebUIMode(WebUIMode.DEFAULT);
        getGeoServer().save(global);
        GeoServerApplication app = getGeoServerApplication();
        app.setDefaultIsRedirect(true);
        app.init();
        WicketTester tester = new WicketTester(app, false);

        tester.startPage(StatusPage.class);

        assertEquals(2, tester.getPreviousResponses().size());
        assertEquals(302, tester.getPreviousResponses().get(0).getStatus());
    }

    @Test
    public void testDefaultDoNotRedirect() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setWebUIMode(WebUIMode.DEFAULT);
        getGeoServer().save(global);
        GeoServerApplication app = getGeoServerApplication();
        app.setDefaultIsRedirect(false);
        app.init();
        WicketTester tester = new WicketTester(app, false);

        tester.startPage(StatusPage.class);

        assertEquals(1, tester.getPreviousResponses().size());
        assertEquals(200, tester.getPreviousResponses().get(0).getStatus());
    }
}
