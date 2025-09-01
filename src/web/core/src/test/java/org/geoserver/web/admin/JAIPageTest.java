/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class JAIPageTest extends GeoServerWicketTestSupport {

    private GeoServer geoServer;

    @Before
    public void reset() {
        geoServer = getGeoServerApplication().getGeoServer();
        GeoServerInfo gsInfo = geoServer.getGlobal();
        JAIInfo jai = gsInfo.getJAI();
        jai.setTileThreads(2);
        geoServer.save(gsInfo);
    }

    @Test
    public void testValues() {
        JAIInfo info = geoServer.getGlobal().getJAI();

        login();

        tester.startPage(JAIPage.class);
        tester.assertComponent("form:tileThreads", TextField.class);
        tester.assertModelValue("form:tileThreads", info.getTileThreads());
    }

    @Test
    public void testSave() {
        login();

        tester.startPage(JAIPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("tileThreads", "3");
        ft.submit("submit");

        tester.assertRenderedPage(GeoServerHomePage.class);
        JAIInfo jai = geoServer.getGlobal().getJAI();
        assertEquals(3, jai.getTileThreads());
    }

    @Test
    public void testApply() {
        login();

        tester.startPage(JAIPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("tileThreads", "3");
        ft.submit("apply");

        tester.assertRenderedPage(JAIPage.class);
        JAIInfo jai = geoServer.getGlobal().getJAI();
        assertEquals(3, jai.getTileThreads());
    }
}
