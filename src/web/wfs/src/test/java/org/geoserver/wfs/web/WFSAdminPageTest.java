/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.junit.Test;

public class WFSAdminPageTest extends GeoServerWicketTestSupport {
    @Test
    public void testValues() throws Exception {
        WFSInfo wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);

        login();
        tester.startPage(WFSAdminPage.class);
        tester.assertModelValue("form:maxFeatures", wfs.getMaxFeatures());
        tester.assertModelValue("form:maxNumberOfFeaturesForPreview", wfs.getMaxNumberOfFeaturesForPreview());
        tester.assertModelValue("form:keywords", wfs.getKeywords());
    }
    
    @Test
    public void testChangesToValues() throws Exception {
        String testValue1 = "100", testValue2 = "0";
        WFSInfo wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        login();
        tester.startPage(WFSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("maxNumberOfFeaturesForPreview", (String)testValue1);
        ft.submit("submit");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue1 = 100", 100, (int)wfs.getMaxNumberOfFeaturesForPreview());
        tester.startPage(WFSAdminPage.class);
        ft = tester.newFormTester("form");
        ft.setValue("maxNumberOfFeaturesForPreview", (String)testValue2);
        ft.submit("submit");
        wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);
        assertEquals("testValue2 = 0", 0, (int)wfs.getMaxNumberOfFeaturesForPreview());
    }
}
