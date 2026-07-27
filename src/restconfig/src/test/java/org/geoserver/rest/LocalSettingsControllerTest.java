/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.After;
import org.junit.Test;
import org.kordamp.json.JSON;
import org.kordamp.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests for {@link LocalSettingsController}, focusing on workspace validation and CRUD operations for
 * workspace-specific settings.
 */
public class LocalSettingsControllerTest extends CatalogRESTTestSupport {

    private static final String WS = "sf";
    private static final String ENDPOINT = RestBaseController.ROOT_PATH + "/workspaces/%s/settings";

    @After
    public void revert() {
        GeoServer gs = getGeoServer();
        WorkspaceInfo ws = gs.getCatalog().getWorkspaceByName(WS);
        if (ws != null && gs.getSettings(ws) == null) {
            SettingsInfo settings = new SettingsInfoImpl();
            settings.setWorkspace(ws);
            gs.add(settings);
        }
    }

    // -- Non-existent workspace returns 404 --

    @Test
    public void testGetNonExistentWorkspace() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(ENDPOINT.formatted("nonexistent") + ".json");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testPostNonExistentWorkspace() throws Exception {
        String json = "{'settings':{'charset':'UTF-8'}}";
        MockHttpServletResponse response = postAsServletResponse(ENDPOINT.formatted("nonexistent"), json, "text/json");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testPutNonExistentWorkspace() throws Exception {
        String json = "{'settings':{'charset':'UTF-8'}}";
        MockHttpServletResponse response = putAsServletResponse(ENDPOINT.formatted("nonexistent"), json, "text/json");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeleteNonExistentWorkspace() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse(ENDPOINT.formatted("nonexistent"));
        assertEquals(404, response.getStatus());
    }

    // -- CRUD on valid workspace --

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON(ENDPOINT.formatted(WS) + ".json");
        JSONObject settings = ((JSONObject) json).getJSONObject("settings");
        assertNotNull(settings);
        assertEquals(WS, settings.getJSONObject("workspace").get("name"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(ENDPOINT.formatted(WS) + ".xml");
        assertEquals("settings", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo(WS, "/settings/workspace/name", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json = "{'settings':{'workspace':{'name':'%s'},'charset':'ISO-8859-1','numDecimals':5}}".formatted(WS);
        MockHttpServletResponse response = putAsServletResponse(ENDPOINT.formatted(WS), json, "text/json");
        assertEquals(200, response.getStatus());

        JSONObject updated = ((JSONObject) getAsJSON(ENDPOINT.formatted(WS) + ".json")).getJSONObject("settings");
        assertEquals("ISO-8859-1", updated.get("charset"));
        assertEquals("5", updated.get("numDecimals").toString().trim());
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml = "<settings><workspace><name>%s</name></workspace><charset>ISO-8859-1</charset></settings>"
                .formatted(WS);
        MockHttpServletResponse response = putAsServletResponse(ENDPOINT.formatted(WS), xml, MediaType.TEXT_XML_VALUE);
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(ENDPOINT.formatted(WS) + ".xml");
        assertXpathEvaluatesTo("ISO-8859-1", "/settings/charset", dom);
    }

    @Test
    public void testDeleteAndRecreate() throws Exception {
        // delete
        assertEquals(200, deleteAsServletResponse(ENDPOINT.formatted(WS)).getStatus());

        // recreate via POST
        String json = "{'settings':{'workspace':{'name':'%s'},'charset':'UTF-8','numDecimals':8}}".formatted(WS);
        MockHttpServletResponse response = postAsServletResponse(ENDPOINT.formatted(WS), json, "text/json");
        assertEquals(201, response.getStatus());

        // verify
        JSONObject settings = ((JSONObject) getAsJSON(ENDPOINT.formatted(WS) + ".json")).getJSONObject("settings");
        assertEquals(WS, settings.getJSONObject("workspace").get("name"));
    }

    @Test
    public void testDeleteNonExistentSettings() throws Exception {
        // delete settings first
        deleteAsServletResponse(ENDPOINT.formatted(WS));

        // deleting again should return 404
        MockHttpServletResponse response = deleteAsServletResponse(ENDPOINT.formatted(WS));
        assertEquals(404, response.getStatus());
    }
}
