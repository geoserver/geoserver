/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.wmts;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class WMTSAdminPageTest extends GeoServerWicketTestSupport {

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
        tester.assertModelValue("form:enabled", wmts.isEnabled());
        tester.assertModelValue("form:title", wmts.getTitle());
        tester.assertModelValue("form:maintainer", wmts.getMaintainer());
        tester.assertModelValue("form:abstract", wmts.getAbstract());
        tester.assertModelValue("form:accessConstraints", wmts.getAccessConstraints());
        tester.assertModelValue("form:fees", wmts.getFees());
        tester.assertModelValue("form:onlineResource", wmts.getOnlineResource());
    }

    @Test
    public void testFormSubmit() throws Exception {
        tester.startPage(WMTSAdminPage.class);
        // let's submit the form
        FormTester formTester = tester.newFormTester("form");
        formTester.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }
}
