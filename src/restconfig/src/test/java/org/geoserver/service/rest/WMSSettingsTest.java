/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.service.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.wms.WMSInfo;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WMSSettingsTest extends CatalogRESTTestSupport {
    
    @After 
    public void revertChanges() {
        revertService(WMSInfo.class, null);
    }

    @Test
    public void testGetASJSON() throws Exception {
        JSON json = getAsJSON("/rest/services/wms/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");
        assertEquals("true", wmsinfo.get("enabled").toString().trim());
        assertEquals("WMS", wmsinfo.get("name"));
        JSONObject watermark = (JSONObject) wmsinfo.get("watermark");
        assertEquals("false", watermark.get("enabled").toString().trim());
        assertEquals("Nearest", wmsinfo.get("interpolation"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/services/wms/settings.xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
        assertXpathEvaluatesTo("false", "/wms/watermark/enabled", dom);
        assertXpathEvaluatesTo("Nearest", "/wms/interpolation", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json = "{'wms': {'id':'wms','enabled':'false','name':'WMS'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wms/settings/",
                json, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/services/wms/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");

        assertEquals("false", wmsinfo.get("enabled").toString().trim());
        assertEquals("WMS", wmsinfo.get("name"));
    }

    @Test
    public void testPutASXML() throws Exception {
        String xml = "<wms>"
                + "<id>wms</id>"
                + "<enabled>false</enabled>"
                + "<name>WMS</name><title>GeoServer Web Map Service</title>"
                + "<maintainer>http://jira.codehaus.org/secure/BrowseProject.jspa?id=10311</maintainer>"
                + "</wms>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wms/settings", xml,
                "text/xml");
        assertEquals(200, response.getStatusCode());

        Document dom = getAsDOM("/rest/services/wms/settings.xml");
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);

    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(405, deleteAsServletResponse("/rest/services/wms/settings").getStatusCode());
    }
    
}
