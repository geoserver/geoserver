/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.service.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WFSSettingsTest extends CatalogRESTTestSupport {
    
    @Before 
    public void revertChanges() {
        revertService(WFSInfo.class, null);
    }

    @Test
    public void testGetASJSON() throws Exception {
        JSON json = getAsJSON("/rest/services/wfs/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("true", wfsinfo.get("enabled").toString().trim());
        assertEquals("WFS", wfsinfo.get("name"));
        assertEquals("COMPLETE", wfsinfo.get("serviceLevel"));
        assertEquals("1000000", wfsinfo.get("maxFeatures").toString().trim());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/services/wfs/settings.xml");
        assertEquals("wfs", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("WFS", "/wfs/name", dom);
        assertXpathEvaluatesTo("COMPLETE", "/wfs/serviceLevel", dom);
        assertXpathEvaluatesTo("1000000", "/wfs/maxFeatures", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json = "{'wfs': {'id':'wfs','enabled':'false','name':'WFS'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wfs/settings/",
                json, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/services/wfs/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("false", wfsinfo.get("enabled").toString().trim());
        assertEquals("WFS", wfsinfo.get("name"));
    }

    @Test
    public void testPutASXML() throws Exception {
        String xml = "<wfs>"
                + "<id>wfs</id>"
                + "<enabled>disabled</enabled>"
                + "<name>WFS</name><title>GeoServer Web Feature Service</title>"
                + "<maintainer>http://geoserver.org/comm</maintainer>"
                + "</wfs>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wfs/settings", xml,
                "text/xml");
        assertEquals(200, response.getStatusCode());
        Document dom = getAsDOM("/rest/services/wfs/settings.xml");
        assertXpathEvaluatesTo("false", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("WFS", "/wfs/name", dom);
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(405, deleteAsServletResponse("/rest/services/wfs/settings").getStatusCode());
    }
}
