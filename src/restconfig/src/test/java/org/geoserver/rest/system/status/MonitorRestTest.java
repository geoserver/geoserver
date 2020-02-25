/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest.system.status;

import static org.junit.Assert.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.rest.RestBaseController;
import org.geoserver.system.status.MetricInfo;
import org.geoserver.system.status.MetricValue;
import org.geoserver.system.status.Metrics;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class MonitorRestTest extends GeoServerSystemTestSupport {

    @Test
    public void testDefaultCallback() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/about/system-status");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        XStream xs = new XStream(new JettisonMappedXmlDriver());
        xs.setMode(XStream.NO_REFERENCES);
        xs.alias("metric", MetricValue.class);
        xs.alias("metrics", Metrics.class);
        xs.addImplicitCollection(Metrics.class, "metrics");
        Metrics metrics = (Metrics) xs.fromXML(response.getContentAsString());
        assertTrue(metrics.getMetrics().size() >= MetricInfo.values().length);
    }

    @Test
    public void testXmlCallback() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/about/system-status.xml");
        assertEquals(200, response.getStatus());
        assertEquals("application/xml", response.getContentType());
        XStream xs = new XStream();
        xs.alias("metric", MetricValue.class);
        xs.alias("metrics", Metrics.class);
        xs.addImplicitCollection(Metrics.class, "metrics");
        Metrics metrics = (Metrics) xs.fromXML(response.getContentAsString());
        assertTrue(metrics.getMetrics().size() >= MetricInfo.values().length);
    }

    @Test
    public void testJsonCallback() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/about/system-status.json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        XStream xs = new XStream(new JettisonMappedXmlDriver());
        xs.setMode(XStream.NO_REFERENCES);
        xs.alias("metric", MetricValue.class);
        xs.alias("metrics", Metrics.class);
        xs.addImplicitCollection(Metrics.class, "metrics");
        Metrics metrics = (Metrics) xs.fromXML(response.getContentAsString());
        assertTrue(metrics.getMetrics().size() >= MetricInfo.values().length);
    }

    @Test
    public void testJsonValueFieldIsEncoded() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/about/system-status.json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        JSONArray metrics =
                (JSONArray)
                        ((JSONObject) json(response))
                                .getJSONObject("metrics")
                                .getJSONArray("metric");
        for (int i = 0; i < metrics.size(); i++) {
            assertNotNull(((JSONObject) metrics.get(i)).containsKey("value"));
        }
    }

    @Test
    public void testXmlValueFieldIsEncoded() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/about/system-status.xml");
        assertEquals(200, response.getStatus());
        assertEquals("application/xml", response.getContentType());
        Document xml =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(response.getContentAsString())));
        XPathExpression expression = XPathFactory.newInstance().newXPath().compile("//value");
        NodeList nodes = (NodeList) expression.evaluate(xml, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            assertNotNull(nodes.item(i).getFirstChild().getNodeValue());
        }
    }

    @Test
    public void testHtmlCallback() throws Exception {
        Document doc = getAsDOM(RestBaseController.ROOT_PATH + "/about/system-status.html");
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes =
                (NodeList) xpath.evaluate("/html/body/table/tr", doc, XPathConstants.NODESET);
        assertTrue(nodes.getLength() >= MetricInfo.values().length);
    }
}
