/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.util.tester.TagTester;
import org.junit.Test;

public class GeoServerAboutPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testLoginFormAction() throws Exception {
        logout();
        tester.executeUrl("./wicket/bookmarkable/org.geoserver.web.AboutGeoServerPage");
        assertThat(tester.getLastRenderedPage(), instanceOf(AboutGeoServerPage.class));

        String responseTxt = tester.getLastResponse().getDocument();
        // System.out.println(responseTxt);
        TagTester tagTester = TagTester.createTagByName(responseTxt, "form");
        assertEquals("http://localhost/context/j_spring_security_check", tagTester.getAttribute("action"));
    }

    /**
     * The About page should hide the sensitive information (like version info, etc...). This test: gets the page as a
     * non-admin -> version info should NOT be there gets the page as ADMIN -> version info SHOULD be there
     */
    @Test
    public void testHideSensitiveInfo() throws Exception {
        logout();
        tester.executeUrl("./wicket/bookmarkable/org.geoserver.web.AboutGeoServerPage");

        String responseTxt = tester.getLastResponse().getDocument();
        assertFalse(responseTxt.contains("geotoolsInfo"));

        login();
        tester.executeUrl("./wicket/bookmarkable/org.geoserver.web.AboutGeoServerPage");

        responseTxt = tester.getLastResponse().getDocument();
        assertTrue(responseTxt.contains("geotoolsInfo"));
    }
}
