/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class LocalWFSSettingsControllerTest extends CatalogRESTTestSupport {

    @After
    public void revertChanges() {
        LocalWorkspace.remove();
        revertService(WFSInfo.class, "sf");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        LocalWorkspace.set(ws);
        WFSInfo wfsInfo = geoServer.getService(WFSInfo.class);
        wfsInfo.setWorkspace(ws);
        geoServer.save(wfsInfo);
    }

    @After
    public void clearLocalWorkspace() throws Exception {
        LocalWorkspace.remove();
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("WFS", wfsinfo.get("name"));
        JSONObject workspace = (JSONObject) wfsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
        assertEquals("COMPLETE", wfsinfo.get("serviceLevel"));
        assertEquals("1000000", wfsinfo.get("maxFeatures").toString().trim());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings.xml");
        assertEquals("wfs", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("true", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wfs/workspace/name", dom);
        assertXpathEvaluatesTo("WFS", "/wfs/name", dom);
        assertXpathEvaluatesTo("COMPLETE", "/wfs/serviceLevel", dom);
        assertXpathEvaluatesTo("1000000", "/wfs/maxFeatures", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings.html");
    }

    @Test
    public void testCreateAsJSON() throws Exception {
        removeLocalWorkspace();
        String input =
                "{'wfs': {'id' : 'wfs', 'name' : 'WFS', 'workspace': {'name': 'sf'},'enabled': 'true'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings/",
                        input,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("WFS", wmsinfo.get("name"));
        JSONObject workspace = (JSONObject) wmsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
    }

    @Test
    public void testCreateAsXML() throws Exception {
        removeLocalWorkspace();
        String xml =
                "<wfs>"
                        + "<id>wfs</id>"
                        + "<workspace>"
                        + "<name>sf</name>"
                        + "</workspace>"
                        + "<name>OGC:WFS</name>"
                        + "<enabled>false</enabled>"
                        + "</wfs>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings.xml");
        assertEquals("wfs", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("false", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wfs/workspace/name", dom);
        assertXpathEvaluatesTo("OGC:WFS", "/wfs/name", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json =
                "{'wfs': {'id':'wfs','workspace':{'name':'sf'},'enabled':'false','name':'WFS'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings/",
                        json,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod =
                getAsJSON(
                        RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("false", wfsinfo.get("enabled").toString().trim());
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml =
                "<wfs>"
                        + "<id>wfs</id>"
                        + "<workspace>"
                        + "<name>sf</name>"
                        + "</workspace>"
                        + "<enabled>false</enabled>"
                        + "</wfs>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings.xml");
        assertXpathEvaluatesTo("false", "/wfs/enabled", dom);
    }

    @Test
    public void testPutFullAsXML() throws Exception {
        String xml =
                IOUtils.toString(
                        LocalWFSSettingsControllerTest.class.getResourceAsStream("wfs.xml"),
                        "UTF-8");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/workspaces/sf/settings.xml");
        assertXpathEvaluatesTo("true", "/wfs/enabled", dom);
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/services/wfs/workspaces/sf/settings")
                        .getStatus());
        boolean thrown = false;
        try {
            JSON json =
                    getAsJSON(
                            RestBaseController.ROOT_PATH
                                    + "/services/wfs/workspaces/sf/settings.json");
        } catch (JSONException e) {
            thrown = true;
        }
        assertEquals(true, thrown);
    }

    private void removeLocalWorkspace() {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WFSInfo wfsInfo = geoServer.getService(ws, WFSInfo.class);
        geoServer.remove(wfsInfo);
    }
}
