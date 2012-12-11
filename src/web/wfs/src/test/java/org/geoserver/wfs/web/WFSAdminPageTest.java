/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

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
        tester.assertModelValue("form:keywords", wfs.getKeywords());
    }
}
