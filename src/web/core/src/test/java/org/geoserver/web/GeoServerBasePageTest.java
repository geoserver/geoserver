/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.Component;
import org.apache.wicket.core.util.string.ComponentRenderer;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.TagTester;
import org.junit.Test;

public class GeoServerBasePageTest extends GeoServerWicketTestSupport {
    @Test
    public void testLoginFormShowsWhenLoggedOut() throws Exception {
        logout();
        tester.startPage(GeoServerHomePage.class);
        tester.assertVisible("loginforms");
        tester.assertVisible("loginforms:0:loginform");
        tester.assertInvisible("logoutform");
        ListView loginForms = (ListView) tester.getLastRenderedPage().get("loginforms");
        String responseTxt = ComponentRenderer.renderComponent(loginForms).toString();
        TagTester tagTester = TagTester.createTagByName(responseTxt, "form");
        assertEquals("http://localhost/context/j_spring_security_check", tagTester.getAttribute("action"));
    }

    @Test
    public void testLogoutFormShowsWhenLoggedIn() throws Exception {
        login();
        tester.startPage(GeoServerHomePage.class);
        tester.assertVisible("loginforms");
        tester.assertInvisible("loginforms:0:loginform");
        tester.assertVisible("logoutform");
        ListView loginForms = (ListView) tester.getLastRenderedPage().get("loginforms");
        assertEquals(1, loginForms.getList().size());
        Component logoutforms = tester.getLastRenderedPage().get("logoutform");
        String responseTxt = ComponentRenderer.renderComponent(logoutforms).toString();
        TagTester tagTester = TagTester.createTagByName(responseTxt, "a");
        assertEquals("http://localhost/context/j_spring_security_logout", tagTester.getAttribute("href"));
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
        System.setProperty(DefaultGeoServerNodeInfo.GEOSERVER_NODE_OPTS, "id:test;background:red;color:black");
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
