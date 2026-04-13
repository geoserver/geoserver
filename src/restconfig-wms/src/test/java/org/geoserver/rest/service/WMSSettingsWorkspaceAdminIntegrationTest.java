/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.WorkspaceAdminCatalogRESTTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.wms.WMSInfo;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.kordamp.json.JSON;
import org.kordamp.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests that workspace administrators can manage per-workspace WMS service settings for workspaces they administer,
 * cannot access settings for other workspaces, and cannot access or modify global WMS service settings.
 */
@TestSetup(run = TestSetupFrequency.ONCE)
public class WMSSettingsWorkspaceAdminIntegrationTest extends WorkspaceAdminCatalogRESTTestSupport {

    private static final String WS_ENDPOINT = RestBaseController.ROOT_PATH + "/services/wms/workspaces/%s/settings";
    private static final String GLOBAL_ENDPOINT = RestBaseController.ROOT_PATH + "/services/wms/settings";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        ensureWMSSettings(WS);
    }

    @After
    public void revert() {
        revertService(WMSInfo.class, WS);
        ensureWMSSettings(WS);
    }

    private void ensureWMSSettings(String workspace) {
        GeoServer gs = getGeoServer();
        WorkspaceInfo ws = gs.getCatalog().getWorkspaceByName(workspace);
        WMSInfo wmsInfo = gs.getService(ws, WMSInfo.class);
        if (wmsInfo == null) {
            wmsInfo = gs.getFactory().create(WMSInfo.class);
            wmsInfo.setName("WMS");
            wmsInfo.setWorkspace(ws);
            gs.add(wmsInfo);
        }
    }

    //
    // Workspace-specific settings (allowed for adminable workspace)
    //

    @Test
    public void testGetAsJSON() throws Exception {
        setWorkspaceAdminRequestAuth();

        JSON json = getAsJSON(WS_ENDPOINT.formatted(WS) + ".json");
        JSONObject wmsinfo = ((JSONObject) json).getJSONObject("wms");
        assertNotNull(wmsinfo);
        assertEquals("WMS", wmsinfo.get("name"));
        assertEquals(WS, wmsinfo.getJSONObject("workspace").get("name"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        setWorkspaceAdminRequestAuth();

        Document dom = getAsDOM(WS_ENDPOINT.formatted(WS) + ".xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
    }

    @Test
    public void testPutAsJSON() throws Exception {
        setWorkspaceAdminRequestAuth();

        String json = "{'wms': {'workspace':{'name':'%s'},'enabled':'false','name':'WMS'}}".formatted(WS);
        MockHttpServletResponse response = putAsServletResponse(WS_ENDPOINT.formatted(WS), json, "text/json");
        assertEquals(200, response.getStatus());

        JSONObject updated = ((JSONObject) getAsJSON(WS_ENDPOINT.formatted(WS) + ".json")).getJSONObject("wms");
        assertEquals("false", updated.get("enabled").toString().trim());
    }

    @Test
    public void testPutAsXML() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<wms><workspace><name>%s</name></workspace><enabled>false</enabled></wms>".formatted(WS);
        MockHttpServletResponse response =
                putAsServletResponse(WS_ENDPOINT.formatted(WS), xml, MediaType.TEXT_XML_VALUE);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteAllowed() throws Exception {
        setWorkspaceAdminRequestAuth();

        assertEquals(200, deleteAsServletResponse(WS_ENDPOINT.formatted(WS)).getStatus());
    }

    //
    // Other workspace settings (denied)
    //

    @Test
    public void testGetOtherWorkspaceDenied() throws Exception {
        ensureWMSSettings(WS_OTHER);

        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse(WS_ENDPOINT.formatted(WS_OTHER) + ".xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testPutOtherWorkspaceDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<wms><workspace><name>%s</name></workspace><enabled>false</enabled></wms>".formatted(WS_OTHER);
        MockHttpServletResponse response =
                putAsServletResponse(WS_ENDPOINT.formatted(WS_OTHER), xml, MediaType.TEXT_XML_VALUE);
        assertThat(
                "Workspace admin should not be able to modify WMS settings in non-adminable workspace",
                response.getStatus(),
                Matchers.oneOf(403, 404));
    }

    @Test
    public void testDeleteOtherWorkspaceDenied() throws Exception {
        ensureWMSSettings(WS_OTHER);

        setWorkspaceAdminRequestAuth();

        assertEquals(
                404, deleteAsServletResponse(WS_ENDPOINT.formatted(WS_OTHER)).getStatus());
    }

    //
    // Global WMS settings (denied for workspace admins)
    //

    // Verifies workspace admins cannot access global service settings.
    // The rule /rest/services/*/workspaces/{workspace}/settings=rw only matches
    // per-workspace endpoints, not /rest/services/wms/settings (global).
    @Test
    public void testGetGlobalSettingsDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        assertEquals(403, getAsServletResponse(GLOBAL_ENDPOINT + ".xml").getStatus());
    }

    @Test
    public void testPutGlobalSettingsDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<wms><name>WMS</name><enabled>false</enabled></wms>";
        MockHttpServletResponse response = putAsServletResponse(GLOBAL_ENDPOINT, xml, MediaType.TEXT_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDeleteGlobalSettingsDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = deleteAsServletResponse(GLOBAL_ENDPOINT);
        assertThat(
                "Workspace admin should not be able to delete global WMS settings",
                response.getStatus(),
                Matchers.oneOf(403, 405));
    }
}
