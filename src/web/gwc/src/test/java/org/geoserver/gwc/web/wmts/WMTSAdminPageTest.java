/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.wmts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class WMTSAdminPageTest extends GeoServerWicketTestSupport {
    /** Location of general service panel within form */
    final String SERVICE_ADMIN_PANEL = "tabs:panel";
    /** Location of WMTSAdminPanel within form */
    final String WMTS_ADMIN_PANEL = "tabs:panel:initial";

    private WMTSInfo wmts;

    @Before
    public void beforeTest() throws Exception {
        wmts = getGeoServerApplication().getGeoServer().getService(WMTSInfo.class);
        login();
    }

    @Test
    public void testPageStarts() throws Exception {
        tester.startPage(WMTSAdminPage.class);
        // let's see if the page was properly filled
        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":serviceControl:enabled", wmts.isEnabled());
        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":serviceTitleAndAbstract:title", wmts.getTitle());
        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":maintainer", wmts.getMaintainer());
        tester.assertModelValue(
                "form:" + SERVICE_ADMIN_PANEL + ":serviceTitleAndAbstract:abstract", wmts.getAbstract());
        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":accessConstraints", wmts.getAccessConstraints());
        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":fees", wmts.getFees());
        tester.assertModelValue("form:" + SERVICE_ADMIN_PANEL + ":onlineResource", wmts.getOnlineResource());
    }

    @Test
    public void testFormSubmit() throws Exception {
        // get WMTS service information
        WMTSInfo wmtsInfo = getGeoServerApplication().getGeoServer().getService(WMTSInfo.class);
        // start WMTS administration page
        tester.startPage(WMTSAdminPage.class);

        // let's submit the form
        FormTester formTester = tester.newFormTester("form");
        // change cite compliance value
        boolean citeCompliant = wmtsInfo.isCiteCompliant();

        formTester.setValue(SERVICE_ADMIN_PANEL + ":serviceControl:citeCompliant", !citeCompliant);
        // submit form
        formTester.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
        // check the service info object was correctly updated
        assertThat(wmtsInfo.isCiteCompliant(), is(!citeCompliant));
    }
}
