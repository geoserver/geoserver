/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web;

import org.geoserver.wcs.WCSInfo;
import org.geoserver.web.wicket.KeywordsEditor;
import org.junit.Test;

public class WCSAdminPageTest extends GeoServerWicketCoverageTestSupport {

    @Test
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
