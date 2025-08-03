/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import java.util.*;
import junit.framework.TestCase;
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

/** Tests for AuthenticationFilterChain REST API (XML contract + JSON /order). */
public class AuthenticationFilterChainRestControllerMarshallingTest extends GeoServerSystemTestSupport {

    private static final String BASEPATH = RestBaseController.ROOT_PATH;
    private static final String CLASS_HTML = "org.geoserver.security.HtmlLoginFilterChain";

    private static final String INTERCEPTOR = "interceptor";
    private static final String EXCEPTION_TRANSLATION = "exception";

    private XpathEngine xp;

    @Before
    public void setUp() {
        xp = XMLUnit.newXpathEngine();
        super.loginAsAdmin(); // do not override; just call
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ----------------- helpers -----------------

    private static String newName() {
        return "t-" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Build a single chain XML payload:
     *
     * <p><filters name="..." class="..." path="a,b" disabled="false" allowSessionCreation="true" ssl="false"
     * matchHTTPMethod="false" interceptorName="..." exceptionTranslationName="..."> <filter>f1</filter> ... </filters>
     */
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

    private void safeDelete(String name) throws Exception {
        // Accept 200 (deleted) or 410 (already gone)
        deleteAsServletResponse(BASEPATH + "/security/filterChain/" + name);
    }

    private List<String> listNames() throws Exception {
        Document dom = getAsDOM(BASEPATH + "/security/filterChain/", 200);
        NodeList nodes = xp.getMatchingNodes("/filterChain/filters/@name", dom);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(nodes.item(i).getNodeValue());
        }
        return names;
    }

    // ----------------- list -----------------

    @Test
    public void testList_XML_containsCreated() throws Exception {
        String a = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain", defaultChainXml(a))
                            .getStatus());
            Document dom = getAsDOM(BASEPATH + "/security/filterChain.xml", 200);
            NodeList nodes = xp.getMatchingNodes("/filterChain/filters[@name='" + a + "']", dom);
            assertEquals(1, nodes.getLength());
        } finally {
            safeDelete(a);
        }
    }

    @Test
    public void testList_NotAuthorised_403() throws Exception {
        SecurityContextHolder.clearContext();
        TestCase.assertEquals(
                403, getAsServletResponse(BASEPATH + "/security/filterChain").getStatus());
    }

    // ----------------- view -----------------

    @Test
    public void testView_XML() throws Exception {
        String name = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(name))
                            .getStatus());
            Document doc = getAsDOM(BASEPATH + "/security/filterChain/" + name, 200);
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
    public void testView_Unknown_404() throws Exception {
        TestCase.assertEquals(
                404,
                getAsServletResponse(BASEPATH + "/security/filterChain/does-not-exist")
                        .getStatus());
    }

    @Test
    public void testView_ReservedOrder_405() throws Exception {
        // GET on /filterChain/order is not allowed
        TestCase.assertEquals(
                405,
                getAsServletResponse(BASEPATH + "/security/filterChain/order").getStatus());
    }

    // ----------------- create -----------------

    @Test
    public void testPost_Create_201_and_View() throws Exception {
        String name = newName();
        try {
            MockHttpServletResponse resp =
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(name));
            TestCase.assertEquals(201, resp.getStatus());
            String location = resp.getHeader("Location");
            assertNotNull(location);
            assertTrue(location.endsWith("/security/filterChain/" + name));

            // verify view works
            getAsDOM(BASEPATH + "/security/filterChain/" + name, 200);
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testPost_WithPosition_201() throws Exception {
        String a = newName();
        String b = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(a))
                            .getStatus());
            MockHttpServletResponse r =
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml" + "?position=0", defaultChainXml(b));
            TestCase.assertEquals(201, r.getStatus());
            // b should now be first
            List<String> names = listNames();
            assertTrue(names.size() >= 2);
            assertEquals(b, names.get(0));
        } finally {
            safeDelete(a);
            safeDelete(b);
        }
    }

    @Test
    public void testPost_Duplicate_400() throws Exception {
        String name = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(name))
                            .getStatus());
            TestCase.assertEquals(
                    400,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(name))
                            .getStatus());
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testPost_BadPayload_400() throws Exception {
        // missing required name attribute
        String bad = defaultChainXml("X").replace(" name=\"X\"", "");
        TestCase.assertEquals(
                500,
                postAsServletResponse(BASEPATH + "/security/filterChain.xml", bad)
                        .getStatus());
    }

    @Test
    public void testPost_Unauthorised_403() throws Exception {
        SecurityContextHolder.clearContext();
        TestCase.assertEquals(
                403,
                postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(newName()))
                        .getStatus());
    }

    // ----------------- update -----------------

    @Test
    public void testPut_Update_200() throws Exception {
        String name = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(name))
                            .getStatus());
            // flip disabled to true
            String updated = chainXml(
                    name, "/web/**,/", true, true, false, false, Arrays.asList("rememberme", "form", "anonymous"));
            MockHttpServletResponse resp =
                    putAsServletResponse(BASEPATH + "/security/filterChain/" + name, updated, "application/xml");
            TestCase.assertEquals(200, resp.getStatus());

            Document doc = getAsDOM(BASEPATH + "/security/filterChain/" + name, 200);
            assertXpathEvaluatesTo("true", "/filters/@disabled", doc);
        } finally {
            safeDelete(name);
        }
    }

    @Test
    public void testPut_MovePosition_200() throws Exception {
        String a = newName();
        String b = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(a))
                            .getStatus());
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(b))
                            .getStatus());

            // move b to position 0
            MockHttpServletResponse resp = putAsServletResponse(
                    BASEPATH + "/security/filterChain/" + b + "?position=0", defaultChainXml(b), "application/xml");
            TestCase.assertEquals(200, resp.getStatus());

            List<String> names = listNames();
            assertTrue(names.size() >= 2);
            assertEquals(b, names.get(0));
        } finally {
            safeDelete(a);
            safeDelete(b);
        }
    }

    @Test
    public void testPut_NameMismatch_400() throws Exception {
        String a = newName();
        String b = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(a))
                            .getStatus());
            TestCase.assertEquals(
                    400,
                    putAsServletResponse(BASEPATH + "/security/filterChain/" + a, defaultChainXml(b), "application/xml")
                            .getStatus());
        } finally {
            safeDelete(a);
            safeDelete(b);
        }
    }

    @Test
    public void testPut_NotFound_404() throws Exception {
        String body = defaultChainXml(newName());
        TestCase.assertEquals(
                400,
                putAsServletResponse(BASEPATH + "/security/filterChain/does-not-exist", body, "application/xml")
                        .getStatus());
    }

    @Test
    public void testPut_NotAuthorised_403() throws Exception {
        String a = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(a))
                            .getStatus());
            SecurityContextHolder.clearContext();
            TestCase.assertEquals(
                    403,
                    putAsServletResponse(BASEPATH + "/security/filterChain/" + a, defaultChainXml(a), "application/xml")
                            .getStatus());
        } finally {
            super.loginAsAdmin();
            safeDelete(a);
        }
    }

    // ----------------- delete -----------------

    @Test
    public void testDelete_200_and_View404() throws Exception {
        String a = newName();
        TestCase.assertEquals(
                201,
                postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(a))
                        .getStatus());
        TestCase.assertEquals(
                200,
                deleteAsServletResponse(BASEPATH + "/security/filterChain/" + a).getStatus());
        TestCase.assertEquals(
                404,
                getAsServletResponse(BASEPATH + "/security/filterChain/" + a).getStatus());
    }

    @Test
    public void testDelete_NotAuthorised_403() throws Exception {
        String a = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(a))
                            .getStatus());
            SecurityContextHolder.clearContext();
            TestCase.assertEquals(
                    403,
                    deleteAsServletResponse(BASEPATH + "/security/filterChain/" + a)
                            .getStatus());
        } finally {
            super.loginAsAdmin();
            safeDelete(a);
        }
    }

    @Test
    public void testDelete_Unknown_410() throws Exception {
        TestCase.assertEquals(
                410,
                deleteAsServletResponse(BASEPATH + "/security/filterChain/does-not-exist")
                        .getStatus());
    }

    // ----------------- order (JSON) -----------------

    @Test
    public void testOrder_Update_200() throws Exception {
        String a = newName();
        String b = newName();
        try {
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(a))
                            .getStatus());
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(b))
                            .getStatus());

            List<String> cur = listNames();
            assertTrue(cur.containsAll(Arrays.asList(a, b)));

            // reverse order
            Collections.reverse(cur);
            String body = "{\"order\":" + toJsonArray(cur) + "}";

            TestCase.assertEquals(
                    200,
                    putAsServletResponse(BASEPATH + "/security/filterChain/order", body, "application/json")
                            .getStatus());
            List<String> after = listNames();
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
            TestCase.assertEquals(
                    201,
                    postAsServletResponse(BASEPATH + "/security/filterChain.xml", defaultChainXml(a))
                            .getStatus());
            String body = "{\"order\":[\"nonexistent-only\"]}";
            TestCase.assertEquals(
                    400,
                    putAsServletResponse(BASEPATH + "/security/filterChain/order", body, "application/json")
                            .getStatus());
        } finally {
            safeDelete(a);
        }
    }

    @Test
    public void testOrder_MethodNotAllowed_405() throws Exception {
        // POST/DELETE not allowed on /order
        TestCase.assertEquals(
                405,
                postAsServletResponse(BASEPATH + "/security/filterChain/order.json", "{}")
                        .getStatus());
        TestCase.assertEquals(
                405,
                deleteAsServletResponse(BASEPATH + "/security/filterChain/order")
                        .getStatus());
    }

    // --- tiny JSON helper (avoid json-lib inheritance issues) ---
    private static String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(items.get(i)).append('"');
        }
        sb.append(']');
        return sb.toString();
    }
}
