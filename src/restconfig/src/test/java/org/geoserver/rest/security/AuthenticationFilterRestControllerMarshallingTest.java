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
        Document dom = getAsDOM(BASEPATH + "/security/authfilters.xml", 200);
        // Extract all <name> values
        NodeList nameNodes = xp.getMatchingNodes("//authfilter/name", dom);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nameNodes.getLength(); i++) {
            names.add(nameNodes.item(i).getTextContent());
        }

        names.forEach(name -> {
            String xpath = "//authfilter[name='%s']/atom:link".formatted(name);
            try {
                NodeList link = xp.getMatchingNodes(xpath, dom);
                assertEquals(1, link.getLength());
                String href = link.item(0).getAttributes().getNamedItem("href").getTextContent();
                assertTrue(href.endsWith("/security/authfilters/" + name + ".xml"));
            } catch (XpathException e) {
                fail("Xpath evaluation failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testList_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "/security/authfilters");
        TestCase.assertEquals(403, response.getStatus());
    }

    @Test
    public void testList_JSON() throws Exception {
        JSON json = getAsJSON(BASEPATH + "/security/authfilters.json", 200);
        JSONArray authfilters = ((JSONObject) json).getJSONObject("authfilters").getJSONArray("authfilter");

        for (Object jsonObject : authfilters) {
            JSONObject filterchain = (JSONObject) jsonObject;
            String name = filterchain.getString("name");
            String href = filterchain.getString("href");
            assertTrue(href.endsWith("/security/authfilters/" + name + ".json"));
        }
    }

    private static final String testViewXML =
            """
            <org.geoserver.security.config.SecurityInterceptorFilterConfig>
              <name>viewXml</name>
                <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
                <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
                <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
            </org.geoserver.security.config.SecurityInterceptorFilterConfig>
            """;

    // This only checks the values in web it does not check all possible fields
    @Test
    public void testView_XML() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authfilters", testViewXML, "application/xml");
        TestCase.assertEquals(200, response.getStatus());

        Document document = getAsDOM(BASEPATH + "/security/authfilters/viewXml.xml", 200);
        assertXpathEvaluatesTo(
                "viewXml", "/org.geoserver.security.config.SecurityInterceptorFilterConfig/name", document);
        assertXpathExists("/org.geoserver.security.config.SecurityInterceptorFilterConfig/id", document);
    }

    @Test
    public void testView_Unauthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "/security/authfilters", "application/xml");
        assertEquals(403, response.getStatus());
    }

    private static final String testViewJSON =
            """
            {
                "org.geoserver.security.config.SecurityInterceptorFilterConfig": {
                    "name": "viewJson",
                        "className": "org.geoserver.security.filter.GeoServerSecurityInterceptorFilter",
                        "allowIfAllAbstainDecisions": false,
                        "securityMetadataSource": "restFilterDefinitionMap"
                }
            }\
            """;

    @Test
    public void testView_JSON() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authfilters.json", testViewJSON, "application/json");
        TestCase.assertEquals(200, response.getStatus());

        JSON json = getAsJSON(BASEPATH + "/security/authfilters/viewJson.json", 200);
        JSONObject jsonObject =
                ((JSONObject) json).getJSONObject("org.geoserver.security.config.SecurityInterceptorFilterConfig");
        assertEquals("viewJson", jsonObject.getString("name"));
        assertNotNull("false", jsonObject.getJSONObject("config"));
    }

    private static final String testPostXML =
            """
            <org.geoserver.security.config.SecurityInterceptorFilterConfig>
              <name>postXml</name>
                <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
                <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
                <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
            </org.geoserver.security.config.SecurityInterceptorFilterConfig>
            """;

    @Test
    public void testPost_XML() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authfilters", testPostXML, "application/xml");
        TestCase.assertEquals(200, response.getStatus());
        assertEquals("application/xml", response.getContentType());
        Document viewDocument = getAsDOM(BASEPATH + "/security/authfilters/postXml.xml", 200);
        assertXpathEvaluatesTo(
                "postXml", "/org.geoserver.security.config.SecurityInterceptorFilterConfig/name", viewDocument);
        assertXpathExists("/org.geoserver.security.config.SecurityInterceptorFilterConfig/id", viewDocument);
    }

    private static final String testPostJSON =
            """
            {
                "org.geoserver.security.config.SecurityInterceptorFilterConfig": {
                    "name": "postJson",
                        "className": "org.geoserver.security.filter.GeoServerSecurityInterceptorFilter",
                        "allowIfAllAbstainDecisions": false,
                        "securityMetadataSource": "restFilterDefinitionMap"
                }
            }\
            """;

    @Test
    public void testPost_JSON() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/authfilters.json", testPostJSON, "application/json");
        TestCase.assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        Document viewDocument = getAsDOM(BASEPATH + "/security/authfilters/postJson.xml", 200);
        assertXpathEvaluatesTo(
                "postJson", "/org.geoserver.security.config.SecurityInterceptorFilterConfig/name", viewDocument);
        assertXpathExists("/org.geoserver.security.config.SecurityInterceptorFilterConfig/id", viewDocument);
    }

    private static final String testSetupPutXml =
            """
            <org.geoserver.security.config.SecurityInterceptorFilterConfig>
                <name>putXml</name>
                    <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
                    <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
                    <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
            </org.geoserver.security.config.SecurityInterceptorFilterConfig>""";

    private static final String testPutXml =
            """
            <org.geoserver.security.config.SecurityInterceptorFilterConfig>
                <name>putXml</name>
                    <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
                    <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
                    <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
            </org.geoserver.security.config.SecurityInterceptorFilterConfig>""";

    @Test
    public void testPut_XML() throws Exception {
        MockHttpServletResponse setupResponse =
                postAsServletResponse(BASEPATH + "/security/authfilters", testSetupPutXml, "application/xml");
        TestCase.assertEquals(200, setupResponse.getStatus());

        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authfilters/putXml.xml", testPutXml, "application/xml");
        TestCase.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPut_JSON() throws Exception {
        String json = getAsString(RestBaseController.ROOT_PATH + "/security/authfilters/restInterceptor.json");
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authfilters/restInterceptor", json, "application/json");
        TestCase.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPut_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/authfilters/putXml", testPutXml, "application/xml");
        TestCase.assertEquals(403, response.getStatus());
    }

    private static final String testSetupDeleteXml =
            """
            <org.geoserver.security.config.SecurityInterceptorFilterConfig>
                <name>deleteXml</name>
                    <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
                    <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
                    <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
            </org.geoserver.security.config.SecurityInterceptorFilterConfig>""";

    @Test
    public void testDelete() throws Exception {
        MockHttpServletResponse restoreWeb =
                postAsServletResponse(BASEPATH + "/security/authfilters", testSetupDeleteXml, "application/xml");
        TestCase.assertEquals(200, restoreWeb.getStatus());

        MockHttpServletResponse response = deleteAsServletResponse(BASEPATH + "/security/authfilters/deleteXml");
        TestCase.assertEquals(200, response.getStatus());

        MockHttpServletResponse viewResponse = getAsServletResponse(BASEPATH + "/security/authfilters/deleteXml.xml");
        TestCase.assertEquals(200, viewResponse.getStatus());
    }

    @Test
    public void testDelete_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = deleteAsServletResponse(BASEPATH + "/security/authfilters/restInterceptor");
        TestCase.assertEquals(403, response.getStatus());
    }

    private void notAuthorised() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
