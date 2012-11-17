/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geoserver.wps.WPSInfo;
import org.junit.Test;

public class WPSAdminPageTest extends GeoServerWicketTestSupport {

    @Test
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
