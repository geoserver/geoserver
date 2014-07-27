/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.util.Collections;

import org.apache.wicket.PageParameters;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.ows.util.OwsUtils;
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

    @Test
    public void testWorkspace() throws Exception {
        GeoServer geoServer = getGeoServerApplication().getGeoServer();
        WPSInfo wps = geoServer.getService(WPSInfo.class);
        WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();
        WPSInfo wps2 = geoServer.getFactory().create(WPSInfo.class);
        OwsUtils.copy(wps, wps2, WPSInfo.class);
        ((ServiceInfoImpl) wps2).setId(null);
        wps2.setWorkspace(defaultWs);
        wps2.setMaintainer("TestMaintainer");
        geoServer.add(wps2);

        // start the page with the custom workspace
        login();
        tester.startPage(WPSAdminPage.class,
                new PageParameters(Collections.singletonMap("workspace", defaultWs.getName())));
        // print(tester.getLastRenderedPage(), true, true, true);

        // test that components have been filled as expected
        tester.assertModelValue("form:maintainer", "TestMaintainer");

    }
}
