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
import org.geoserver.wfs.WFSInfo;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.kordamp.json.JSON;
import org.kordamp.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests that workspace administrators can manage per-workspace WFS service settings for workspaces they administer,
 * cannot access settings for other workspaces, and cannot access or modify global WFS service settings.
 */
@TestSetup(run = TestSetupFrequency.ONCE)
public class WFSSettingsWorkspaceAdminIntegrationTest extends WorkspaceAdminCatalogRESTTestSupport {

    private static final String WS_ENDPOINT = RestBaseController.ROOT_PATH + "/services/wfs/workspaces/%s/settings";
    private static final String GLOBAL_ENDPOINT = RestBaseController.ROOT_PATH + "/services/wfs/settings";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        ensureWFSSettings(WS);
    }

    @After
    public void revert() {
        revertService(WFSInfo.class, WS);
        ensureWFSSettings(WS);
    }

    private void ensureWFSSettings(String workspace) {
        GeoServer gs = getGeoServer();
        WorkspaceInfo ws = gs.getCatalog().getWorkspaceByName(workspace);
        WFSInfo wfsInfo = gs.getService(ws, WFSInfo.class);
        if (wfsInfo == null) {
            wfsInfo = gs.getFactory().create(WFSInfo.class);
            wfsInfo.setName("WFS");
            wfsInfo.setWorkspace(ws);
            gs.add(wfsInfo);
        }
    }

    //
    // Workspace-specific settings (allowed for adminable workspace)
    //

    @Test
    public void testGetAsJSON() throws Exception {
        setWorkspaceAdminRequestAuth();

        JSON json = getAsJSON(WS_ENDPOINT.formatted(WS) + ".json");
        JSONObject wfsinfo = ((JSONObject) json).getJSONObject("wfs");
        assertNotNull(wfsinfo);
        assertEquals("WFS", wfsinfo.get("name"));
        assertEquals(WS, wfsinfo.getJSONObject("workspace").get("name"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        setWorkspaceAdminRequestAuth();

        Document dom = getAsDOM(WS_ENDPOINT.formatted(WS) + ".xml");
        assertEquals("wfs", dom.getDocumentElement().getLocalName());
    }

    @Test
    public void testPutAsJSON() throws Exception {
        setWorkspaceAdminRequestAuth();

        String json = "{'wfs': {'workspace':{'name':'%s'},'enabled':'false','name':'WFS'}}".formatted(WS);
        MockHttpServletResponse response = putAsServletResponse(WS_ENDPOINT.formatted(WS), json, "text/json");
        assertEquals(200, response.getStatus());

        JSONObject updated = ((JSONObject) getAsJSON(WS_ENDPOINT.formatted(WS) + ".json")).getJSONObject("wfs");
        assertEquals("false", updated.get("enabled").toString().trim());
    }

    @Test
    public void testPutAsXML() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<wfs><workspace><name>%s</name></workspace><enabled>false</enabled></wfs>".formatted(WS);
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
        ensureWFSSettings(WS_OTHER);

        setWorkspaceAdminRequestAuth();

        assertEquals(
                404,
                getAsServletResponse(WS_ENDPOINT.formatted(WS_OTHER) + ".xml").getStatus());
    }

    @Test
    public void testPutOtherWorkspaceDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<wfs><workspace><name>%s</name></workspace><enabled>false</enabled></wfs>".formatted(WS_OTHER);
        MockHttpServletResponse response =
                putAsServletResponse(WS_ENDPOINT.formatted(WS_OTHER), xml, MediaType.TEXT_XML_VALUE);
        assertThat(
                "Workspace admin should not be able to modify WFS settings in non-adminable workspace",
                response.getStatus(),
                Matchers.oneOf(403, 404));
    }

    @Test
    public void testDeleteOtherWorkspaceDenied() throws Exception {
        ensureWFSSettings(WS_OTHER);

        setWorkspaceAdminRequestAuth();

        assertEquals(
                404, deleteAsServletResponse(WS_ENDPOINT.formatted(WS_OTHER)).getStatus());
    }

    //
    // Global WFS settings (denied for workspace admins)
    //

    // Verifies workspace admins cannot access global service settings.
    // The rule /rest/services/*/workspaces/{workspace}/settings=rw only matches
    // per-workspace endpoints, not /rest/services/wfs/settings (global).
    @Test
    public void testGetGlobalSettingsDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        assertEquals(403, getAsServletResponse(GLOBAL_ENDPOINT + ".xml").getStatus());
    }

    @Test
    public void testPutGlobalSettingsDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<wfs><name>WFS</name><enabled>false</enabled></wfs>";
        MockHttpServletResponse response = putAsServletResponse(GLOBAL_ENDPOINT, xml, MediaType.TEXT_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDeleteGlobalSettingsDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = deleteAsServletResponse(GLOBAL_ENDPOINT);
        assertThat(
                "Workspace admin should not be able to delete global WFS settings",
                response.getStatus(),
                Matchers.oneOf(403, 405));
    }
}
