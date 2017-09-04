package org.geoserver.status.monitoring;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.easymock.EasyMock;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.rest.DispatcherCallback;
import org.geoserver.rest.RestBaseController;
import org.geoserver.status.monitoring.collector.MetricInfo;
import org.geoserver.status.monitoring.collector.MetricValue;
import org.geoserver.status.monitoring.collector.Metrics;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

public class MonitorRestTest extends GeoServerSystemTestSupport {

    DispatcherCallback callback;

    @Before
    public void prepareCallback() throws Exception {
        callback = EasyMock.createMock(DispatcherCallback.class);
        GeoServerExtensionsHelper.init(applicationContext);
        GeoServerExtensionsHelper.singleton("testCallback", callback, DispatcherCallback.class);
    }

    @Test
    public void testXmlCallback() throws Exception {
        callback.init(anyObject(), anyObject());
        expectLastCall();
        callback.dispatched(anyObject(), anyObject(), anyObject());
        expectLastCall();
        callback.finished(anyObject(), anyObject());
        expectLastCall();
        replay(callback);

        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/about/monitoring.xml");
        assertEquals(200, response.getStatus());
        assertEquals("application/xml", response.getContentType());
        XStream xs = new XStream();
        xs.alias("metric", MetricValue.class);
        xs.alias("metrics", Metrics.class);
        xs.addImplicitCollection(Metrics.class, "metrics");
        Metrics metrics = (Metrics) xs.fromXML(response.getContentAsString());
        assertTrue(metrics.getMetrics().size() >= MetricInfo.values().length);
        verify(callback);
    }

    @Test
    public void testJsonCallback() throws Exception {
        callback.init(anyObject(), anyObject());
        expectLastCall();
        callback.dispatched(anyObject(), anyObject(), anyObject());
        expectLastCall();
        callback.finished(anyObject(), anyObject());
        expectLastCall();
        replay(callback);

        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/about/monitoring.json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        XStream xs = new XStream(new JettisonMappedXmlDriver());
        xs.setMode(XStream.NO_REFERENCES);
        xs.alias("metric", MetricValue.class);
        xs.alias("metrics", Metrics.class);
        xs.addImplicitCollection(Metrics.class, "metrics");
        Metrics metrics = (Metrics) xs.fromXML(response.getContentAsString());
        assertTrue(metrics.getMetrics().size() >= MetricInfo.values().length);
        verify(callback);
    }

    @Test
    public void testHtmlCallback() throws Exception {
        callback.init(anyObject(), anyObject());
        expectLastCall();
        callback.dispatched(anyObject(), anyObject(), anyObject());
        expectLastCall();
        callback.finished(anyObject(), anyObject());
        expectLastCall();
        replay(callback);

        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/about/monitoring.html");
        assertEquals(200, response.getStatus());
        assertEquals("text/html", response.getContentType());
        XPath xpath = XPathFactory.newInstance().newXPath();
        TagNode tagNode = new HtmlCleaner().clean(response.getContentAsString());
        Document doc = new DomSerializer(new CleanerProperties()).createDOM(tagNode);
        NodeList nodes = (NodeList) xpath.evaluate("/html/body/table/tbody/tr", doc,
                XPathConstants.NODESET);
        assertTrue(nodes.getLength() >= MetricInfo.values().length);
        verify(callback);
    }

}
