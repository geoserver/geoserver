package org.geoserver.wcs.web;

import org.geoserver.wcs.WCSInfo;
import org.geoserver.web.wicket.KeywordsEditor;

public class WCSAdminPageTest extends GeoServerWicketCoverageTestSupport {

    public void test() throws Exception {
        login();
        WCSInfo wcs = getGeoServerApplication().getGeoServer().getService(WCSInfo.class);
        
        // start the page
        tester.startPage(new WCSAdminPage());
        
        // test that components have been filled as expected
        tester.assertComponent("form:keywords", KeywordsEditor.class);
        tester.assertModelValue("form:keywords", wcs.getKeywords());
    }
}
