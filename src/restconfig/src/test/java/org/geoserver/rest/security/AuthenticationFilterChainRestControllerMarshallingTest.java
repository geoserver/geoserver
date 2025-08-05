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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import junit.framework.TestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * End-to-end marshalling tests for AuthenticationFilterChain REST API. Uses header-based content negotiation (no
 * .xml/.json suffixes).
 */
public class AuthenticationFilterChainRestControllerMarshallingTest extends GeoServerSystemTestSupport {

    private static final String BASE = RestBaseController.ROOT_PATH + "/security/filterChain";
    private static final String CLASS_HTML = "org.geoserver.security.HtmlLoginFilterChain";

    private static final String CT_XML = "application/xml";
    private static final String CT_JSON = "application/json";

    private XpathEngine xp;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Before
    public void setUp() {
        xp = XMLUnit.newXpathEngine();
        super.loginAsAdmin();
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ----------------- request helpers (header-driven) -----------------

    private MockHttpServletResponse doGet(String path, String accept) throws Exception {
        MockHttpServletRequest req = createRequest(path);
        req.setMethod("GET");
        if (accept != null) req.addHeader("Accept", accept);
        return dispatch(req);
    }

    private MockHttpServletResponse doDelete(String path, String accept) throws Exception {
        MockHttpServletRequest req = createRequest(path);
        req.setMethod("DELETE");
        if (accept != null) req.addHeader("Accept", accept);
        return dispatch(req);
    }

    private MockHttpServletResponse doPost(String path, String body, String contentType, String accept)
            throws Exception {
        MockHttpServletRequest req = createRequest(path);
        req.setMethod("POST");
        req.setContentType(contentType);
        if (accept != null) req.addHeader("Accept", accept);
        if (body != null) req.setContent(body.getBytes(StandardCharsets.UTF_8));
        return dispatch(req);
    }

    private MockHttpServletResponse doPut(String path, String body, String contentType, String accept)
            throws Exception {
        MockHttpServletRequest req = createRequest(path);
        req.setMethod("PUT");
        req.setContentType(contentType);
        if (accept != null) req.addHeader("Accept", accept);
        if (body != null) req.setContent(body.getBytes(StandardCharsets.UTF_8));
        return dispatch(req);
    }

    private Document getAsDOMWithAccept(String path, int expected, String accept) throws Exception {
        MockHttpServletResponse r = doGet(path, accept);
        assertEquals(expected, r.getStatus());
        try (ByteArrayInputStream in = new ByteArrayInputStream(r.getContentAsByteArray())) {
            return dom(in);
        }
    }

    // ----------------- content helpers -----------------

    private static final String INTERCEPTOR = "interceptor";
    private static final String EXCEPTION_TRANSLATION = "exception";

    private static String newName() {
        return "t-" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /** XML body for one chain */
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
        sb.append(" ssl=\"").append(ssl).append('"');
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
                false, /* ssl */
                false, /* matchHTTPMethod */
                Arrays.asList("rememberme", "form", "anonymous"));
    }

    /** JSON body for one chain in XStream/RestWrapper shape */
    private static String chainJson(
            String name,
            String pathCsv,
            boolean disabled,
            boolean allowSessionCreation,
            boolean ssl,
            boolean matchHTTPMethod,
            List<String> filters) {

        StringBuilder sb = new StringBuilder();
        sb.append("{\"filters\":{");
        sb.append("\"@name\":\"").append(name).append("\",");
        sb.append("\"@class\":\"").append(CLASS_HTML).append("\",");
        sb.append("\"@path\":\"").append(pathCsv).append("\",");
        sb.append("\"@disabled\":").append(disabled).append(",");
        sb.append("\"@allowSessionCreation\":").append(allowSessionCreation).append(",");
        sb.append("\"@ssl\":").append(ssl).append(",");
        sb.append("\"@matchHTTPMethod\":").append(matchHTTPMethod).append(",");
        sb.append("\"@interceptorName\":\"").append(INTERCEPTOR).append("\",");
        sb.append("\"@exceptionTranslationName\":\"")
                .append(EXCEPTION_TRANSLATION)
                .append("\",");

        sb.append("\"filter\":[");
        for (int i = 0; i < filters.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append("\"").append(filters.get(i)).append("\"");
        }
        sb.append("]}}");
        return sb.toString();
    }

