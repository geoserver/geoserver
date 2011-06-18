package org.geoserver.wfs.web;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.WFSInfo;

public class WFSAdminPageTest extends GeoServerWicketTestSupport {
    public void testValues() throws Exception {
        WFSInfo wfs = getGeoServerApplication().getGeoServer().getService(WFSInfo.class);

        login();
        tester.startPage(WFSAdminPage.class);
        tester.assertModelValue("form:maxFeatures", wfs.getMaxFeatures());
        tester.assertModelValue("form:keywords", wfs.getKeywords());
    }
}
