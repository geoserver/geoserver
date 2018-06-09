/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServer;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class WFSSettingsControllerTest extends CatalogRESTTestSupport {

    @Before
    public void revertChanges() {
        revertService(WFSInfo.class, null);
    }

    @Test
    public void testGetASJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/services/wfs/settings.json");
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
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/settings.xml");
        assertEquals("wfs", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("WFS", "/wfs/name", dom);
        assertXpathEvaluatesTo("COMPLETE", "/wfs/serviceLevel", dom);
        assertXpathEvaluatesTo("1000000", "/wfs/maxFeatures", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/settings.html");
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json = "{'wfs': {'id':'wfs','enabled':'false','name':'WFS'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/settings/",
                        json,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod = getAsJSON(RestBaseController.ROOT_PATH + "/services/wfs/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wfsinfo = (JSONObject) jsonObject.get("wfs");
        assertEquals("false", wfsinfo.get("enabled").toString().trim());
        assertEquals("WFS", wfsinfo.get("name"));
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml =
                "<wfs>"
                        + "<id>wfs</id>"
                        + "<enabled>disabled</enabled>"
                        + "<name>WFS</name><title>GeoServer Web Feature Service</title>"
                        + "<maintainer>http://geoserver.org/comm</maintainer>"
                        + "</wfs>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/settings.xml");
        assertXpathEvaluatesTo("false", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("WFS", "/wfs/name", dom);
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        GeoServer geoServer = getGeoServer();
        WFSInfo i = geoServer.getService(WFSInfo.class);
        i.setEnabled(true);
        String xml =
                "<wfs>"
                        + "<id>wfs</id>"
                        + "<name>WFS</name><title>GeoServer Web Feature Service</title>"
                        + "<maintainer>http://geoserver.org/comm</maintainer>"
                        + "</wfs>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/settings.xml");
        assertXpathEvaluatesTo("true", "/wfs/enabled", dom);
        assertXpathEvaluatesTo("WFS", "/wfs/name", dom);
        i = geoServer.getService(WFSInfo.class);
        geoServer.save(i);
        assertTrue(i.isEnabled());
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/services/wfs/settings")
                        .getStatus());
    }
}
