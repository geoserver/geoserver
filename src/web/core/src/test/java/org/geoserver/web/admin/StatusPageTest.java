package org.geoserver.web.admin;

import org.geoserver.web.GeoServerWicketTestSupport;

public class StatusPageTest extends GeoServerWicketTestSupport {
    
    @Override
    protected void setUpInternal() throws Exception {
        login();
        tester.startPage(StatusPage.class);
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testValues() {
        tester.assertRenderedPage(StatusPage.class);
        tester.assertLabel("locks", "0");
    }
}
