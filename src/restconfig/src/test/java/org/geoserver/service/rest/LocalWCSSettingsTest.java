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
import org.geoserver.wcs.WCSInfo;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class LocalWCSSettingsTest extends CatalogRESTTestSupport {

    protected GeoServer geoServer;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        geoServer = GeoServerExtensions.bean(GeoServer.class, applicationContext);
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        LocalWorkspace.set(ws);
        WCSInfo wcsInfo = geoServer.getService(WCSInfo.class);
        wcsInfo.setWorkspace(ws);
        geoServer.save(wcsInfo);
    }

    @Override
    protected void tearDownInternal() throws Exception {
        LocalWorkspace.remove();
    }

    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON("/rest/services/wcs/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wcsinfo = (JSONObject) jsonObject.get("wcs");
        assertEquals("wcs", wcsinfo.get("id"));
        assertEquals("My GeoServer WCS", wcsinfo.get("name"));
        JSONObject workspace = (JSONObject) wcsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
        assertEquals("false", wcsinfo.get("verbose").toString().trim());
    }

    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/services/wcs/sf/settings.xml");
        assertEquals("wcs", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("true", "/wcs/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wcs/workspace/name", dom);
        assertXpathEvaluatesTo("My GeoServer WCS", "/wcs/name", dom);
        assertXpathEvaluatesTo("false", "/wcs/verbose", dom);
    }

    public void testCreateAsJSON() throws Exception {
        removeLocalWorkspace();
        String input = "{'wcs': {'id' : 'wcs', 'name' : 'WCS', 'workspace': {'name': 'sf'},'enabled': 'true'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wcs/sf/settings/",
                input, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON json = getAsJSON("/rest/services/wcs/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wcs");
        assertEquals("wcs", wmsinfo.get("id"));
        assertEquals("WCS", wmsinfo.get("name"));
        JSONObject workspace = (JSONObject) wmsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
    }

    public void testCreateAsXML() throws Exception {
        removeLocalWorkspace();
        String xml = "<wcs>" + "<id>wcs</id>" + "<workspace>" + "<name>sf</name>"
                + "</workspace>" + "<name>OGC:WCS</name>" + "<enabled>false</enabled>"
                + "</wcs>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wcs/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatusCode());

        Document dom = getAsDOM("/rest/services/wcs/sf/settings.xml");
        assertEquals("wcs", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("false", "/wcs/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wcs/workspace/name", dom);
        assertXpathEvaluatesTo("OGC:WCS", "/wcs/name", dom);
    }

    public void testPutAsJSON() throws Exception {
        String json = "{'wcs': {'id':'wcs','workspace':{'name':'sf'},'enabled':'false','name':'WCS'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wcs/sf/settings/",
                json, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/services/wcs/sf/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wcsinfo = (JSONObject) jsonObject.get("wcs");
        assertEquals("wcs", wcsinfo.get("id"));
        assertEquals("false", wcsinfo.get("enabled").toString().trim());
    }

    public void testPutAsXML() throws Exception {
        String xml = "<wcs>" + "<id>wcs</id>" + "<workspace>" + "<name>sf</name>"
                + "</workspace>" + "<enabled>false</enabled>" + "</wcs>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wcs/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatusCode());
        Document dom = getAsDOM("/rest/services/wcs/sf/settings.xml");
        assertXpathEvaluatesTo("false", "/wcs/enabled", dom);
    }

    public void testDelete() throws Exception {
        assertEquals(200, deleteAsServletResponse("/rest/services/wcs/sf/settings").getStatusCode());
        boolean thrown = false;
        try {
            JSON json = getAsJSON("/rest/services/wcs/sf/settings.json");
        } catch (JSONException e) {
            thrown = true;
        }
        assertEquals(true, thrown);
    }

    private void removeLocalWorkspace() {
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WCSInfo wcsInfo = geoServer.getService(ws, WCSInfo.class);
        geoServer.remove(wcsInfo);
    }
}
