package org.geoserver.web;

import org.springframework.security.web.savedrequest.SavedRequest;
import org.geoserver.web.data.layer.LayerPage;

public class GeoServerSecuredPageTest extends GeoServerWicketTestSupport {
    public void testSecuredPageGivesRedirectWhenLoggedOut() {
        logout();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);
        // make sure the spring security emulation is properly setup
        SavedRequest sr = (SavedRequest) tester.getServletSession().getAttribute(GeoServerSecuredPage.SAVED_REQUEST);
        assertNotNull(sr);
        assertTrue(sr.getRedirectUrl().endsWith("?wicket:bookmarkablePage=:org.geoserver.web.data.layer.LayerPage"));
                        
    }

    public void testSecuredPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);
    }
}
