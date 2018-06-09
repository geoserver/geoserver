/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServer;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class WMSSettingsControllerTest extends CatalogRESTTestSupport {

    @After
    public void revertChanges() {
        revertService(WMSInfo.class, null);
    }

    @Test
    public void testGetASJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/services/wms/settings.json");
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
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/settings.xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
        assertXpathEvaluatesTo("false", "/wms/watermark/enabled", dom);
        assertXpathEvaluatesTo("Nearest", "/wms/interpolation", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/settings.html");
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json = "{'wms': {'id':'wms','enabled':'false','name':'WMS'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/settings/",
                        json,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod = getAsJSON(RestBaseController.ROOT_PATH + "/services/wms/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");

        assertEquals("false", wmsinfo.get("enabled").toString().trim());
        assertEquals("WMS", wmsinfo.get("name"));
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml =
                "<wms>"
                        + "<id>wms</id>"
                        + "<enabled>false</enabled>"
                        + "<name>WMS</name><title>GeoServer Web Map Service</title>"
                        + "<maintainer>http://geoserver.org/comm</maintainer>"
                        + "</wms>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/settings.xml");
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
    }

    @Test
    public void testRoundTripJSON() throws Exception {
        JSONObject original =
                (JSONObject)
                        getAsJSON(RestBaseController.ROOT_PATH + "/services/wms/settings.json");
        assertNotNull(original);
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/settings/",
                        original.toString(),
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON updated = getAsJSON(RestBaseController.ROOT_PATH + "/services/wms/settings.json");
        assertEquals(original, updated);
    }

    @Test
    public void testRoundTripXML() throws Exception {
        Document original = getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/settings.xml");
        assertEquals("wms", original.getDocumentElement().getLocalName());
        String originalString = documentToString(original);

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/settings",
                        originalString,
                        "text/xml");
        assertEquals(200, response.getStatus());
        Document updated = getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/settings.xml");
        assertEquals(originalString, documentToString(updated));
    }

    private String documentToString(Document doc) throws Exception {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        return writer.toString();
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        GeoServer geoServer = getGeoServer();
        WMSInfo i = geoServer.getService(WMSInfo.class);
        i.setEnabled(true);
        geoServer.save(i);
        String xml =
                "<wms>"
                        + "<id>wms</id>"
                        + "<name>WMS</name><title>GeoServer Web Map Service</title>"
                        + "<maintainer>http://geoserver.org/comm</maintainer>"
                        + "</wms>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/settings.xml");
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
        i = geoServer.getService(WMSInfo.class);
        assertTrue(i.isEnabled());
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/services/wms/settings")
                        .getStatus());
    }
}
