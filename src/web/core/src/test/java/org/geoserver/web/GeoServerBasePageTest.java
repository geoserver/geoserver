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
}
