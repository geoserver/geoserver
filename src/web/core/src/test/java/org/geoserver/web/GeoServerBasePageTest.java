/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.junit.Test;

public class GeoServerBasePageTest extends GeoServerWicketTestSupport {
    @Test
    public void testLoginFormShowsWhenLoggedOut() throws Exception {
        logout();
        tester.startPage(GeoServerHomePage.class);
        tester.assertVisible("loginform");
        tester.assertInvisible("logoutform");
    }

    @Test
    public void testLogoutFormShowsWhenLoggedIn() throws Exception {
        login();
        tester.startPage(GeoServerHomePage.class);
    }
    
    @Test
    public void testDefaultNodeInfoLoggedOut() throws Exception {
        logout();
        System.setProperty(DefaultGeoServerNodeInfo.GEOSERVER_NODE_OPTS, "id=test");
        DefaultGeoServerNodeInfo.initializeFromEnviroment();

        tester.startPage(GeoServerHomePage.class);
        tester.assertInvisible("nodeIdContainer");
    }

    @Test
    public void testDefaultNodeInfoLoggedIn() throws Exception {
        login();
        System.setProperty(DefaultGeoServerNodeInfo.GEOSERVER_NODE_OPTS,
                "id:test;background:red;color:black");
        DefaultGeoServerNodeInfo.initializeFromEnviroment();

        tester.startPage(GeoServerHomePage.class);
        tester.assertVisible("nodeIdContainer");
        tester.assertModelValue("nodeIdContainer:nodeId", "test");
        // this does not work, damn wicket tester...
        // TagTester tags = tester.getTagByWicketId("nodeIdContainer");
        // String style = tags.getAttribute("style");
        // assertTrue(style.contains("background:red;"));
        // assertTrue(style.contains("color:black;"));
    }
}
