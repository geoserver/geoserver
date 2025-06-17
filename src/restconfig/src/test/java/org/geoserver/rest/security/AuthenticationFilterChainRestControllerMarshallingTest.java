/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
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

public class AuthenticationFilterChainRestControllerMarshallingTest extends GeoServerSystemTestSupport {
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
        Document dom = getAsDOM(BASEPATH + "/security/filterChains.xml", 200);
        // Extract all <name> values
        NodeList nameNodes = xp.getMatchingNodes("//filterChain/name", dom);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nameNodes.getLength(); i++) {
            names.add(nameNodes.item(i).getTextContent());
        }

        names.forEach(name -> {
            String xpath = String.format("//filterChain[name='%s']/atom:link", name);
            try {
                NodeList link = xp.getMatchingNodes(xpath, dom);
                assertEquals(1, link.getLength());
                String href = link.item(0).getAttributes().getNamedItem("href").getTextContent();
                assertTrue(href.endsWith("/security/filterChains/" + name + ".xml"));
            } catch (XpathException e) {
                fail("Xpath evaluation failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testList_NotAuthorised() throws Exception {
        notAuthorised();
        getAsDOM(BASEPATH + "/security/filterChains.xml", 403);
    }

    @Test
    public void testList_JSON() throws Exception {
        JSON json = getAsJSON(BASEPATH + "/security/filterChains.json", 200);
        JSONArray filterChains =
                ((JSONObject) json).getJSONObject("filterChains").getJSONArray("filterChain");

        for (Object jsonObject : filterChains) {
            JSONObject filterChain = (JSONObject) jsonObject;
            String name = filterChain.getString("name");
            String href = filterChain.getString("href");
            assertTrue(href.endsWith("/security/filterChains/" + name + ".json"));
        }
    }

    // This only checks the values in web it does not check all possible fields
    @Test
    public void testView_XML() throws Exception {
        Document document = getAsDOM(BASEPATH + "/security/filterChains/web.xml", 200);
        assertXpathEvaluatesTo("web", "/filterChain/name", document);
        assertXpathEvaluatesTo("org.geoserver.security.HtmlLoginFilterChain", "/filterChain/className", document);
        assertXpathEvaluatesTo("false", "/filterChain/disabled", document);
        assertXpathEvaluatesTo("false", "/filterChain/requireSSL", document);
        assertXpathEvaluatesTo("false", "/filterChain/matchHTTPMethod", document);
        assertXpathEvaluatesTo("0", "/filterChain/position", document);
        assertXpathEvaluatesTo("true", "/filterChain/allowSessionCreation", document);

        NodeList patternNodes = xp.getMatchingNodes("/filterChain/patterns/string", document);
        assertEquals(3, patternNodes.getLength());

        NodeList filterNodes = xp.getMatchingNodes("/filterChain/filters/string", document);
        assertEquals(3, filterNodes.getLength());
    }

    @Test
    public void testView_Unauthorised() throws Exception {
        notAuthorised();
        getAsDOM(BASEPATH + "/security/filterChains/web.xml", 403);
    }

    @Test
    public void testView_JSON() throws Exception {
        JSON json = getAsJSON(BASEPATH + "/security/filterChains/web", 200);
        JSONObject jsonObject = ((JSONObject) json).getJSONObject("filterChain");

        assertEquals("web", jsonObject.getString("name"));
        assertEquals("org.geoserver.security.HtmlLoginFilterChain", jsonObject.getString("className"));
        assertEquals("false", jsonObject.getString("disabled"));
        assertEquals("false", jsonObject.getString("requireSSL"));
        assertEquals("false", jsonObject.getString("matchHTTPMethod"));
        assertEquals("0", jsonObject.getString("position"));
        assertEquals("true", jsonObject.getString("allowSessionCreation"));

        JSONArray patterns = jsonObject.getJSONObject("patterns").getJSONArray("string");
        assertEquals(3, patterns.size());
        JSONArray filters = jsonObject.getJSONObject("filters").optJSONArray("string");
        assertEquals(3, filters.size());
    }

    @Test
    public void testPost_XML() throws Exception {
        String document = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web.xml");
        deleteAsServletResponse(RestBaseController.ROOT_PATH + "/security/filterChains/web");
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/filterChains", document, "application/xml");
        TestCase.assertEquals(201, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String location = response.getHeader("Location");
        assertNotNull(location);
        assertTrue(location.endsWith("/security/filterChains/web"));

        Document viewDocument = getAsDOM(BASEPATH + "/security/filterChains/web.xml", 200);
        assertXpathEvaluatesTo("web", "/filterChain/name", viewDocument);
        assertXpathEvaluatesTo("org.geoserver.security.HtmlLoginFilterChain", "/filterChain/className", viewDocument);
        assertXpathEvaluatesTo("false", "/filterChain/disabled", viewDocument);
        assertXpathEvaluatesTo("false", "/filterChain/requireSSL", viewDocument);
        assertXpathEvaluatesTo("false", "/filterChain/matchHTTPMethod", viewDocument);
        assertXpathEvaluatesTo("0", "/filterChain/position", viewDocument);
        assertXpathEvaluatesTo("true", "/filterChain/allowSessionCreation", viewDocument);
    }

    @Test
    public void testPost_JSON() throws Exception {
        String json = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web.json");
        deleteAsServletResponse(RestBaseController.ROOT_PATH + "/security/filterChains/web");
        MockHttpServletResponse response =
                postAsServletResponse(BASEPATH + "/security/filterChains", json, "application/json");
        TestCase.assertEquals(201, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        String location = response.getHeader("Location");
        assertNotNull(location);
        assertTrue(location.endsWith("/security/filterChains/web"));

        JSON viewJson = getAsJSON(BASEPATH + "/security/filterChains/web", 200);
        JSONObject jsonObject = ((JSONObject) viewJson).getJSONObject("filterChain");
        assertEquals("web", jsonObject.getString("name"));
        assertEquals("org.geoserver.security.HtmlLoginFilterChain", jsonObject.getString("className"));
        assertEquals("false", jsonObject.getString("disabled"));
        assertEquals("false", jsonObject.getString("requireSSL"));
        assertEquals("false", jsonObject.getString("matchHTTPMethod"));
        assertEquals("0", jsonObject.getString("position"));
        assertEquals("true", jsonObject.getString("allowSessionCreation"));
    }

    @Test
    public void testPut_XML() throws Exception {
        String xml = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web.xml");
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/filterChains/web", xml, "application/xml");
        TestCase.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPut_JSON() throws Exception {
        String json = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web.json");
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/filterChains/web", json, "application/json");
        TestCase.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPut_NotAuthorised() throws Exception {
        notAuthorised();
        String json = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web.json");
        MockHttpServletResponse response =
                putAsServletResponse(BASEPATH + "/security/filterChains/web", json, "application/json");
        TestCase.assertEquals(403, response.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        String xml = getAsString(RestBaseController.ROOT_PATH + "/security/filterChains/web.xml");
        MockHttpServletResponse response = deleteAsServletResponse(BASEPATH + "/security/filterChains/web");
        TestCase.assertEquals(200, response.getStatus());

        MockHttpServletResponse viewResponse = getAsServletResponse(BASEPATH + "/security/filterChains/web.xml");
        TestCase.assertEquals(404, viewResponse.getStatus());

        // Restore web filter incase it is used elsewwhere
        MockHttpServletResponse restoreWeb =
                postAsServletResponse(BASEPATH + "/security/filterChains", xml, "application/xml");
        TestCase.assertEquals(201, restoreWeb.getStatus());
    }

    @Test
    public void testDelete_NotAuthorised() throws Exception {
        notAuthorised();
        MockHttpServletResponse response = deleteAsServletResponse(BASEPATH + "/security/filterChains/web");
        TestCase.assertEquals(403, response.getStatus());
    }

    private void notAuthorised() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
