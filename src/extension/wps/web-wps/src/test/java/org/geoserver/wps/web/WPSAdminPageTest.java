package org.geoserver.wps.web;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.web.WPSAdminPage;

public class WPSAdminPageTest extends GeoServerWicketTestSupport {

    public void test() throws Exception {
        login();
        WPSInfo wps = getGeoServerApplication().getGeoServer().getService(WPSInfo.class);
        
        // start the page
        tester.startPage(new WPSAdminPage());
        
        // test that components have been filled as expected
        tester.assertComponent("form:keywords", KeywordsEditor.class);
        tester.assertModelValue("form:keywords", wps.getKeywords());
    }
}
