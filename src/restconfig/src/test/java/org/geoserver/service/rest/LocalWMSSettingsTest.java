/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.service.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.io.IOUtils;

import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.wms.WMSInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import org.springframework.mock.web.MockHttpServletResponse;

public class LocalWMSSettingsTest extends CatalogRESTTestSupport {

    @After
    public void clearLocalWorkspace() throws Exception {
        LocalWorkspace.remove();
        revertService(WMSInfo.class, "sf");
    }

    @Before
    public void initLocalWMS() throws Exception {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WMSInfo wmsInfo = geoServer.getService(ws, WMSInfo.class);
        if (wmsInfo != null) {
            geoServer.remove(wmsInfo);
        }
        wmsInfo = geoServer.getFactory().create(WMSInfo.class);
        
        wmsInfo.setName("WMS");
        wmsInfo.setWorkspace(ws);
        geoServer.add(wmsInfo);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON("/rest/services/wms/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");
        //assertEquals("wms", wmsinfo.get("id"));
        assertEquals("WMS", wmsinfo.get("name"));
        JSONObject workspace = (JSONObject) wmsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
        JSONObject watermark = (JSONObject) wmsinfo.get("watermark");
        assertEquals("false", watermark.get("enabled").toString().trim());
        assertEquals("Nearest", wmsinfo.get("interpolation"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/services/wms/workspaces/sf/settings.xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wms/workspace/name", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
        assertXpathEvaluatesTo("false", "/wms/watermark/enabled", dom);
        assertXpathEvaluatesTo("Nearest", "/wms/interpolation", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM("/rest/services/wms/workspaces/sf/settings.html" );
    }

    public void testCreateAsJSON() throws Exception {
        removeLocalWorkspace();
        String input = "{'wms': {'id' : 'wms_sf', 'workspace':{'name':'sf'},'name' : 'WMS', 'enabled': 'true'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wms/workspaces/sf/settings/",
                input, "text/json");
        assertEquals(200, response.getStatus());
        JSON json = getAsJSON("/rest/services/wms/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");
        assertEquals("wms_sf", wmsinfo.get("id"));
        assertEquals("WMS", wmsinfo.get("name"));
        assertEquals("true", wmsinfo.get("enabled").toString().trim());
        JSONObject workspace = (JSONObject) wmsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
    }

    @Test
    public void testCreateAsXML() throws Exception {
        removeLocalWorkspace();
        String xml = "<wms>" + "<id>wms_sf</id>" + "<workspace>" + "<name>sf</name>"
                + "</workspace>" + "<name>OGC:WMS</name>" + "<enabled>false</enabled>"
                + "<interpolation>Nearest</interpolation>" + "</wms>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wms/workspaces/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM("/rest/services/wms/workspaces/sf/settings.xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wms/workspace/name", dom);
        assertXpathEvaluatesTo("OGC:WMS", "/wms/name", dom);
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
        assertXpathEvaluatesTo("Nearest", "/wms/interpolation", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json = "{'wms': {'id':'wms','workspace':{'name':'sf'},'enabled':'false','name':'WMS'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wms/workspaces/sf/settings/",
                json, "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod = getAsJSON("/rest/services/wms/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");
        //assertEquals("wms", wmsinfo.get("id"));
        assertEquals("false", wmsinfo.get("enabled").toString().trim());
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml = "<wms>" + "<id>wms</id>" + "<workspace>" + "<name>sf</name>"
                + "</workspace>" + "<enabled>false</enabled>" + "</wms>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wms/workspaces/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM("/rest/services/wms/workspaces/sf/settings.xml");
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
    }
    
    @Test
    public void testPutFullAsXML() throws Exception {
        String xml = IOUtils.toString(LocalWFSSettingsTest.class.getResourceAsStream("wms.xml"));
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wms/workspaces/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM("/rest/services/wms/workspaces/sf/settings.xml");
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(200, deleteAsServletResponse("/rest/services/wms/workspaces/sf/settings").getStatus());
        boolean thrown = false;
        try {
            JSON json = getAsJSON("/rest/services/wms/workspaces/sf/settings.json");
        } catch (JSONException e) {
            thrown = true;
        }
        assertEquals(true, thrown);
    }

    private void removeLocalWorkspace() {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WMSInfo wmsInfo = geoServer.getService(ws, WMSInfo.class);
        geoServer.remove(wmsInfo);
    }
}
