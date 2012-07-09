package org.geoserver.service.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WFSSettingsTest extends CatalogRESTTestSupport {

    protected GeoServer geoServer;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        geoServer = GeoServerExtensions.bean(GeoServer.class, applicationContext);
    }

    public void testGetASJSON() throws Exception {
        JSON json = getAsJSON("/rest/services/wfs/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("wfs", wfsinfo.get("id"));
        assertEquals("true", wfsinfo.get("enabled").toString().trim());
        assertEquals("My GeoServer WFS", wfsinfo.get("name"));
        assertEquals("COMPLETE", wfsinfo.get("serviceLevel"));
        assertEquals("1000000", wfsinfo.get("maxFeatures").toString().trim());
    }

    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/services/wfs/settings.xml");
        assertEquals("wfs", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("My GeoServer WFS", "/wfs/name", dom);
        assertXpathEvaluatesTo("COMPLETE", "/wfs/serviceLevel", dom);
        assertXpathEvaluatesTo("1000000", "/wfs/maxFeatures", dom);
    }

    public void testPutAsJSON() throws Exception {
        String json = "{'wfs': {'id':'wfs','enabled':'false','name':'WFS'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wfs/settings/",
                json, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/services/wfs/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("wfs", wfsinfo.get("id"));
        assertEquals("false", wfsinfo.get("enabled").toString().trim());
        assertEquals("WFS", wfsinfo.get("name"));
    }

    public void testPutASXML() throws Exception {
        String xml = "<wfs>"
                + "<id>wfs</id>"
                + "<enabled>disabled</enabled>"
                + "<name>WFS</name><title>GeoServer Web Feature Service</title>"
                + "<maintainer>http://jira.codehaus.org/secure/BrowseProject.jspa?id=10311</maintainer>"
                + "</wfs>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wfs/settings", xml,
                "text/xml");
        assertEquals(200, response.getStatusCode());
        Document dom = getAsDOM("/rest/services/wfs/settings.xml");
        assertXpathEvaluatesTo("false", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("WFS", "/wfs/name", dom);
    }

    public void testDelete() throws Exception {
        assertEquals(405, deleteAsServletResponse("/rest/services/wfs/settings").getStatusCode());
    }
}
