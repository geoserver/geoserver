/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.rest.RestBaseController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class OseoSettingsControllerTest extends OSEORestTestSupport {

    @Before
    public void revertChanges() {
        revertService(OSEOInfo.class, null);
    }

    @Test
    public void testGetASJSON() throws Exception {
        OSEOInfo oseo = getGeoServer().getService(OSEOInfo.class);
        oseo.getGlobalQueryables().addAll(Arrays.asList("id", "geometry"));
        getGeoServer().save(oseo);

        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/services/oseo/settings.json");
        print(json);
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject oseoinfo = (JSONObject) jsonObject.get("oseo");
        assertEquals("true", oseoinfo.get("enabled").toString().trim());
        assertEquals("OSEO", oseoinfo.get("name"));
        JSONArray queryables = oseoinfo.getJSONObject("globalQueryables").getJSONArray("string");
        assertEquals("id", queryables.get(0));
        assertEquals("geometry", queryables.get(1));
    }

    @Test
    public void testGetAsXML() throws Exception {
        OSEOInfo oseo = getGeoServer().getService(OSEOInfo.class);
        oseo.getGlobalQueryables().addAll(Arrays.asList("id", "geometry"));
        getGeoServer().save(oseo);

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/oseo/settings.xml");
        print(dom);
        assertEquals("oseo", dom.getDocumentElement().getLocalName());
        assertEquals(8, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/oseo/enabled", dom);
        assertXpathEvaluatesTo("OSEO", "/oseo/name", dom);
        assertXpathEvaluatesTo("id", "/oseo/globalQueryables/string[1]", dom);
        assertXpathEvaluatesTo("geometry", "/oseo/globalQueryables/string[2]", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/services/oseo/settings.html");
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json =
                "{'oseo': {'id':'oseo','enabled':'false','name':'OSEO','globalQueryables': { 'string': ['id','geometry']}}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/oseo/settings",
                        json,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod = getAsJSON(RestBaseController.ROOT_PATH + "/services/oseo/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject oseoinfo = (JSONObject) jsonObject.get("oseo");
        assertEquals("false", oseoinfo.get("enabled").toString().trim());
        assertEquals("OSEO", oseoinfo.get("name"));
        assertEquals(
                Arrays.asList("id", "geometry"),
                oseoinfo.getJSONObject("globalQueryables").getJSONArray("string"));
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml =
                "<oseo>"
                        + "<id>oseo</id>"
                        + "<enabled>disabled</enabled>"
                        + "<name>oseo</name><title>GeoServer OSEO Service</title>"
                        + "<maintainer>http://geoserver.org/comm</maintainer>"
                        + "</oseo>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/oseo/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/oseo/settings.xml");
        assertXpathEvaluatesTo("false", "/oseo/enabled", dom);
        assertXpathEvaluatesTo("oseo", "/oseo/name", dom);
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        GeoServer geoServer = getGeoServer();
        OSEOInfo i = geoServer.getService(OSEOInfo.class);
        i.setEnabled(true);
        String xml =
                "<oseo>"
                        + "<id>oseo</id>"
                        + "<name>oseo</name><title>GeoServer Web Feature Service</title>"
                        + "<maintainer>http://geoserver.org/comm</maintainer>"
                        + "</oseo>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/oseo/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/services/oseo/settings.xml");
        assertXpathEvaluatesTo("true", "/oseo/enabled", dom);
        assertXpathEvaluatesTo("oseo", "/oseo/name", dom);
        i = geoServer.getService(OSEOInfo.class);
        geoServer.save(i);
        assertTrue(i.isEnabled());
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/services/oseo/settings")
                        .getStatus());
    }
}
