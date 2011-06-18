package org.geoserver.web.admin;

import org.geoserver.web.GeoServerWicketTestSupport;

public class CoverageAccessPageTest extends GeoServerWicketTestSupport {

    public void testLoad() throws Exception {
        login();
        tester.startPage(CoverageAccessPage.class);
        tester.assertRenderedPage(CoverageAccessPage.class);
    }
}
