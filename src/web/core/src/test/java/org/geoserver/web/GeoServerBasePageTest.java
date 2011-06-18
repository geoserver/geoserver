package org.geoserver.web;

public class GeoServerBasePageTest extends GeoServerWicketTestSupport {
    public void testLoginFormShowsWhenLoggedOut() throws Exception {
        logout();
        tester.startPage(GeoServerHomePage.class);
        tester.assertVisible("loginform");
        tester.assertInvisible("logoutform");
    }

    public void testLogoutFormShowsWhenLoggedIn() throws Exception {
        login();
        tester.startPage(GeoServerHomePage.class);
    }
}
