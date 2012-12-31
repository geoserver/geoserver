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
    
    public void testReload() throws Exception {
        // the status page was rendered as expected
        tester.assertRenderedPage(StatusPage.class);
        
        // now force a config reload
        getGeoServer().reload();
        
        // force the page reload
        tester.startPage(StatusPage.class);
        
        // check we did not NPE
        tester.assertRenderedPage(StatusPage.class);
    }
}
