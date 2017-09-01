/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class StatusPageTest extends GeoServerWicketTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    @Before
    public void setupTests() {
        login();
        tester.startPage(StatusPage.class);
    }
    
    @Test
    public void testValues() {
        tester.assertRenderedPage(StatusPage.class);
        tester.assertLabel("tabs:panel:locks", "0");
        tester.assertLabel("tabs:panel:jai.memory.used", "0 KB");
    }
    
    @Test
    public void testFreeLocks() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:free.locks", false);
        tester.assertRenderedPage(StatusPage.class);
    }
    @Test
    public void testFreeMemory() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:free.memory", false);
        tester.assertRenderedPage(StatusPage.class);
    }
    
    @Test
    public void testFreeMemoryJAI() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:free.memory.jai", false);
        tester.assertRenderedPage(StatusPage.class);
    }
    
    @Test
    public void testClearCache() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:clear.resourceCache", true);
        tester.assertRenderedPage(StatusPage.class);
    }

    @Test
    public void testReloadConfig() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:reload.catalogConfig", true);
        tester.assertRenderedPage(StatusPage.class);
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
    
    @Test
    public void testModuleStatusPanel() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        tester.assertContains("gs-main");
    }

    @Test
    public void testModuleStatusPopup() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        tester.clickLink("tabs:panel:listViewContainer:modules:1:msg", true);
        tester.assertRenderedPage(StatusPage.class);
        tester.assertContains("GeoServer Main");
        tester.assertContains("gs-main");
        tester.assertContains("Message:");
    }
}
