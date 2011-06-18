package org.geoserver.web.demo;

import org.geoserver.web.GeoServerWicketTestSupport;

public class MapPreviewPageTest extends GeoServerWicketTestSupport {
    public void testValues() throws Exception {
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);
    }
}
