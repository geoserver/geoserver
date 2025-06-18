/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static java.lang.String.format;
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

public class AuthenticationProviderRestControllerMarshallingTest extends GeoServerSystemTestSupport {
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
        Document dom = getAsDOM(BASEPATH + "/security/authProviders.xml", 200);
        // Extract all <name> values
        NodeList nameNodes = xp.getMatchingNodes("//authProvider/name", dom);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nameNodes.getLength(); i++) {
            names.add(nameNodes.item(i).getTextContent());
        }

        names.forEach(name -> {
            String xpath = format("//authProvider[name='%s']/atom:link", name);
            try {
                NodeList link = xp.getMatchingNodes(xpath, dom);
                assertEquals(1, link.getLength());
                String href = link.item(0).getAttributes().getNamedItem("href").getTextContent();
                assertTrue(href.endsWith("/security/authProviders/" + name + ".xml"));
            } catch (XpathException e) {
                fail("Xpath evaluation failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testList_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "/security/authProviders");
        TestCase.assertEquals(403, response.getStatus());
    }

    @Test
    public void testList_JSON() throws Exception {
        JSON json = getAsJSON(BASEPATH + "/security/authProviders.json", 200);
        JSONArray authProviders = ((JSONObject) json).getJSONObject("authProviders").getJSONArray("authProvider");

        for (Object jsonObject : authProviders) {
            JSONObject filterChain = (JSONObject) jsonObject;
            String name = filterChain.getString("name");
            String href = filterChain.getString("href");
            assertTrue(href.endsWith("/security/authProviders/" + name + ".json"));
        }
    }

    private static final String testViewXML = 
            "<authProvider>\n" +
            "    <name>viewXml</name>\n" +
            "    <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>\n" +
            "    <userGroupServiceName>default</userGroupServiceName>\n" +
            "    <position>1</position>\n" +
            "    <config class=\"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\">\n" +
            "        <userGroupServiceName>default</userGroupServiceName>\n" +
            "    </config>\n" +
            "    <disabled>false</disabled>\n" +
            "</authProvider>";

    // This only checks the values in web it does not check all possible fields
    @Test
    public void testView_XML() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authProviders", testViewXML, "application/xml");
        TestCase.assertEquals(201, response.getStatus());

        Document document = getAsDOM(BASEPATH + "/security/authProviders/viewXml.xml", 200);
        assertXpathEvaluatesTo("viewXml", "/authProvider/name", document);
        assertXpathEvaluatesTo(
                "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider", "/authProvider/className", document);
        assertXpathExists("/authProvider/id", document);
        assertXpathExists("/authProvider/config", document);
    }

    @Test
    public void testView_Unauthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "/security/authProviders", "application/xml");
        assertEquals(403, response.getStatus());
    }

    private static final String testViewJSON = "{\n" +
            "    \"authProvider\": {\n" +
            "        \"name\": \"viewJson\",\n" +
            "        \"className\": \"org.geoserver.security.auth.UsernamePasswordAuthenticationProvider\",\n" +
            "        \"userGroupServiceName\": \"default\",\n" +
            "        \"position\": 1,\n" +
            "        \"config\": {\n" +
            "            \"@class\": \"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\",\n" +
            "            \"userGroupServiceName\": \"default\"\n" +
            "        },\n" +
            "        \"disabled\": false\n" +
            "    }\n" +
            "}";

    @Test
    public void testView_JSON() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authProviders", testViewJSON, "application/json");
        TestCase.assertEquals(201, response.getStatus());

        JSON json = getAsJSON(BASEPATH + "/security/authProviders/viewJson.json", 200);
        JSONObject jsonObject = ((JSONObject) json).getJSONObject("authProvider");

        assertEquals("viewJson", jsonObject.getString("name"));
        assertEquals(
                "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider", jsonObject.getString("className"));
        assertNotNull("false", jsonObject.getString("id"));
        assertNotNull("false", jsonObject.getJSONObject("config"));
    }

    private static final String testPostXML =
            "<authProvider>\n" +
            "    <name>postXml</name>\n" +
            "    <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>\n" +
            "    <userGroupServiceName>default</userGroupServiceName>\n" +
            "    <position>1</position>\n" +
            "    <config class=\"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\">\n" +
            "        <userGroupServiceName>default</userGroupServiceName>\n" +
            "    </config>\n" +
            "    <disabled>false</disabled>\n" +
            "</authProvider>";

    @Test
    public void testPost_XML() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authProviders", testPostXML, "application/xml");
        TestCase.assertEquals(201, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String location = response.getHeader("Location");
        assertNotNull(location);
        assertTrue(location.endsWith("/security/authProviders/postXml"));

        Document viewDocument = getAsDOM(BASEPATH + "/security/authProviders/postXml.xml", 200);
        assertXpathEvaluatesTo("postXml", "/authProvider/name", viewDocument);
        assertXpathEvaluatesTo(
                "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider", "/authProvider/className", viewDocument);
        assertXpathExists("/authProvider/id", viewDocument);
        assertXpathExists("/authProvider/config", viewDocument);
    }

    private static final String testPostJSON =
            "{\n" +
            "    \"authProvider\": {\n" +
            "        \"name\": \"postJson\",\n" +
            "        \"className\": \"org.geoserver.security.auth.UsernamePasswordAuthenticationProvider\",\n" +
            "        \"userGroupServiceName\": \"default\",\n" +
            "        \"position\": 1,\n" +
            "        \"config\": {\n" +
            "            \"@class\": \"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\",\n" +
            "            \"userGroupServiceName\": \"default\"\n" +
            "        },\n" +
            "        \"disabled\": false\n" +
            "    }\n" +
            "}";

    @Test
    public void testPost_JSON() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authProviders", testPostJSON, "application/json");
        TestCase.assertEquals(201, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String location = response.getHeader("Location");
        assertNotNull(location);
        assertTrue(location.endsWith("/security/authProviders/postJson"));

        Document viewDocument = getAsDOM(BASEPATH + "/security/authProviders/postJson.xml", 200);
        assertXpathEvaluatesTo("postJson", "/authProvider/name", viewDocument);
        assertXpathEvaluatesTo(
                "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider", "/authProvider/className", viewDocument);
        assertXpathExists("/authProvider/id", viewDocument);
        assertXpathExists("/authProvider/config", viewDocument);
    }

    private static final String testSetupPutXml =
            "<authProvider>\n" +
            "    <name>putXml</name>\n" +
            "    <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>\n" +
            "    <userGroupServiceName>default</userGroupServiceName>\n" +
            "    <position>1</position>\n" +
            "    <config class=\"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\">\n" +
            "        <userGroupServiceName>default</userGroupServiceName>\n" +
            "    </config>\n" +
            "    <disabled>false</disabled>\n" +
            "</authProvider>";

    private static final String testPutXml = "<authProvider>\n" +
            "    <id>%s</id>\n" +
            "    <name>putXml</name>\n" +
            "    <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>\n" +
            "    <userGroupServiceName>default</userGroupServiceName>\n" +
            "    <position>1</position>\n" +
            "    <config class=\"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\">\n" +
            "        <userGroupServiceName>default</userGroupServiceName>\n" +
            "    </config>\n" +
            "    <disabled>false</disabled>\n" +
            "</authProvider>";

    @Test
    public void testPut_XML() throws Exception {
        MockHttpServletResponse setupResponse =
                postAsServletResponse(BASEPATH + "/security/authProviders", testSetupPutXml, "application/xml");
        TestCase.assertEquals(201, setupResponse.getStatus());

        Document viewDocument = getAsDOM(BASEPATH + "/security/authProviders/putXml.xml", 200);
        String id = xp.evaluate("/authProvider/id", viewDocument);

        String testPutXmlWithId = format(testPutXml, id);
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authProviders/putXml.xml", testPutXmlWithId, "application/xml");
        TestCase.assertEquals(200, response.getStatus());
    }

    private static final String testSetupPutJson =
            "{\n" +
            "    \"authProvider\": {\n" +
            "        \"name\": \"putJson\",\n" +
            "        \"className\": \"org.geoserver.security.auth.UsernamePasswordAuthenticationProvider\",\n" +
            "        \"userGroupServiceName\": \"default\",\n" +
            "        \"position\": 1,\n" +
            "        \"config\": {\n" +
            "            \"@class\": \"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\",\n" +
            "            \"userGroupServiceName\": \"default\"\n" +
            "        },\n" +
            "        \"disabled\": false\n" +
            "    }\n" +
            "}";

    private static final String testPutJson = "{\n" +
            "    \"authProvider\": {\n" +
            "        \"id\": \"%s\",\n" +
            "        \"name\": \"putJson\",\n" +
            "        \"className\": \"org.geoserver.security.auth.UsernamePasswordAuthenticationProvider\",\n" +
            "        \"userGroupServiceName\": \"default\",\n" +
            "        \"position\": 1,\n" +
            "        \"config\": {\n" +
            "            \"@class\": \"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\",\n" +
            "            \"userGroupServiceName\": \"default\"\n" +
            "        },\n" +
            "        \"disabled\": false\n" +
            "    }\n" +
            "}";

    @Test
    public void testPut_JSON() throws Exception {
        MockHttpServletResponse setupResponse =
                postAsServletResponse(BASEPATH + "/security/authProviders", testSetupPutJson, "application/json");
        TestCase.assertEquals(201, setupResponse.getStatus());

        Document viewDocument = getAsDOM(BASEPATH + "/security/authProviders/putJson.xml", 200);
        String id = xp.evaluate("/authProvider/id", viewDocument);

        String testPutJsonWithId = format(testPutJson, id);
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authProviders/putJson", testPutJsonWithId, "application/json");
        TestCase.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPut_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authProviders/putXml", testPutXml, "application/xml");
        TestCase.assertEquals(403, response.getStatus());
    }

    private static final String testSetupDeleteXml =
            "<authProvider>\n" +
            "    <name>deleteXml</name>\n" +
            "    <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>\n" +
            "    <userGroupServiceName>default</userGroupServiceName>\n" +
            "    <position>1</position>\n" +
            "    <config class=\"org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig\">\n" +
            "        <userGroupServiceName>default</userGroupServiceName>\n" +
            "    </config>\n" +
            "    <disabled>false</disabled>\n" +
            "</authProvider>";

    @Test
    public void testDelete() throws Exception {
        MockHttpServletResponse restoreWeb =
                postAsServletResponse(BASEPATH + "/security/authProviders", testSetupDeleteXml, "application/xml");
        TestCase.assertEquals(201, restoreWeb.getStatus());

        MockHttpServletResponse response = deleteAsServletResponse(BASEPATH + "/security/authProviders/deleteXml");
        TestCase.assertEquals(200, response.getStatus());

        MockHttpServletResponse viewResponse = getAsServletResponse(BASEPATH + "/security/authProviders/deleteXml.xml");
        TestCase.assertEquals(404, viewResponse.getStatus());
    }

    @Test
    public void testDelete_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = deleteAsServletResponse(BASEPATH + "/security/authProviders/restInterceptor");
        TestCase.assertEquals(403, response.getStatus());
    }

    private void notAuthorised() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}

