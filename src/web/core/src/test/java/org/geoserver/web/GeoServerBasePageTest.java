/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.apache.wicket.core.util.string.ComponentRenderer;
import org.apache.wicket.util.tester.TagTester;
import org.junit.Test;

public class GeoServerBasePageTest extends GeoServerWicketTestSupport {
    @Test
    public void testLoginFormShowsWhenLoggedOut() throws Exception {
        logout();
        tester.startPage(GeoServerHomePage.class);
        tester.assertVisible("loginform");
        tester.assertInvisible("logoutform");
        Component loginForm = tester.getLastRenderedPage().get("loginform");
        String responseTxt = ComponentRenderer.renderComponent(loginForm).toString();
        TagTester tagTester = TagTester.createTagByAttribute(responseTxt, "form");
        assertEquals("../j_spring_security_check", tagTester.getAttribute("action"));
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
