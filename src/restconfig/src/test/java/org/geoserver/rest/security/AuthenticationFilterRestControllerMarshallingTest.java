/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class AuthenticationFilterRestControllerMarshallingTest extends GeoServerSystemTestSupport {
    private static XpathEngine xp;
    private static final String BASEPATH = RestBaseController.ROOT_PATH;

    @BeforeClass
    public static void init() throws Exception {
        xp = XMLUnit.newXpathEngine();
        xp.setNamespaceContext(new SimpleNamespaceContext(Map.of("atom", "http://www.w3.org/2005/Atom")));
    }

    @Before
    public void setUp() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @After
    public void after() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void testList_XML() throws Exception {
        Document dom = getAsDOM(BASEPATH + "/security/authFilters.xml", 200);
        // Extract all <name> values
        NodeList nameNodes = xp.getMatchingNodes("//authFilter/name", dom);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nameNodes.getLength(); i++) {
            names.add(nameNodes.item(i).getTextContent());
        }

        names.forEach(name -> {
            String xpath = String.format("//authFilter[name='%s']/atom:link", name);
            try {
                NodeList link = xp.getMatchingNodes(xpath, dom);
                assertEquals(1, link.getLength());
                String href = link.item(0).getAttributes().getNamedItem("href").getTextContent();
                assertTrue(href.endsWith("/security/authFilters/" + name + ".xml"));
            } catch (XpathException e) {
                fail("Xpath evaluation failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testList_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "/security/authFilters");
        TestCase.assertEquals(403, response.getStatus());
    }

    @Test
    public void testList_JSON() throws Exception {
        JSON json = getAsJSON(BASEPATH + "/security/authFilters.json", 200);
        JSONArray authFilters = ((JSONObject) json).getJSONObject("authFilters").getJSONArray("authFilter");

        for (Object jsonObject : authFilters) {
            JSONObject filterChain = (JSONObject) jsonObject;
            String name = filterChain.getString("name");
            String href = filterChain.getString("href");
            assertTrue(href.endsWith("/security/authFilters/" + name + ".json"));
        }
    }

    private static final String testViewXML = "<authFilter>\n" + "  <name>viewXml</name>\n"
            + "  <config class=\"org.geoserver.security.config.SecurityInterceptorFilterConfig\">\n"
            + "    <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>\n"
            + "    <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>\n"
            + "    <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>\n"
            + "  </config>\n"
            + "</authFilter>\n";

    // This only checks the values in web it does not check all possible fields
    @Test
    public void testView_XML() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authFilters", testViewXML, "application/xml");
        TestCase.assertEquals(201, response.getStatus());

        Document document = getAsDOM(BASEPATH + "/security/authFilters/viewXml.xml", 200);
        assertXpathEvaluatesTo("viewXml", "/authFilter/name", document);
        assertXpathExists("/authFilter/config", document);
    }

    @Test
    public void testView_Unauthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "/security/authFilters", "application/xml");
        assertEquals(403, response.getStatus());
    }

    private static final String testViewJSON = "{\n" + "    \"authFilter\": {\n"
            + "        \"name\": \"viewJson\",\n"
            + "        \"config\": {\n"
            + "            \"@class\": \"org.geoserver.security.config.SecurityInterceptorFilterConfig\",\n"
            + "            \"className\": \"org.geoserver.security.filter.GeoServerSecurityInterceptorFilter\",\n"
            + "            \"allowIfAllAbstainDecisions\": false,\n"
            + "            \"securityMetadataSource\": \"restFilterDefinitionMap\"\n"
            + "        }\n"
            + "    }\n"
            + "}";

    @Test
    public void testView_JSON() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authFilters", testViewJSON, "application/json");
        TestCase.assertEquals(201, response.getStatus());

        JSON json = getAsJSON(BASEPATH + "/security/authFilters/viewJson.json", 200);
        JSONObject jsonObject = ((JSONObject) json).getJSONObject("authFilter");
        assertEquals("viewJson", jsonObject.getString("name"));
        assertNotNull("false", jsonObject.getJSONObject("config"));
    }

    private static final String testPostXML = "<authFilter>\n" + "  <name>postXml</name>\n"
            + "  <config class=\"org.geoserver.security.config.SecurityInterceptorFilterConfig\">\n"
            + "    <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>\n"
            + "    <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>\n"
            + "    <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>\n"
            + "  </config>\n"
            + "</authFilter>\n";

    @Test
    public void testPost_XML() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authFilters", testPostXML, "application/xml");
        TestCase.assertEquals(201, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String location = response.getHeader("Location");
        assertNotNull(location);
        assertTrue(location.endsWith("/security/authFilters/postXml"));

        Document viewDocument = getAsDOM(BASEPATH + "/security/authFilters/postXml.xml", 200);
        assertXpathEvaluatesTo("postXml", "/authFilter/name", viewDocument);
        assertXpathExists("/authFilter/config", viewDocument);
    }

    private static final String testPostJSON = "{\n" + "    \"authFilter\": {\n"
            + "        \"name\": \"postJson\",\n"
            + "        \"config\": {\n"
            + "            \"@class\": \"org.geoserver.security.config.SecurityInterceptorFilterConfig\",\n"
            + "            \"className\": \"org.geoserver.security.filter.GeoServerSecurityInterceptorFilter\",\n"
            + "            \"allowIfAllAbstainDecisions\": false,\n"
            + "            \"securityMetadataSource\": \"restFilterDefinitionMap\"\n"
            + "        }\n"
            + "    }\n"
            + "}";

    @Test
    public void testPost_JSON() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authFilters", testPostJSON, "application/json");
        TestCase.assertEquals(201, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String location = response.getHeader("Location");
        assertNotNull(location);
        assertTrue(location.endsWith("/security/authFilters/postJson"));

        Document viewDocument = getAsDOM(BASEPATH + "/security/authFilters/postJson.xml", 200);
        assertXpathEvaluatesTo("postJson", "/authFilter/name", viewDocument);
        assertXpathExists("/authFilter/config", viewDocument);
    }

    private static final String testSetupPutXml = "<authFilter>\n" + "    <name>putXml</name>\n"
            + "    <config class=\"org.geoserver.security.config.SecurityInterceptorFilterConfig\">\n"
            + "        <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>\n"
            + "        <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>\n"
            + "        <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>\n"
            + "    </config>\n"
            + "</authFilter>";

    private static final String testPutXml = "<authFilter>\n" + "    <name>putXml</name>\n"
            + "    <config class=\"org.geoserver.security.config.SecurityInterceptorFilterConfig\">\n"
            + "        <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>\n"
            + "        <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>\n"
            + "        <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>\n"
            + "    </config>\n"
            + "</authFilter>";

    @Test
    public void testPut_XML() throws Exception {
        MockHttpServletResponse setupResponse =
                postAsServletResponse(BASEPATH + "/security/authFilters", testSetupPutXml, "application/xml");
        TestCase.assertEquals(201, setupResponse.getStatus());

        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authFilters/putXml.xml", testPutXml, "application/xml");
        TestCase.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPut_JSON() throws Exception {
        String json = getAsString(RestBaseController.ROOT_PATH + "/security/authFilters/restInterceptor.json");
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authFilters/restInterceptor", json, "application/json");
        TestCase.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPut_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authFilters/putXml", testPutXml, "application/xml");
        TestCase.assertEquals(403, response.getStatus());
    }

    private static final String testSetupDeleteXml = "<authFilter>\n" + "    <name>deleteXml</name>\n"
            + "    <config class=\"org.geoserver.security.config.SecurityInterceptorFilterConfig\">\n"
            + "        <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>\n"
            + "        <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>\n"
            + "        <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>\n"
            + "    </config>\n"
            + "</authFilter>";

    @Test
    public void testDelete() throws Exception {
        MockHttpServletResponse restoreWeb =
                postAsServletResponse(BASEPATH + "/security/authFilters", testSetupDeleteXml, "application/xml");
        TestCase.assertEquals(201, restoreWeb.getStatus());

        MockHttpServletResponse response = deleteAsServletResponse(BASEPATH + "/security/authFilters/deleteXml");
        TestCase.assertEquals(200, response.getStatus());

        MockHttpServletResponse viewResponse = getAsServletResponse(BASEPATH + "/security/authFilters/deleteXml.xml");
        TestCase.assertEquals(404, viewResponse.getStatus());
    }

    @Test
    public void testDelete_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = deleteAsServletResponse(BASEPATH + "/security/authFilters/restInterceptor");
        TestCase.assertEquals(403, response.getStatus());
    }

    private void notAuthorised() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
