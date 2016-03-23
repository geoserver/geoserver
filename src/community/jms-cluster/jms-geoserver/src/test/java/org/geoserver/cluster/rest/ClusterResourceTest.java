package org.geoserver.cluster.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ClusterResourceTest extends GeoServerSystemTestSupport {
    
    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }
    
    @Test
    public void testGetConfiguration() throws Exception {
        Document dom = getAsDOM("rest/cluster.xml");
        // print(dom);
        // checking a property that's unlikely to change
        assertXpathEvaluatesTo("VirtualTopic.>", "/properties/property[@name='topicName']/@value", dom);
    }
    
    @Test
    public void testUpdateConfiguration() throws Exception {
        String config = "<properties><property name=\"toggleSlave\" value=\"false\"/></properties>";
        MockHttpServletResponse response = postAsServletResponse("rest/cluster.xml", config);
        assertEquals(201, response.getStatus());
        Document dom = getAsDOM("rest/cluster.xml");
        // print(dom);
        // checking the property just modified
        assertXpathEvaluatesTo("false", "/properties/property[@name='toggleSlave']/@value", dom);
    }

}
