/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class CoverageAccessPageTest extends GeoServerWicketTestSupport {
    private GeoServer geoServer;

    @Before
    public void reset() {
        geoServer = getGeoServerApplication().getGeoServer();
        GeoServerInfo gsInfo = geoServer.getGlobal();
        CoverageAccessInfo cai = gsInfo.getCoverageAccess();
        cai.setCorePoolSize(2);
        geoServer.save(gsInfo);
    }

    @Test
    public void testLoad() throws Exception {
        login();
        tester.startPage(CoverageAccessPage.class);
        tester.assertRenderedPage(CoverageAccessPage.class);
    }

    @Test
    public void testSave() throws Exception {
        login();

        tester.startPage(CoverageAccessPage.class);
        print(tester.getLastRenderedPage(), true, true);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("corePoolSize", "3");
        ft.submit("submit");

        tester.assertRenderedPage(GeoServerHomePage.class);
        CoverageAccessInfo cai = geoServer.getGlobal().getCoverageAccess();
        assertEquals(3, cai.getCorePoolSize());
    }

    @Test
    public void testApply() throws Exception {
        login();

        tester.startPage(CoverageAccessPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("corePoolSize", "5");
        ft.submit("apply");

        tester.assertRenderedPage(CoverageAccessPage.class);
        CoverageAccessInfo cai = geoServer.getGlobal().getCoverageAccess();
        assertEquals(5, cai.getCorePoolSize());
    }
}
