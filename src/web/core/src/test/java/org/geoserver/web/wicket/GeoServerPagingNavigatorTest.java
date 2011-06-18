package org.geoserver.web.wicket;

import org.geoserver.web.GeoServerWicketTestSupport;

public class GeoServerPagingNavigatorTest extends GeoServerWicketTestSupport {
    public void testPageLoads() throws Exception {
        tester.startPage(GeoServerPagingNavigatorTestPage.class);
    }
}