    private static String defaultChainJson(String name) {
        return chainJson(
                name,
                "/web/**,/gwc/rest/web/**,/",
                false, /* disabled */
                true, /* allowSessionCreation */
                false, /* ssl */
                false, /* matchHTTPMethod */
                Arrays.asList("rememberme", "form", "anonymous"));
    }

    private void safeDeleteXml(String name) throws Exception {
        // Accept is mostly irrelevant for DELETE, but send XML to be explicit
        doDelete(BASE + "/" + name, CT_XML);
    }

    private void safeDeleteJson(String name) throws Exception {
        doDelete(BASE + "/" + name, CT_JSON);
    }

    private List<String> listNamesXml() throws Exception {
        Document dom = getAsDOMWithAccept(BASE, 200, CT_XML);
        NodeList nodes = xp.getMatchingNodes("/filterChain/filters/@name", dom);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(nodes.item(i).getNodeValue());
        }
        return names;
    }

    private List<String> listNamesJson() throws Exception {
        String body = getAsString(BASE + ".json");
        JsonNode root = MAPPER.readTree(body);
        JsonNode filters = null;
        if (root.has("filterChain")) {
            filters = root.get("filterChain").get("filters");
        } else {
            filters = root.get("filters");
        }
        List<String> names = new ArrayList<>();
        if (filters != null && filters.isArray()) {
            for (JsonNode n : filters) {
                JsonNode name = getAttr(n, "name"); // supports "name" and "@name"
                if (name != null) names.add(name.asText());
            }
        }
        return names;
    }

    // ---------- JSON helpers for XStream-style (@-prefixed) -------------

    /** Support @-prefixed attribute names produced by RestWrapper/XStream. */
    private static JsonNode attr(JsonNode n, String key) {
        if (n == null || !n.isObject()) return null;
        if (n.has(key)) return n.get(key);
        String at = "@" + key;
        return n.has(at) ? n.get(at) : null;
    }

    // --- helpers for JSON attribute-style payloads ---
    private static JsonNode getAttr(JsonNode n, String key) {
        if (n == null || !n.isObject()) return null;
        if (n.has(key)) return n.get(key);
        String at = "@" + key;
        return n.has(at) ? n.get(at) : null;
    }

    private ObjectNode normalizeChain(JsonNode raw) {
        // Takes either:
        //   { "@name": "...", "@class":"...", "filter":[...] }  // XStream style
        // or { "name": "...", "class":"...", "filters":[...] } // plain
        // and returns a normalized object with plain keys and "filters" array
        ObjectNode out = MAPPER.createObjectNode();
        String[] scalarKeys = new String[] {
            "name",
            "class",
            "path",
            "disabled",
            "allowSessionCreation",
            "ssl",
            "matchHTTPMethod",
            "interceptorName",
            "exceptionTranslationName",
            "roleFilterName"
        };
        for (String k : scalarKeys) {
            JsonNode v = getAttr(raw, k);
            if (v != null) out.set(k, v);
        }

        // normalize filters -> always an array under key "filters"
        JsonNode filters = raw.get("filters");
        if (filters == null) filters = raw.get("filter");
        if (filters == null) {
            out.putArray("filters"); // empty
        } else if (filters.isArray()) {
            out.set("filters", filters);
        } else {
            // single string into array
            ArrayNode arr = MAPPER.createArrayNode();
            arr.add(filters);
            out.set("filters", arr);
        }
        return out;
    }

    private JsonNode findFirstChain(JsonNode root) {
        if (root == null || root.isNull()) return null;

        Deque<JsonNode> dq = new ArrayDeque<>();
        dq.add(root);

        while (!dq.isEmpty()) {
            JsonNode n = dq.removeFirst();

            if (n.isObject()) {
                // Common shapes:
                // { "filters": { ... } } or { "filters": [ {...}, ... ] }
                if (n.has("filters")) {
                    JsonNode f = n.get("filters");
                    if (f.isObject() && getAttr(f, "name") != null) {
                        return normalizeChain(f);
                    } else if (f.isArray() && f.size() > 0) {
                        JsonNode first = f.get(0);
                        if (getAttr(first, "name") != null) return normalizeChain(first);
                        f.forEach(dq::addLast);
                    } else {
                        dq.addLast(f);
                    }
                }

                // Other wrapper: { "filterChain": { "filters": [...] } }
                if (n.has("filterChain")) dq.addLast(n.get("filterChain"));

                // Fallback: any object carrying a name attribute
                if (getAttr(n, "name") != null) return normalizeChain(n);

                n.fields().forEachRemaining(e -> dq.addLast(e.getValue()));
            } else if (n.isArray()) {
                n.forEach(dq::addLast);
            }
        }
        return null;
    }

    // ----------------- list -----------------

    @Test
    public void testList_XML_containsCreated() throws Exception {
        String a = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainXml(a), CT_XML, CT_XML).getStatus());
            Document dom = getAsDOMWithAccept(BASE, 200, CT_XML);
            NodeList nodes = xp.getMatchingNodes("/filterChain/filters[@name='" + a + "']", dom);
            assertEquals(1, nodes.getLength());
        } finally {
            safeDeleteXml(a);
        }
    }

    @Test
    public void testList_JSON_containsCreated() throws Exception {
        String a = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainJson(a), CT_JSON, CT_JSON).getStatus());
            List<String> names = listNamesJson();
            assertTrue("New name should appear in JSON list", names.contains(a));
        } finally {
            safeDeleteJson(a);
        }
    }

    // ----------------- view -----------------

    @Test
    public void testView_XML() throws Exception {
        String name = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainXml(name), CT_XML, CT_XML).getStatus());
            Document doc = getAsDOMWithAccept(BASE + "/" + name, 200, CT_XML);
            assertXpathEvaluatesTo(name, "/filters/@name", doc);
            assertXpathEvaluatesTo(CLASS_HTML, "/filters/@class", doc);
            assertXpathEvaluatesTo("false", "/filters/@disabled", doc);
            assertXpathEvaluatesTo("true", "/filters/@allowSessionCreation", doc);
            assertXpathEvaluatesTo("false", "/filters/@ssl", doc);
            assertXpathEvaluatesTo("false", "/filters/@matchHTTPMethod", doc);
            NodeList f = xp.getMatchingNodes("/filters/filter", doc);
            assertEquals(3, f.getLength());
        } finally {
            safeDeleteXml(name);
        }
    }

    @Test
    public void testView_JSON() throws Exception {
        String name = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainJson(name), CT_JSON, CT_JSON).getStatus());
            MockHttpServletResponse r = doGet(BASE + "/" + name, CT_JSON);
            assertEquals(200, r.getStatus());
            JsonNode node = MAPPER.readTree(r.getContentAsByteArray());
            JsonNode target = findFirstChain(node);
            assertNotNull("JSON chain object not found", target);
            assertEquals(name, target.get("name").asText());
            assertEquals(CLASS_HTML, target.get("class").asText());
            assertFalse(target.get("disabled").asBoolean());
            assertTrue(target.get("allowSessionCreation").asBoolean());
            assertFalse(target.get("ssl").asBoolean());
            assertFalse(target.get("matchHTTPMethod").asBoolean());
            assertTrue(target.get("filters").isArray());
            assertEquals(3, target.get("filters").size());
        } finally {
            safeDeleteJson(name);
        }
    }

    @Test
    public void testView_Unknown_404_XML_and_JSON() throws Exception {
        TestCase.assertEquals(404, doGet(BASE + "/does-not-exist", CT_XML).getStatus());
        TestCase.assertEquals(404, doGet(BASE + "/does-not-exist", CT_JSON).getStatus());
    }

    @Test
    public void testView_ReservedOrder_405() throws Exception {
        TestCase.assertEquals(405, doGet(BASE + "/order", CT_XML).getStatus());
        TestCase.assertEquals(405, doGet(BASE + "/order", CT_JSON).getStatus());
    }

    // ----------------- create -----------------

    @Test
    public void testPost_Create_201_and_View_XML() throws Exception {
        String name = newName();
        try {
            MockHttpServletResponse resp = doPost(BASE, defaultChainXml(name), CT_XML, CT_XML);
            TestCase.assertEquals(201, resp.getStatus());
            String location = resp.getHeader("Location");
            assertNotNull(location);
            assertTrue(location.endsWith("/security/filterChain/" + name));
            // verify view
            getAsDOMWithAccept(BASE + "/" + name, 200, CT_XML);
        } finally {
            safeDeleteXml(name);
        }
    }

    @Test
    public void testPost_Create_201_and_View_JSON() throws Exception {
        String name = newName();
        try {
            MockHttpServletResponse resp = doPost(BASE, defaultChainJson(name), CT_JSON, CT_JSON);
            TestCase.assertEquals(201, resp.getStatus());
            MockHttpServletResponse view = doGet(BASE + "/" + name, CT_JSON);
            JsonNode target = findFirstChain(MAPPER.readTree(view.getContentAsByteArray()));
            assertNotNull(target);
            assertEquals(name, target.get("name").asText());
        } finally {
            safeDeleteJson(name);
        }
    }

    @Test
    public void testPost_Duplicate_400_XML_and_JSON() throws Exception {
        String name = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainXml(name), CT_XML, CT_XML).getStatus());
            TestCase.assertEquals(
                    400, doPost(BASE, defaultChainXml(name), CT_XML, CT_XML).getStatus());
        } finally {
            safeDeleteXml(name);
        }

        String j = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainJson(j), CT_JSON, CT_JSON).getStatus());
            TestCase.assertEquals(
                    400, doPost(BASE, defaultChainJson(j), CT_JSON, CT_JSON).getStatus());
        } finally {
            safeDeleteJson(j);
        }
    }

    @Test
    public void testPost_BadPayload_400_XML_and_JSON() throws Exception {
        // XML: missing required name
        String badXml = defaultChainXml("X").replace(" name=\"X\"", "");
        TestCase.assertEquals(400, doPost(BASE, badXml, CT_XML, CT_XML).getStatus());

        // JSON: remove name field
        String badJson = defaultChainJson("Y").replace("\"@name\":\"Y\",", "");
        TestCase.assertEquals(400, doPost(BASE, badJson, CT_JSON, CT_JSON).getStatus());
    }

    // ----------------- update -----------------

    @Test
    public void testPut_Update_200_XML_and_JSON() throws Exception {
        String name = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainXml(name), CT_XML, CT_XML).getStatus());

            // XML update: flip disabled -> true
            String updatedXml = chainXml(
                    name, "/web/**,/", true, true, false, false, Arrays.asList("rememberme", "form", "anonymous"));
            MockHttpServletResponse r1 = doPut(BASE + "/" + name, updatedXml, CT_XML, CT_XML);
            TestCase.assertEquals(200, r1.getStatus());
            Document doc = getAsDOMWithAccept(BASE + "/" + name, 200, CT_XML);
            assertXpathEvaluatesTo("true", "/filters/@disabled", doc);

            // JSON update: move position and verify disabled becomes false again
            String updatedJson = defaultChainJson(name); // disabled=false
            MockHttpServletResponse r2 = doPut(BASE + "/" + name + "?position=0", updatedJson, CT_JSON, CT_JSON);
            TestCase.assertEquals(200, r2.getStatus());
            JsonNode target = findFirstChain(
                    MAPPER.readTree(doGet(BASE + "/" + name, CT_JSON).getContentAsByteArray()));
            assertNotNull(target);
            assertFalse(target.get("disabled").asBoolean());
        } finally {
            safeDeleteXml(name);
        }
    }

    @Test
    public void testPut_NameMismatch_400_XML_and_JSON() throws Exception {
        String a = newName();
        String b = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainXml(a), CT_XML, CT_XML).getStatus());

            TestCase.assertEquals(
                    400,
                    doPut(BASE + "/" + a, defaultChainXml(b), CT_XML, CT_XML).getStatus());
            TestCase.assertEquals(
                    400,
                    doPut(BASE + "/" + a, defaultChainJson(b), CT_JSON, CT_JSON).getStatus());
        } finally {
            safeDeleteXml(a);
            safeDeleteXml(b);
        }
    }

    @Test
    public void testPut_NotFound_404_XML_and_JSON() throws Exception {
        String x = newName();
        TestCase.assertEquals(
                400,
                doPut(BASE + "/does-not-exist", defaultChainXml(x), CT_XML, CT_XML)
                        .getStatus());
        TestCase.assertEquals(
                400,
                doPut(BASE + "/does-not-exist", defaultChainJson(x), CT_JSON, CT_JSON)
                        .getStatus());
    }

    // ----------------- delete -----------------

    @Test
    public void testDelete_200_and_View404_XML_and_JSON() throws Exception {
        String a = newName();
        TestCase.assertEquals(
                201, doPost(BASE, defaultChainXml(a), CT_XML, CT_XML).getStatus());
        TestCase.assertEquals(200, doDelete(BASE + "/" + a, CT_XML).getStatus());
        TestCase.assertEquals(404, doGet(BASE + "/" + a, CT_XML).getStatus());

        String b = newName();
        TestCase.assertEquals(
                201, doPost(BASE, defaultChainJson(b), CT_JSON, CT_JSON).getStatus());
        TestCase.assertEquals(200, doDelete(BASE + "/" + b, CT_JSON).getStatus());
        TestCase.assertEquals(404, doGet(BASE + "/" + b, CT_JSON).getStatus());
    }

    @Test
    public void testDelete_Unknown_410_XML_and_JSON() throws Exception {
        TestCase.assertEquals(410, doDelete(BASE + "/does-not-exist", CT_XML).getStatus());
        TestCase.assertEquals(410, doDelete(BASE + "/does-not-exist", CT_JSON).getStatus());
    }

    // ----------------- order -----------------

    @Test
    public void testOrder_Update_200_XML_and_JSON() throws Exception {
        String a = newName();
        String b = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainXml(a), CT_XML, CT_XML).getStatus());
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainXml(b), CT_XML, CT_XML).getStatus());

            List<String> cur = listNamesXml();
            assertTrue(cur.containsAll(Arrays.asList(a, b)));
            Collections.reverse(cur);

            String jsonBody = "{\"order\":" + toJsonArray(cur) + "}";
            TestCase.assertEquals(
                    200, doPut(BASE + "/order", jsonBody, CT_JSON, CT_JSON).getStatus());

            List<String> afterJson = listNamesXml(); // order applies to both, check via XML
            assertEquals(cur, afterJson);

            // Also test XML order payload
            Collections.reverse(cur); // swap back
            String xmlBody = "<order>" + toXmlList("order", cur) + "</order>";
            TestCase.assertEquals(
                    200, doPut(BASE + "/order", xmlBody, CT_XML, CT_XML).getStatus());
            List<String> afterXml = listNamesXml();
            assertEquals(cur, afterXml);

        } finally {
            safeDeleteXml(a);
            safeDeleteXml(b);
        }
    }

    @Test
    public void testOrder_InvalidPermutation_400_JSON() throws Exception {
        String a = newName();
        try {
            TestCase.assertEquals(
                    201, doPost(BASE, defaultChainJson(a), CT_JSON, CT_JSON).getStatus());
            String body = "{\"order\":[\"nonexistent-only\"]}";
            TestCase.assertEquals(
                    400, doPut(BASE + "/order", body, CT_JSON, CT_JSON).getStatus());
        } finally {
            safeDeleteJson(a);
        }
    }

    // --- tiny helpers for order payloads ---
    private static String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(items.get(i)).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String toXmlList(String tag, List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            sb.append('<')
                    .append(tag)
                    .append('>')
                    .append(s)
                    .append("</")
                    .append(tag)
                    .append('>');
        }
        return sb.toString();
    }
}
