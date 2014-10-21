/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
    
    @Test
    public void testReload() throws Exception {
        // the status page was rendered as expected
        tester.assertRenderedPage(StatusPage.class);
        
        // now force a config reload
        getGeoServer().reload();
        
        // force the page reload
        login();
        tester.startPage(StatusPage.class);
        
        // check we did not NPE
        tester.assertRenderedPage(StatusPage.class);
    }
}
