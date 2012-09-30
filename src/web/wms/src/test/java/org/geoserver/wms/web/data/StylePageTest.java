package org.geoserver.wms.web.data;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class StylePageTest extends GeoServerWicketTestSupport {
    
    @Test
    public void testPageLoad() {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
    }
}
