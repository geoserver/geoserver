/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class LocalWMTSSettingsControllerTest extends CatalogRESTTestSupport {

    @After
    public void clearLocalWorkspace() throws Exception {
        LocalWorkspace.remove();
        revertService(WMTSInfo.class, "sf");
    }

    @Before
    public void initLocalWMTS() throws Exception {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WMTSInfo wmtsInfo = geoServer.getService(ws, WMTSInfo.class);
        if (wmtsInfo != null) {
            geoServer.remove(wmtsInfo);
        }
        wmtsInfo = geoServer.getFactory().create(WMTSInfo.class);

        wmtsInfo.setName("WMTS");
        wmtsInfo.setWorkspace(ws);
        geoServer.add(wmtsInfo);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH
                                + "/services/wmts/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmtsinfo = (JSONObject) jsonObject.get("wmts");
        // assertEquals("wmts", wmtsinfo.get("id"));
        assertEquals("WMTS", wmtsinfo.get("name"));
        JSONObject workspace = (JSONObject) wmtsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings.xml");
        assertEquals("wmts", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("true", "/wmts/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wmts/workspace/name", dom);
        assertXpathEvaluatesTo("WMTS", "/wmts/name", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings.html");
    }

    @Test
    public void testCreateAsJSON() throws Exception {
        removeLocalWorkspace();
        String input =
                "{'wmts': {'id' : 'wmts_sf', 'workspace':{'name':'sf'},'name' : 'WMTS', 'enabled': 'true'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings/",
                        input,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH
                                + "/services/wmts/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmtsinfo = (JSONObject) jsonObject.get("wmts");
        assertEquals("WMTS", wmtsinfo.get("name"));
        assertEquals("true", wmtsinfo.get("enabled").toString().trim());
        JSONObject workspace = (JSONObject) wmtsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
    }

    @Test
    public void testCreateAsXML() throws Exception {
        removeLocalWorkspace();
        String xml =
                "<wmts>"
                        + "<id>wmts_sf</id>"
                        + "<workspace>"
                        + "<name>sf</name>"
                        + "</workspace>"
                        + "<name>WMTS</name>"
                        + "<enabled>false</enabled>"
                        + "</wmts>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings.xml");
        assertEquals("wmts", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("false", "/wmts/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wmts/workspace/name", dom);
        assertXpathEvaluatesTo("WMTS", "/wmts/name", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json =
                "{'wmts': {'id':'wmts','workspace':{'name':'sf'},'enabled':'false','name':'WMTS'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings/",
                        json,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod =
                getAsJSON(
                        RestBaseController.ROOT_PATH
                                + "/services/wmts/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wmtsinfo = (JSONObject) jsonObject.get("wmts");
        // assertEquals("wmts", wmtsinfo.get("id"));
        assertEquals("false", wmtsinfo.get("enabled").toString().trim());
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml =
                "<wmts>"
                        + "<id>wmts</id>"
                        + "<workspace>"
                        + "<name>sf</name>"
                        + "</workspace>"
                        + "<enabled>false</enabled>"
                        + "</wmts>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings.xml");
        assertXpathEvaluatesTo("false", "/wmts/enabled", dom);
    }

    @Test
    public void testPutFullAsXML() throws Exception {
        String xml =
                IOUtils.toString(
                        LocalWMTSSettingsControllerTest.class.getResourceAsStream("wmts.xml"),
                        "UTF-8");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH + "/services/wmts/workspaces/sf/settings.xml");
        assertXpathEvaluatesTo("true", "/wmts/enabled", dom);
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/services/wmts/workspaces/sf/settings")
                        .getStatus());
        boolean thrown = false;
        try {
            JSON json =
                    getAsJSON(
                            RestBaseController.ROOT_PATH
                                    + "/services/wmts/workspaces/sf/settings.json");
        } catch (JSONException e) {
            thrown = true;
        }
        assertEquals(true, thrown);
    }

    private void removeLocalWorkspace() {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WMTSInfo wmtsInfo = geoServer.getService(ws, WMTSInfo.class);
        geoServer.remove(wmtsInfo);
    }
}
