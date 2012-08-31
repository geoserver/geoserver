package org.geoserver.web;

import org.springframework.security.web.savedrequest.SavedRequest;
import org.geoserver.web.data.layer.LayerPage;

public class GeoServerSecuredPageTest extends GeoServerWicketTestSupport {
    public void testSecuredPageGivesRedirectWhenLoggedOut() {
        logout();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);
        // make sure the spring security emulation is properly setup
        SavedRequest sr = (SavedRequest) tester.getSession().getAttribute(GeoServerSecuredPage.SAVED_REQUEST);
        assertNotNull(sr);
        
        // XXX: Do we need to go back to the old query-string-based bookmarkable pages?
        // assertTrue(sr.getRedirectUrl(), sr.getRedirectUrl().endsWith("?wicket:bookmarkablePage=:org.geoserver.web.data.layer.LayerPage"));
        assertTrue(sr.getRedirectUrl(), sr.getRedirectUrl().endsWith("/servlet/wicket/bookmarkable/org.geoserver.web.data.layer.LayerPage"));
    }

    public void testSecuredPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);
    }
}
