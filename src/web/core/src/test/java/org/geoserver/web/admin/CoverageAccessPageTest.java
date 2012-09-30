package org.geoserver.web.admin;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class CoverageAccessPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testLoad() throws Exception {
        login();
        tester.startPage(CoverageAccessPage.class);
        tester.assertRenderedPage(CoverageAccessPage.class);
    }
}
