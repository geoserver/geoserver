/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.service;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Locale;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServer;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.GrowableInternationalString;
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
        JSON json = getAsJSON(ROOT_PATH + "/services/wms/settings.json");
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
        Document dom = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
        assertXpathEvaluatesTo("false", "/wms/watermark/enabled", dom);
        assertXpathEvaluatesTo("Nearest", "/wms/interpolation", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(ROOT_PATH + "/services/wms/settings.html");
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json = "{'wms': {'id':'wms','enabled':'false','name':'WMS'}}";
        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/services/wms/settings", json, "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod = getAsJSON(ROOT_PATH + "/services/wms/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");

        assertEquals("false", wmsinfo.get("enabled").toString().trim());
        assertEquals("WMS", wmsinfo.get("name"));
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml = "<wms>"
                + "<id>wms</id>"
                + "<enabled>false</enabled>"
                + "<name>WMS</name><title>GeoServer Web Map Service</title>"
                + "<maintainer>http://geoserver.org/comm</maintainer>"
                + "</wms>";
        MockHttpServletResponse response = putAsServletResponse(ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
    }

    @Test
    public void testRoundTripJSON() throws Exception {
        JSONObject original = (JSONObject) getAsJSON(ROOT_PATH + "/services/wms/settings.json");
        assertNotNull(original);
        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/services/wms/settings", original.toString(), "text/json");
        assertEquals(200, response.getStatus());
        JSON updated = getAsJSON(ROOT_PATH + "/services/wms/settings.json");
        assertEquals(original, updated);
    }

    @Test
    public void testRoundTripXML() throws Exception {
        Document original = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
        assertEquals("wms", original.getDocumentElement().getLocalName());
        String originalString = documentToString(original);

        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/services/wms/settings", originalString, "text/xml");
        assertEquals(200, response.getStatus());
        Document updated = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
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
        String xml = "<wms>"
                + "<id>wms</id>"
                + "<name>WMS</name><title>GeoServer Web Map Service</title>"
                + "<maintainer>http://geoserver.org/comm</maintainer>"
                + "</wms>";
        MockHttpServletResponse response = putAsServletResponse(ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
        i = geoServer.getService(WMSInfo.class);
        assertTrue(i.isEnabled());
    }

    @Test
    public void testUnsetBBOXForEachCRS() throws Exception {
        GeoServer geoServer = getGeoServer();
        WMSInfo i = geoServer.getService(WMSInfo.class);
        i.setBBOXForEachCRS(true);
        assertNotNull(i.getWatermark());
        geoServer.save(i);

        String xml =
                """
            <wms>
            <id>wms</id>
            <bboxForEachCRS xsi:nil="true"/>
            </wms>
            """;
        MockHttpServletResponse response = putAsServletResponse(ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        // the Java API returns always non null, but if nillified the XML representation will miss the property
        Document dom = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
        assertXpathNotExists("wms/bboxForEachCRS", dom);
    }

    /**
     * Testing nested object removal in REST API (watermark is not really meant to be forced to null, but it's a good
     * test candidate)
     */
    @Test
    public void testRemoveWatermark() throws Exception {
        GeoServer geoServer = getGeoServer();
        WMSInfo i = geoServer.getService(WMSInfo.class);
        i.setEnabled(true);
        assertNotNull(i.getWatermark());
        geoServer.save(i);
        String xml =
                """
            <wms>
            <id>wms</id>
            <watermark xsi:nil="true"/>
            </wms>
            """;
        MockHttpServletResponse response = putAsServletResponse(ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        i = geoServer.getService(WMSInfo.class);
        assertTrue(i.isEnabled());
        assertNull(i.getWatermark());
    }

    /**
     * Follows up {@link WMSSettingsControllerTest#testRemoveWatermark()}, testing that the nil marker beats any content
     * in the XML
     */
    @Test
    public void testNilMarkerBeatsContent() throws Exception {
        GeoServer geoServer = getGeoServer();
        WMSInfo i = geoServer.getService(WMSInfo.class);
        i.setEnabled(true);
        assertNotNull(i.getWatermark());
        geoServer.save(i);
        String xml =
                """
            <wms>
            <id>wms</id>
            <watermark xsi:nil="true">
              <enabled>true</enabled>
              <url>https://example.com/watermark.png</url>
            </watermark>
            </wms>
            """;
        MockHttpServletResponse response = putAsServletResponse(ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        i = geoServer.getService(WMSInfo.class);
        assertTrue(i.isEnabled());
        assertNull(i.getWatermark());
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(ROOT_PATH + "/services/wms/settings").getStatus());
    }

    @Test
    public void testDisableDefaultStyleOption() throws Exception {
        String xml = "<wms>"
                + "<id>wms</id>"
                + "<enabled>true</enabled>"
                + "<name>WMS</name><title>GeoServer Web Map Service</title>"
                + "<maintainer>http://geoserver.org/comm</maintainer>"
                + "</wms>";
        MockHttpServletResponse response = putAsServletResponse(ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM(ROOT_PATH + "/services/wms/settings.xml"); // default should be true
        assertXpathEvaluatesTo("true", "/wms/defaultGroupStyleEnabled", dom);
        String xml2 = "<wms>"
                + "<id>wms</id>"
                + "<enabled>true</enabled>"
                + "<name>WMS</name><title>GeoServer Web Map Service</title>"
                + "<maintainer>http://geoserver.org/comm</maintainer>"
                + "<defaultGroupStyleEnabled>false</defaultGroupStyleEnabled>"
                + "</wms>";
        response = putAsServletResponse(ROOT_PATH + "/services/wms/settings", xml2, "text/xml");
        assertEquals(200, response.getStatus());

        dom = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
        // updated to false
        assertXpathEvaluatesTo("false", "/wms/defaultGroupStyleEnabled", dom);
    }

    @Test
    public void testGetAsXMLInternationalRootLayer() throws Exception {
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        GrowableInternationalString growableInternationalString = new GrowableInternationalString();
        growableInternationalString.add(Locale.ENGLISH, "i18n english root layer title");
        wmsInfo.setInternationalRootLayerTitle(growableInternationalString);
        getGeoServer().save(wmsInfo);
        Document dom = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("i18n english root layer title", "/wms/internationalRootLayerTitle/en", dom);
    }

    @Test
    public void testPutRootLayerAbstract() throws Exception {
        String xml = "<wms><id>wms</id><enabled>false</enabled><name>WMS</name><title>GeoServer Web Map"
                + " Service</title><internationalRootLayerAbstract><en>en abstract</en><it>it"
                + " abstract</it></internationalRootLayerAbstract><maintainer>http://geoserver.org/comm</maintainer>"
                + "</wms>";
        MockHttpServletResponse response = putAsServletResponse(ROOT_PATH + "/services/wms/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(ROOT_PATH + "/services/wms/settings.xml");
        assertXpathEvaluatesTo("en abstract", "/wms/internationalRootLayerAbstract/en", dom);
        assertXpathEvaluatesTo("it abstract", "/wms/internationalRootLayerAbstract/it", dom);
    }
}
