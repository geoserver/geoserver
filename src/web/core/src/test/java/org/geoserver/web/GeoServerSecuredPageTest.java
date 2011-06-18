package org.geoserver.web;

import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.security.ui.savedrequest.SavedRequest;
import org.geoserver.web.data.layer.LayerPage;

public class GeoServerSecuredPageTest extends GeoServerWicketTestSupport {
    public void testSecuredPageGivesRedirectWhenLoggedOut() {
        logout();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);
        // make sure the spring security emulation is properly setup
        SavedRequest sr = (SavedRequest) tester.getServletSession().getAttribute(AbstractProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY);
        assertNotNull(sr);
        assertTrue(sr.getFullRequestUrl().endsWith("?wicket:bookmarkablePage=:org.geoserver.web.data.layer.LayerPage"));
    }

    public void testSecuredPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);
    }
}
