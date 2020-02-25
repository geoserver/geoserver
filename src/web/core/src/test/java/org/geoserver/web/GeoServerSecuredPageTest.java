/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.protocol.http.WebSession;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.data.layer.LayerPage;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.savedrequest.SavedRequest;

public class GeoServerSecuredPageTest extends GeoServerWicketTestSupport {

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Test
    public void testSecuredPageGivesRedirectWhenLoggedOut() throws UnsupportedEncodingException {
        logout();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);
        // make sure the spring security emulation is properly setup
        SavedRequest sr =
                (SavedRequest)
                        tester.getHttpSession().getAttribute(GeoServerSecuredPage.SAVED_REQUEST);
        assertNotNull(sr);
        String redirectUrl = new URLDecoder().decode(sr.getRedirectUrl(), "UTF8");
        assertTrue(
                redirectUrl.contains("wicket/bookmarkable/org.geoserver.web.data.layer.LayerPage"));
    }

    @Test
    public void testSecuredPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);
    }

    @Test
    public void testToolPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(ToolPage.class);
        tester.assertRenderedPage(ToolPage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testSessionFixationAvoidance() throws Exception {
        tester.startPage(GeoServerHomePage.class);
        final WebSession session = WebSession.get();
        session.bind(); // fore session creation
        session.setAttribute("test", "whatever");
        // login, this will invalidate the session
        tester.startPage(GeoServerHomePage.class);
        MockHttpServletRequest request = createRequest("login");
        request.setMethod("POST");
        request.setParameter("username", "admin");
        request.setParameter("password", "geoserver");
        String oldSessionId = request.getSession().getId();
        dispatch(request);
        // verify that the session ID changed
        assertNotEquals(oldSessionId, request.getSession().getId());
        // the session in wicket tester mock does not disappear, the only
        // way to see if it has been invalidated is to check that the attributes are gone...
        assertNull(session.getAttribute("test"));
    }
}
