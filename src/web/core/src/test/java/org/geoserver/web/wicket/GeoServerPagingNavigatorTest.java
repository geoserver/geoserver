package org.geoserver.web.wicket;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class GeoServerPagingNavigatorTest extends GeoServerWicketTestSupport {
    @Test
    public void testPageLoads() throws Exception {
        tester.startPage(GeoServerPagingNavigatorTestPage.class);
    }
}
