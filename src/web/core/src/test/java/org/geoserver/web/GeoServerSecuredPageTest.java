/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.geoserver.web.data.layer.LayerPage;
import org.junit.Test;
import org.springframework.security.web.savedrequest.SavedRequest;

public class GeoServerSecuredPageTest extends GeoServerWicketTestSupport {
    
    @Test
    public void testSecuredPageGivesRedirectWhenLoggedOut() throws UnsupportedEncodingException {
        logout();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);
        // make sure the spring security emulation is properly setup
        SavedRequest sr = (SavedRequest) tester.getHttpSession().getAttribute(GeoServerSecuredPage.SAVED_REQUEST);
        assertNotNull(sr);
        String redirectUrl = new URLDecoder().decode(sr.getRedirectUrl(), "UTF8");
        assertTrue(redirectUrl.contains("wicket/bookmarkable/org.geoserver.web.data.layer.LayerPage"));
    }

    @Test
    public void testSecuredPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);
    }
}
