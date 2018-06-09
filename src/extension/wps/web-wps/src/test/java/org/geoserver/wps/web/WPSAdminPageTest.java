/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geoserver.wps.WPSInfo;
import org.junit.Test;

public class WPSAdminPageTest extends WPSPagesTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        wps.setMaxAsynchronousExecutionTime(600);
        wps.setMaxSynchronousExecutionTime(60);
        wps.setMaxSynchronousProcesses(16);
        wps.setMaxAsynchronousProcesses(16);
        getGeoServer().save(wps);
    }

    @Test
    public void test() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSAdminPage());
        // print(tester.getLastRenderedPage(), true, true);

        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        wps.setMaxAsynchronousTotalTime(6000);
        wps.setMaxSynchronousTotalTime(120);
        getGeoServer().save(wps);

        // test that components have been filled as expected
        tester.assertComponent("form:keywords", KeywordsEditor.class);
        tester.assertModelValue("form:keywords", wps.getKeywords());
        tester.assertModelValue("form:maxSynchronousProcesses:", 16);
        tester.assertModelValue("form:maxAsynchronousProcesses:", 16);
        tester.assertModelValue("form:maxSynchronousExecutionTime:", 60);
        tester.assertModelValue("form:maxAsynchronousExecutionTime:", 600);
        tester.assertModelValue("form:maxSynchronousTotalTime:", 120);
        tester.assertModelValue("form:maxAsynchronousTotalTime:", 6000);
    }

    @Test
    public void testUpgrade() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSAdminPage());

        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        getGeoServer().save(wps);

        // test that components have been filled as expected
        tester.assertModelValue("form:maxSynchronousExecutionTime:", 60);
        tester.assertModelValue("form:maxAsynchronousExecutionTime:", 600);
        tester.assertModelValue("form:maxSynchronousTotalTime:", 60);
        tester.assertModelValue("form:maxAsynchronousTotalTime:", 600);
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
        tester.startPage(
                WPSAdminPage.class, new PageParameters().add("workspace", defaultWs.getName()));
        // print(tester.getLastRenderedPage(), true, true, true);

        // test that components have been filled as expected
        tester.assertModelValue("form:maintainer", "TestMaintainer");
    }
}
