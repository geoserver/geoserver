/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class PreviewHomePageContentProviderTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/org/geoserver/web/demo/PreviewHomePageContentProviderTestContext.xml");
    }

    @Test
    public void testDropdownUsesExternalScript() {
        logout();
        PageParameters params = new PageParameters();
        params.add("layer", getLayerId(MockData.BASIC_POLYGONS));

        tester.startPage(GeoServerHomePage.class, params);
        tester.assertRenderedPage(GeoServerHomePage.class);

        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("preview-home-page-menu-select"));
        assertFalse(html.contains("onchange="));
    }
}
