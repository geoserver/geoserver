package org.geoserver.service.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSInfo;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class LocalWFSSettingsTest extends CatalogRESTTestSupport {

    protected GeoServer geoServer;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        geoServer = GeoServerExtensions.bean(GeoServer.class, applicationContext);
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        LocalWorkspace.set(ws);
        WFSInfo wfsInfo = geoServer.getService(WFSInfo.class);
        wfsInfo.setWorkspace(ws);
        geoServer.save(wfsInfo);
    }

    @Override
    protected void tearDownInternal() throws Exception {
        LocalWorkspace.remove();
    }

    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON("/rest/services/wfs/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("wfs", wfsinfo.get("id"));
        assertEquals("My GeoServer WFS", wfsinfo.get("name"));
        JSONObject workspace = (JSONObject) wfsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
        assertEquals("COMPLETE", wfsinfo.get("serviceLevel"));
        assertEquals("1000000", wfsinfo.get("maxFeatures").toString().trim());
    }

    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/services/wfs/sf/settings.xml");
        assertEquals("wfs", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("true", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wfs/workspace/name", dom);
        assertXpathEvaluatesTo("My GeoServer WFS", "/wfs/name", dom);
        assertXpathEvaluatesTo("COMPLETE", "/wfs/serviceLevel", dom);
        assertXpathEvaluatesTo("1000000", "/wfs/maxFeatures", dom);
    }

    public void testCreateAsJSON() throws Exception {
        removeLocalWorkspace();
        String input = "{'wfs': {'id' : 'wfs', 'name' : 'WFS', 'workspace': {'name': 'sf'},'enabled': 'true'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wfs/sf/settings/",
                input, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON json = getAsJSON("/rest/services/wfs/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("wfs", wmsinfo.get("id"));
        assertEquals("WFS", wmsinfo.get("name"));
        JSONObject workspace = (JSONObject) wmsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
    }

    public void testCreateAsXML() throws Exception {
        removeLocalWorkspace();
        String xml = "<wfs>" + "<id>wfs</id>" + "<workspace>" + "<name>sf</name>"
                + "</workspace>" + "<name>OGC:WFS</name>" + "<enabled>false</enabled>"
                + "</wfs>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wfs/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatusCode());

        Document dom = getAsDOM("/rest/services/wfs/sf/settings.xml");
        assertEquals("wfs", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("false", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wfs/workspace/name", dom);
        assertXpathEvaluatesTo("OGC:WFS", "/wfs/name", dom);
    }

    public void testPutAsJSON() throws Exception {
        String json = "{'wfs': {'id':'wfs','workspace':{'name':'sf'},'enabled':'false','name':'WFS'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wfs/sf/settings/",
                json, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/services/wfs/sf/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("wfs", wfsinfo.get("id"));
        assertEquals("false", wfsinfo.get("enabled").toString().trim());
    }

    public void testPutAsXML() throws Exception {
        String xml = "<wfs>" + "<id>wfs</id>" + "<workspace>" + "<name>sf</name>"
                + "</workspace>" + "<enabled>false</enabled>" + "</wfs>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wfs/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatusCode());
        Document dom = getAsDOM("/rest/services/wfs/sf/settings.xml");
        assertXpathEvaluatesTo("false", "/wfs/enabled", dom);
    }

    public void testDelete() throws Exception {
        assertEquals(200, deleteAsServletResponse("/rest/services/wfs/sf/settings").getStatusCode());
        boolean thrown = false;
        try {
            JSON json = getAsJSON("/rest/services/wfs/sf/settings.json");
        } catch (JSONException e) {
            thrown = true;
        }
        assertEquals(true, thrown);
    }

    private void removeLocalWorkspace() {
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WFSInfo wfsInfo = geoServer.getService(ws, WFSInfo.class);
        geoServer.remove(wfsInfo);
    }
}
