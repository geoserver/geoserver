/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** System tests for AuthenticationFilterChainRestController using both XML and JSON. */
public class AuthenticationFilterChainRestControllerTest extends GeoServerSystemTestSupport {

    private static final String BASE = RestBaseController.ROOT_PATH + "/security/filterChain";
    private static final String CLASS_HTML = "org.geoserver.security.HtmlLoginFilterChain";

    private static final String JSON = "application/json";
    private static final String XML = "application/xml";

    private static final String INTERCEPTOR = "interceptor";
    private static final String EXCEPTION_TRANSLATION = "exception";

    private final ObjectMapper om = new ObjectMapper();
    private XpathEngine xp;

    @Before
    public void setUp() {
        xp = XMLUnit.newXpathEngine();
        super.loginAsAdmin();
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ----------------- helpers -----------------

    private static String newName() {
        return "t-" + UUID.randomUUID().toString().replace("-", "");
    }

    /** XML body for a single <filters ...> element (DTO: AuthFilterChainFilters). */
    private static String chainXml(
            String name,
            String pathCsv,
            boolean disabled,
            boolean allowSessionCreation,
            boolean ssl,
            boolean matchHTTPMethod,
            List<String> filters) {

        StringBuilder sb = new StringBuilder();
        sb.append("<filters");
        sb.append(" name=\"").append(name).append('"');
        sb.append(" class=\"").append(CLASS_HTML).append('"');
        sb.append(" path=\"").append(pathCsv).append('"');
        sb.append(" disabled=\"").append(disabled).append('"');
        sb.append(" allowSessionCreation=\"").append(allowSessionCreation).append('"');
        sb.append(" ssl=\"").append(ssl).append('"'); // alias of requireSSL
        sb.append(" matchHTTPMethod=\"").append(matchHTTPMethod).append('"');
        sb.append(" interceptorName=\"").append(INTERCEPTOR).append('"');
        sb.append(" exceptionTranslationName=\"").append(EXCEPTION_TRANSLATION).append('"');
        sb.append(">");
        for (String f : filters) sb.append("<filter>").append(f).append("</filter>");
        sb.append("</filters>");
        return sb.toString();
    }

    private static String defaultChainXml(String name) {
        return chainXml(
                name,
                "/web/**,/gwc/rest/web/**,/",
                false, /* disabled */
                true, /* allowSessionCreation */
                false, /* ssl (requireSSL) */
                false, /* matchHTTPMethod */
                Arrays.asList("rememberme", "form", "anonymous"));
    }

    /** JSON body using RestWrapper with root "filters". */
    private static String chainJson(
            String name,
            String pathCsv,
            boolean disabled,
            boolean allowSessionCreation,
            boolean requireSSL,
            boolean matchHTTPMethod,
            List<String> filters) {

        return "{\n" + "  \"filters\": {\n"
                + "    \"@name\": "
                + q(name) + ",\n" + "    \"@class\": "
                + q(CLASS_HTML) + ",\n" + "    \"@path\": "
                + q(pathCsv) + ",\n" + "    \"@disabled\": "
                + disabled + ",\n" + "    \"@allowSessionCreation\": "
                + allowSessionCreation + ",\n" + "    \"@ssl\": "
                + requireSSL + ",\n" + "    \"@matchHTTPMethod\": "
                + matchHTTPMethod + ",\n" + "    \"@interceptorName\": "
                + q(INTERCEPTOR) + ",\n" + "    \"@exceptionTranslationName\": "
                + q(EXCEPTION_TRANSLATION) + ",\n"
                + "    \"filter\": "
                + toJsonArray(filters) + "\n" + "  }\n"
                + "}";
    }

    private static String defaultChainJson(String name) {
        return chainJson(
                name,
                "/web/**,/gwc/rest/web/**,/",
                false, /* disabled */
                true, /* allowSessionCreation */
                false, /* requireSSL */
                false, /* matchHTTPMethod */
                Arrays.asList("rememberme", "form", "anonymous"));
    }

    private static String q(String s) {
        return "\"" + s + "\"";
    }

    private static String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(q(items.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private void safeDelete(String name) throws Exception {
        deleteAsServletResponse(BASE + "/" + name); // 200 or 410 is fine
    }

    private List<String> listNamesXML() throws Exception {
        Document dom = getAsDOM(BASE, 200);
        NodeList nodes = xp.getMatchingNodes("/filterChain/filters/@name", dom);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(nodes.item(i).getNodeValue());
        }
        return names;
    }

    private List<String> listNamesJSON() throws Exception {
        MockHttpServletResponse r = getAsServletResponse(BASE + ".json");
        assertEquals(200, r.getStatus());
        JsonNode root = om.readTree(r.getContentAsByteArray());
        JsonNode arr = root.path("filterChain").path("filters");
        List<String> names = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode n : arr) names.add(n.path("@name").asText());
        }
        return names;
    }

