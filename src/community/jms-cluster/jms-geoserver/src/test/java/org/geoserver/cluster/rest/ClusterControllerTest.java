/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ClusterControllerTest extends GeoServerSystemTestSupport {

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testGetConfigurationXML() throws Exception {
        Document dom = getAsDOM("rest/cluster.xml");
        // print(dom);
        // checking a property that's unlikely to change
        assertXpathEvaluatesTo(
                "VirtualTopic.geoserver", "/properties/property[@name='topicName']/@value", dom);
    }

    @Test
    public void testGetConfigurationHTML() throws Exception {
        Document dom = getAsDOM("rest/cluster.html");
        assertEquals("html", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testGetConfigurationJSON() throws Exception {
        // get JSON properties
        JSON json = getAsJSON("rest/cluster.json");
        assertThat(json, notNullValue());
        assertThat(json, instanceOf(JSONObject.class));
        JSONObject jsonObject = (JSONObject) json;
        assertThat(jsonObject.get("properties"), notNullValue());
        assertThat(jsonObject.get("properties"), instanceOf(JSONObject.class));
        assertThat(jsonObject.getJSONObject("properties").get("property"), notNullValue());
        assertThat(
                jsonObject.getJSONObject("properties").get("property"),
                instanceOf(JSONArray.class));
        JSONArray properties = jsonObject.getJSONObject("properties").getJSONArray("property");
        assertThat(properties.size(), is(15));
        // check properties exist
        checkPropertyExists(properties, "toggleSlave");
        checkPropertyExists(properties, "connection");
        checkPropertyExists(properties, "topicName");
        checkPropertyExists(properties, "brokerURL");
        checkPropertyExists(properties, "durable");
        checkPropertyExists(properties, "xbeanURL");
        checkPropertyExists(properties, "toggleMaster");
        checkPropertyExists(properties, "embeddedBroker");
        checkPropertyExists(properties, "CLUSTER_CONFIG_DIR");
        checkPropertyExists(properties, "embeddedBrokerProperties");
        checkPropertyExists(properties, "connection.retry");
        checkPropertyExists(properties, "readOnly");
        checkPropertyExists(properties, "instanceName");
        checkPropertyExists(properties, "group");
        checkPropertyExists(properties, "connection.maxwait");
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

    /** Helper method that checks if a property exists. */
    private void checkPropertyExists(JSONArray properties, String expectedName) {
        boolean found = false;
        for (Object json : properties) {
            assertThat(json, instanceOf(JSONObject.class));
            JSONObject jsonObject = (JSONObject) json;
            assertThat(jsonObject.get("@name"), notNullValue());
            if (jsonObject.get("@name").equals(expectedName)) {
                assertThat(jsonObject.get("@value"), notNullValue());
                found = true;
            }
        }
        assertThat(found, is(true));
    }
}
