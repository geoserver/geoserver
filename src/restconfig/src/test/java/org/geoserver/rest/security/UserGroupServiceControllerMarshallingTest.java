/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
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
 * End-to-end marshaling tests for UserGroupService REST API (XML & JSON). Uses header-based content negotiation (no
 * .xml/.json suffixes).
 */
public class UserGroupServiceControllerMarshallingTest extends GeoServerSystemTestSupport {

    private static final String BASE = RestBaseController.ROOT_PATH + "/security/usergroupservices";

    private static final String CT_XML = "application/xml";
    private static final String CT_JSON = "application/json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
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

    // ----------------- tiny utils -----------------

    private static String newName() {
        return "ugs-" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /** Minimal XML config for XMLUserGroupService (fileName is REQUIRED). */
    private static String xmlServiceXml(String name) {
        return "<org.geoserver.security.xml.XMLUserGroupServiceConfig>"
                + "  <name>" + name + "</name>"
                + "  <className>org.geoserver.security.xml.XMLUserGroupService</className>"
                + "  <fileName>" + name + ".xml</fileName>"
                + "  <passwordEncoderName>plainTextPasswordEncoder</passwordEncoderName>"
                + "  <passwordPolicyName>default</passwordPolicyName>"
                + "</org.geoserver.security.xml.XMLUserGroupServiceConfig>";
    }

    /** Minimal JSON config (XStream-friendly element-style keys). */
    private static String xmlServiceJson(String name) {
        return "{ \"org.geoserver.security.xml.XMLUserGroupServiceConfig\": {"
                + "  \"name\": \"" + name + "\","
                + "  \"className\": \"org.geoserver.security.xml.XMLUserGroupService\","
                + "  \"fileName\": \"" + name + ".xml\","
                + "  \"passwordEncoderName\": \"plainTextPasswordEncoder\","
                + "  \"passwordPolicyName\": \"default\""
                + "} }";
    }

    // ----------------- request helpers -----------------

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

    // ----------------- JSON helpers -----------------

    /** Find the first object with keys "name" and "className" somewhere in the response. */
    private JsonNode findFirstService(JsonNode root) {
        if (root == null || root.isNull()) return null;

        Deque<JsonNode> dq = new ArrayDeque<>();
        dq.add(root);

        while (!dq.isEmpty()) {
            JsonNode n = dq.removeFirst();

            if (n.isObject()) {
                if (n.has("name") && n.has("className")) return n;

                // unwrap typical shapes used by RestWrapper/XStream
                n.properties().forEach(e -> dq.addLast(e.getValue()));
            } else if (n.isArray()) {
                n.forEach(dq::addLast);
            }
        }
        return null;
    }

    // ----------------- list -----------------

    @Test
    public void testList_XML_containsCreated() throws Exception {
        String name = newName();
        try {
            MockHttpServletResponse post = doPost(BASE, xmlServiceXml(name), CT_XML, CT_XML);
            // Your controller may return 200 or 201; accept both
            assertTrue(post.getStatus() >= 200 && post.getStatus() < 300);

            Document dom = getAsDOMWithAccept(BASE, 200, CT_XML);
            // list items are summaries: <userGroupService><name>...</name><cls>...</cls></userGroupService>
            NodeList nodes = xp.getMatchingNodes("/userGroupService/userGroupService[name='" + name + "']", dom);
            // depending on wrapper, it might be /userGroupService[name='...'], try fallback if needed
            if (nodes.getLength() == 0) {
                nodes = xp.getMatchingNodes("//userGroupService[name='" + name + "']", dom);
            }
            assertEquals(1, nodes.getLength());
        } finally {
            doDelete(BASE + "/" + name, CT_XML);
        }
    }

    @Test
    public void testList_JSON_containsCreated() throws Exception {
        String name = newName();
        try {
            MockHttpServletResponse post = doPost(BASE, xmlServiceJson(name), CT_JSON, CT_JSON);
            assertTrue(post.getStatus() >= 200 && post.getStatus() < 300);

            MockHttpServletResponse list = doGet(BASE, CT_JSON);
            assertEquals(200, list.getStatus());
            JsonNode root = MAPPER.readTree(list.getContentAsByteArray());

            // gather all "name" values under any array/object called "userGroupService"
            Set<String> names = new HashSet<>();
            Deque<JsonNode> dq = new ArrayDeque<>();
            dq.add(root);
            while (!dq.isEmpty()) {
                JsonNode n = dq.removeFirst();
                if (n.isObject()) {
                    if (n.has("userGroupService")) dq.addLast(n.get("userGroupService"));
                    n.properties().forEach(e -> dq.addLast(e.getValue()));
                } else if (n.isArray()) {
                    n.forEach(dq::addLast);
                }
                // pick up any objects with a "name"
                if (n.isObject() && n.has("name")) names.add(n.get("name").asText());
            }
            assertTrue("Expected list to contain " + name, names.contains(name));
        } finally {
            doDelete(BASE + "/" + name, CT_JSON);
        }
    }

    // ----------------- view -----------------

    @Test
    public void testView_XML_roundTrip() throws Exception {
        String name = newName();
        try {
            assertTrue(doPost(BASE, xmlServiceXml(name), CT_XML, CT_XML).getStatus() < 300);
            Document doc = getAsDOMWithAccept(BASE + "/" + name, 200, CT_XML);
            assertXpathEvaluatesTo(name, "/org.geoserver.security.xml.XMLUserGroupServiceConfig/name", doc);
            assertXpathEvaluatesTo(
                    "org.geoserver.security.xml.XMLUserGroupService",
                    "/org.geoserver.security.xml.XMLUserGroupServiceConfig/className",
                    doc);
            assertXpathEvaluatesTo(
                    name + ".xml", "/org.geoserver.security.xml.XMLUserGroupServiceConfig/fileName", doc);
        } finally {
            doDelete(BASE + "/" + name, CT_XML);
        }
    }

    @Test
    public void testView_JSON_roundTrip() throws Exception {
        String name = newName();
        try {
            assertTrue(doPost(BASE, xmlServiceJson(name), CT_JSON, CT_JSON).getStatus() < 300);
            MockHttpServletResponse r = doGet(BASE + "/" + name, CT_JSON);
            assertEquals(200, r.getStatus());
            JsonNode found = findFirstService(MAPPER.readTree(r.getContentAsByteArray()));
            assertNotNull("Could not locate service object", found);
            assertEquals(name, found.get("name").asText());
            assertEquals(
                    "org.geoserver.security.xml.XMLUserGroupService",
                    found.get("className").asText());
            assertEquals(name + ".xml", found.get("fileName").asText());
        } finally {
            doDelete(BASE + "/" + name, CT_JSON);
        }
    }

    @Test
    public void testView_Unknown_404_XML_and_JSON() throws Exception {
        assertEquals(404, doGet(BASE + "/does-not-exist", CT_XML).getStatus());
        assertEquals(404, doGet(BASE + "/does-not-exist", CT_JSON).getStatus());
    }

    // ----------------- create -----------------

    @Test
    public void testPost_Create_2xx_and_View_XML() throws Exception {
        String name = newName();
        try {
            MockHttpServletResponse resp = doPost(BASE, xmlServiceXml(name), CT_XML, CT_XML);
            assertTrue(resp.getStatus() >= 200 && resp.getStatus() < 300);

            // verify view
            getAsDOMWithAccept(BASE + "/" + name, 200, CT_XML);
        } finally {
            doDelete(BASE + "/" + name, CT_XML);
        }
    }

    @Test
    public void testPost_Create_2xx_and_View_JSON() throws Exception {
        String name = newName();
        try {
            MockHttpServletResponse resp = doPost(BASE, xmlServiceJson(name), CT_JSON, CT_JSON);
            assertTrue(resp.getStatus() >= 200 && resp.getStatus() < 300);

            MockHttpServletResponse view = doGet(BASE + "/" + name, CT_JSON);
            assertEquals(200, view.getStatus());
            JsonNode found = findFirstService(MAPPER.readTree(view.getContentAsByteArray()));
            assertNotNull(found);
            assertEquals(name, found.get("name").asText());
        } finally {
            doDelete(BASE + "/" + name, CT_JSON);
        }
    }

    @Test
    public void testPost_Duplicate_400_XML_and_JSON() throws Exception {
        String name = newName();
        try {
            assertTrue(doPost(BASE, xmlServiceXml(name), CT_XML, CT_XML).getStatus() < 300);
            int dupXml = doPost(BASE, xmlServiceXml(name), CT_XML, CT_XML).getStatus();
            assertTrue("Expected 400 on duplicate (or 500 if bubbled), got " + dupXml, dupXml == 400 || dupXml == 500);
        } finally {
            doDelete(BASE + "/" + name, CT_XML);
        }

        String j = newName();
        try {
            assertTrue(doPost(BASE, xmlServiceJson(j), CT_JSON, CT_JSON).getStatus() < 300);
            int dupJson = doPost(BASE, xmlServiceJson(j), CT_JSON, CT_JSON).getStatus();
            assertTrue(
                    "Expected 400 on duplicate (or 500 if bubbled), got " + dupJson, dupJson == 400 || dupJson == 500);
        } finally {
            doDelete(BASE + "/" + j, CT_JSON);
        }
    }

    @Test
    public void testPost_BadPayload_400_XML_and_JSON() throws Exception {
        // XML: missing required name
        String badXmlMissingName = xmlServiceXml("X").replace("<name>X</name>", "");
        assertEquals(400, doPost(BASE, badXmlMissingName, CT_XML, CT_XML).getStatus());

        // XML: missing required fileName for XML service
        String badXmlMissingFile = xmlServiceXml("Y").replace("<fileName>Y.xml</fileName>", "");
        assertEquals(400, doPost(BASE, badXmlMissingFile, CT_XML, CT_XML).getStatus());

        // JSON: force validator error by providing a bogus class
        String badJsonBadClass = xmlServiceJson("Z")
                .replace("org.geoserver.security.xml.XMLUserGroupService", "com.example.DoesNotExist");

        assertEquals(500, doPost(BASE, badJsonBadClass, CT_JSON, CT_JSON).getStatus());
    }

    // ----------------- update -----------------

    @Test
    public void testPut_Update_200_XML_and_JSON() throws Exception {
        String name = newName();
        try {
            assertTrue(doPost(BASE, xmlServiceXml(name), CT_XML, CT_XML).getStatus() < 300);

            // flip encoder via XML
            String updatedXml = xmlServiceXml(name)
                    .replace(
                            "<passwordEncoderName>plainTextPasswordEncoder</passwordEncoderName>",
                            "<passwordEncoderName>digestPasswordEncoder</passwordEncoderName>");
            assertEquals(
                    200, doPut(BASE + "/" + name, updatedXml, CT_XML, CT_XML).getStatus());

            // verify XML view changed
            Document doc = getAsDOMWithAccept(BASE + "/" + name, 200, CT_XML);
            assertXpathEvaluatesTo(
                    "digestPasswordEncoder",
                    "/org.geoserver.security.xml.XMLUserGroupServiceConfig/passwordEncoderName",
                    doc);

            // switch back via JSON
            String updatedJson = xmlServiceJson(name); // plainTextPasswordEncoder
            assertEquals(
                    200, doPut(BASE + "/" + name, updatedJson, CT_JSON, CT_JSON).getStatus());

            JsonNode found = findFirstService(
                    MAPPER.readTree(doGet(BASE + "/" + name, CT_JSON).getContentAsByteArray()));
            assertNotNull(found);
            assertEquals(
                    "plainTextPasswordEncoder", found.get("passwordEncoderName").asText());
        } finally {
            doDelete(BASE + "/" + name, CT_JSON);
        }
    }

    @Test
    public void testPut_NameMismatch_400_XML_and_JSON() throws Exception {
        String a = newName();
        String b = newName();
        try {
            assertTrue(doPost(BASE, xmlServiceXml(a), CT_XML, CT_XML).getStatus() < 300);

            assertEquals(
                    400, doPut(BASE + "/" + a, xmlServiceXml(b), CT_XML, CT_XML).getStatus());
            assertEquals(
                    400,
                    doPut(BASE + "/" + a, xmlServiceJson(b), CT_JSON, CT_JSON).getStatus());
        } finally {
            doDelete(BASE + "/" + a, CT_XML);
            doDelete(BASE + "/" + b, CT_XML);
        }
    }

    @Test
    public void testPut_NotFound_400_XML_and_JSON() throws Exception {
        String x = newName();
        assertEquals(
                400,
                doPut(BASE + "/does-not-exist", xmlServiceXml(x), CT_XML, CT_XML)
                        .getStatus());
        assertEquals(
                400,
                doPut(BASE + "/does-not-exist", xmlServiceJson(x), CT_JSON, CT_JSON)
                        .getStatus());
    }

    // ----------------- delete -----------------

    @Test
    public void testDelete_200_and_View404_XML_and_JSON() throws Exception {
        String a = newName();
        assertTrue(doPost(BASE, xmlServiceXml(a), CT_XML, CT_XML).getStatus() < 300);
        assertEquals(200, doDelete(BASE + "/" + a, CT_XML).getStatus());
        assertEquals(404, doGet(BASE + "/" + a, CT_XML).getStatus());

        String b = newName();
        assertTrue(doPost(BASE, xmlServiceJson(b), CT_JSON, CT_JSON).getStatus() < 300);
        assertEquals(200, doDelete(BASE + "/" + b, CT_JSON).getStatus());
        assertEquals(404, doGet(BASE + "/" + b, CT_JSON).getStatus());
    }
}