    // ----------------- list -----------------

    @Test
    public void testList_XML_containsDefault() throws Exception {
        Document dom = getAsDOM(BASE, 200);
        NodeList nodes = xp.getMatchingNodes("/filterChain/filters[@name='default']", dom);
        assertTrue(nodes.getLength() >= 1);
    }

    @Test
    public void testList_JSON_containsDefault() throws Exception {
        MockHttpServletResponse r = getAsServletResponse(BASE + ".json");
        assertEquals(200, r.getStatus());
        JsonNode root = om.readTree(r.getContentAsByteArray());
        JsonNode arr = root.path("filterChain").path("filters");
        boolean found = false;
        if (arr.isArray()) {
            for (JsonNode n : arr) if ("default".equals(n.path("@name").asText())) found = true;
        }
        assertTrue("default chain should be present", found);
    }

    // ----------------- view -----------------

    @Test
    public void testView_XML() throws Exception {
        String name = newName();
        try {
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(name), XML).getStatus());
            Document doc = getAsDOM(BASE + "/" + name, 200);
            assertXpathEvaluatesTo(name, "/filters/@name", doc);
            assertXpathEvaluatesTo(CLASS_HTML, "/filters/@class", doc);
            assertXpathEvaluatesTo("false", "/filters/@disabled", doc);
            assertXpathEvaluatesTo("true", "/filters/@allowSessionCreation", doc);
            assertXpathEvaluatesTo("false", "/filters/@ssl", doc);
            assertXpathEvaluatesTo("false", "/filters/@matchHTTPMethod", doc);
            NodeList f = xp.getMatchingNodes("/filters/filter", doc);
            assertEquals(3, f.getLength());
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testView_JSON() throws Exception {
        String name = newName();
        try {
            assertEquals(
                    201,
                    postAsServletResponse(BASE, defaultChainJson(name), JSON).getStatus());

            MockHttpServletResponse r = getAsServletResponse(BASE + "/" + name + ".json");
            assertEquals(200, r.getStatus());

            JsonNode w = om.readTree(r.getContentAsByteArray());
            JsonNode obj = w.path("filters");

            assertEquals(name, obj.path("@name").asText());
            String clazz = obj.has("@class")
                    ? obj.path("@class").asText()
                    : obj.path("class").asText();
            assertEquals(CLASS_HTML, clazz);

            assertFalse(obj.path("@disabled").asBoolean());
            assertTrue(obj.path("@allowSessionCreation").asBoolean());
            assertFalse(obj.path("@ssl").asBoolean());
            assertFalse(obj.path("@matchHTTPMethod").asBoolean());
            assertEquals(3, obj.path("filter").size()); // singular "filter"
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testView_Unknown_404() throws Exception {
        assertEquals(404, getAsServletResponse(BASE + "/does-not-exist").getStatus());
    }

    // ----------------- create -----------------

    @Test
    public void testCreate_XML_and_View_Both() throws Exception {
        String name = newName();
        try {
            MockHttpServletResponse resp = postAsServletResponse(BASE, defaultChainXml(name), XML);
            assertEquals(201, resp.getStatus());
            String location = resp.getHeader("Location");
            assertNotNull(location);
            assertTrue(location.endsWith("/security/filterChain/" + name));

            getAsDOM(BASE + "/" + name, 200);
            MockHttpServletResponse r = getAsServletResponse(BASE + "/" + name + ".json");
            assertEquals(200, r.getStatus());
            JsonNode obj = om.readTree(r.getContentAsByteArray()).path("filters");
            assertEquals(name, obj.path("@name").asText());
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testCreate_JSON_and_View_Both() throws Exception {
        String name = newName();
        try {
            assertEquals(
                    201,
                    postAsServletResponse(BASE, defaultChainJson(name), JSON).getStatus());
            getAsDOM(BASE + "/" + name, 200);
            MockHttpServletResponse r = getAsServletResponse(BASE + "/" + name + ".json");
            assertEquals(200, r.getStatus());
            assertEquals(
                    name,
                    om.readTree(r.getContentAsByteArray())
                            .path("filters")
                            .path("@name")
                            .asText());
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testCreate_Duplicate_400() throws Exception {
        String name = newName();
        try {
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(name), XML).getStatus());
            assertEquals(
                    400, postAsServletResponse(BASE, defaultChainXml(name), XML).getStatus());
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testCreate_Unauthorised_403() throws Exception {
        SecurityContextHolder.clearContext();
        assertEquals(
                403,
                postAsServletResponse(BASE, defaultChainXml(newName()), XML).getStatus());
        super.loginAsAdmin(); // restore
    }

    // ----------------- update -----------------

    @Test
    public void testUpdate_XML_200_and_MovePosition() throws Exception {
        String a = newName();
        String b = newName();
        try {
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(a), XML).getStatus());
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(b), XML).getStatus());

            MockHttpServletResponse resp =
                    putAsServletResponse(BASE + "/" + b + "?position=0", defaultChainXml(b), XML);
            assertEquals(200, resp.getStatus());

            List<String> names = listNamesXML();
            assertTrue(names.size() >= 2);
            assertEquals(b, names.get(0));
        } finally {
            safeDelete(a);
            safeDelete(b);
        }
    }

    @Test
    public void testUpdate_JSON_200() throws Exception {
        String name = newName();
        try {
            assertEquals(
                    201,
                    postAsServletResponse(BASE, defaultChainJson(name), JSON).getStatus());

            String updated = chainJson(
                    name, "/web/**,/", true, true, false, false, Arrays.asList("rememberme", "form", "anonymous"));
            MockHttpServletResponse resp = putAsServletResponse(BASE + "/" + name, updated, JSON);
            assertEquals(200, resp.getStatus());

            MockHttpServletResponse r = getAsServletResponse(BASE + "/" + name + ".json");
            JsonNode obj = om.readTree(r.getContentAsByteArray()).path("filters");
            assertTrue(obj.path("@disabled").asBoolean());
            assertEquals("/web/**,/", obj.path("@path").asText());
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testUpdate_NameMismatch_400() throws Exception {
        String a = newName();
        String b = newName();
        try {
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(a), XML).getStatus());
            assertEquals(
                    400,
                    putAsServletResponse(BASE + "/" + a, defaultChainJson(b), JSON)
                            .getStatus());
        } finally {
            safeDelete(a);
            safeDelete(b);
        }
    }

    // ----------------- delete -----------------

    @Test
    public void testDelete_200_and_View404() throws Exception {
        String a = newName();
        assertEquals(201, postAsServletResponse(BASE, defaultChainXml(a), XML).getStatus());
        assertEquals(200, deleteAsServletResponse(BASE + "/" + a).getStatus());
        assertEquals(404, getAsServletResponse(BASE + "/" + a).getStatus());
    }

    @Test
    public void testDelete_NotAuthorised_403() throws Exception {
        String a = newName();
        try {
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(a), XML).getStatus());
            SecurityContextHolder.clearContext();
            assertEquals(403, deleteAsServletResponse(BASE + "/" + a).getStatus());
        } finally {
            super.loginAsAdmin();
            safeDelete(a);
        }
    }

    // ----------------- order -----------------

    @Test
    public void testOrder_Update_JSON_200() throws Exception {
        String a = newName();
        String b = newName();
        try {
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(a), XML).getStatus());
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(b), XML).getStatus());

            List<String> cur = listNamesJSON();
            assertTrue(cur.containsAll(Arrays.asList(a, b)));
            Collections.reverse(cur);

            String body = "{ \"order\": " + toJsonArray(cur) + " }";
            assertEquals(200, putAsServletResponse(BASE + "/order", body, JSON).getStatus());

            List<String> after = listNamesJSON();
            assertEquals(cur, after);
        } finally {
            safeDelete(a);
            safeDelete(b);
        }
    }

    @Test
    public void testOrder_InvalidPermutation_400() throws Exception {
        String a = newName();
        try {
            assertEquals(
                    201, postAsServletResponse(BASE, defaultChainXml(a), XML).getStatus());
            String body = "{ \"order\": [\"nonexistent-only\"] }";
            assertEquals(400, putAsServletResponse(BASE + "/order", body, JSON).getStatus());
        } finally {
            safeDelete(a);
        }
    }
}