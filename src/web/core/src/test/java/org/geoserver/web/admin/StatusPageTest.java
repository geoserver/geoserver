package org.geoserver.web.admin;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class StatusPageTest extends GeoServerWicketTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        login();
        tester.startPage(StatusPage.class);
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    @Test
    public void testValues() {
        tester.assertRenderedPage(StatusPage.class);
        tester.assertLabel("locks", "0");
    }
}
