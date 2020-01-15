/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class WMTSSettingsControllerTest extends CatalogRESTTestSupport {

    @After
    public void revertChanges() {
        revertService(WMTSInfo.class, null);
    }

    @Test
    public void testGetASJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/services/wmts/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmtsinfo = (JSONObject) jsonObject.get("wmts");
        assertEquals("true", wmtsinfo.get("enabled").toString().trim());
        assertEquals("WMTS", wmtsinfo.get("name"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wmts/settings.xml");
        assertEquals("wmts", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wmts/enabled", dom);
        assertXpathEvaluatesTo("WMTS", "/wmts/name", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/services/wmts/settings.html");
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json =
                "{'wmts': {'id':'wmts','enabled':'false','name':'WMTS', 'title':'New Title'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/settings/",
                        json,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod = getAsJSON(RestBaseController.ROOT_PATH + "/services/wmts/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wmtsinfo = (JSONObject) jsonObject.get("wmts");

        assertEquals("false", wmtsinfo.get("enabled").toString().trim());
        assertEquals("WMTS", wmtsinfo.get("name"));
        assertEquals("New Title", wmtsinfo.get("title"));
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml =
                "<wmts>"
                        + "<id>wmts</id>"
                        + "<enabled>false</enabled>"
                        + "<name>WMTS</name>"
                        + "<title>New Title</title>"
                        + "<maintainer>http://geoserver.org/comm</maintainer>"
                        + "</wmts>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wmts/settings.xml");
        assertXpathEvaluatesTo("false", "/wmts/enabled", dom);
        assertXpathEvaluatesTo("WMTS", "/wmts/name", dom);
        assertXpathEvaluatesTo("New Title", "/wmts/title", dom);
    }

    @Test
    public void testRoundTripJSON() throws Exception {
        JSONObject original =
                (JSONObject)
                        getAsJSON(RestBaseController.ROOT_PATH + "/services/wmts/settings.json");
        assertNotNull(original);
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/settings/",
                        original.toString(),
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON updated = getAsJSON(RestBaseController.ROOT_PATH + "/services/wmts/settings.json");
        assertEquals(original, updated);
    }

    @Test
    public void testRoundTripXML() throws Exception {
        Document original = getAsDOM(RestBaseController.ROOT_PATH + "/services/wmts/settings.xml");
        assertEquals("wmts", original.getDocumentElement().getLocalName());
        String originalString = documentToString(original);

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/settings",
                        originalString,
                        "text/xml");
        assertEquals(200, response.getStatus());
        Document updated = getAsDOM(RestBaseController.ROOT_PATH + "/services/wmts/settings.xml");
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
        WMTSInfo i = geoServer.getService(WMTSInfo.class);
        i.setEnabled(true);
        geoServer.save(i);
        String xml =
                "<wmts>"
                        + "<id>wmts</id>"
                        + "<name>WMTS</name><title>GeoServer Web Map Service</title>"
                        + "<maintainer>http://geoserver.org/comm</maintainer>"
                        + "</wmts>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wmts/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/wmts/settings.xml");
        assertXpathEvaluatesTo("true", "/wmts/enabled", dom);
        assertXpathEvaluatesTo("WMTS", "/wmts/name", dom);
        i = geoServer.getService(WMTSInfo.class);
        assertTrue(i.isEnabled());
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/services/wmts/settings")
                        .getStatus());
    }
}
