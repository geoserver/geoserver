/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.*;

import org.springframework.security.web.savedrequest.SavedRequest;
import org.geoserver.web.data.layer.LayerPage;
import org.junit.Test;

public class GeoServerSecuredPageTest extends GeoServerWicketTestSupport {
    @Test
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

    @Test
    public void testSecuredPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);
    }
}
