package org.geoserver.wms.web.data;

import org.geoserver.web.GeoServerWicketTestSupport;

public class StylePageTest extends GeoServerWicketTestSupport {
    
    public void testPageLoad() {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
    }
}
