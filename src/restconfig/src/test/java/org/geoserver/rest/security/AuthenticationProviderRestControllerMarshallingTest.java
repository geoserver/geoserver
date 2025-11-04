/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * System tests for the REST resource {@code /security/authproviders}.
 *
 * <p>These tests go through the HTTP layer using the GeoServer test harness and verify:
 *
 * <ul>
 *   <li>JSON/XML marshaling shapes,
 *   <li>headers and status codes,
 *   <li>side effects on the enabled order (authproviderNames).
 * </ul>
 *
 * <p>XStream may produce different JSON shapes: FQN-keyed ({@code {"<FQN>": {...}}}), enveloped
 * ({@code {"authprovider": {...}}}), or a bare object. Helpers below normalize these shapes for assertions.
 */
public class AuthenticationProviderRestControllerMarshallingTest extends GeoServerSystemTestSupport {

    // ---------------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------------

    private static final String BASE = RestBaseController.ROOT_PATH;
    private static final String CLASSNAME_UP = "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider";
    private static final String FQN_CFG = "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig";

    /** Track names created during tests so they can be removed on teardown. */
    private final Set<String> created = new LinkedHashSet<>();

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    @Before
    public void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        asAdmin();
    }

    @After
    public void tearDown() throws Exception {
        // if a test cleared auth (e.g. to assert 403), re-auth so cleanup can run
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            asAdmin();
        }
        try {
            // restore order to just ["default"] (best effort)
            try {
                adminPUT(
                        BASE + "/security/authproviders/order",
                        "{\"order\":[\"default\"]}".getBytes(StandardCharsets.UTF_8),
                        "application/json",
                        200);
            } catch (Exception ignored) {
                // ignore cleanup hiccups
            }
            for (String n : created) {
                try {
                    adminDELETE(BASE + "/security/authproviders/" + n);
                } catch (Exception ignored) {
                    // ignore cleanup hiccups
                }
            }
        } finally {
            created.clear();
            SecurityContextHolder.clearContext();
        }
    }

    // ---------------------------------------------------------------------
    // Security helpers
    // ---------------------------------------------------------------------

    private void asAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "geoserver", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------
    // HTTP helpers (auth via SecurityContext)
    // ---------------------------------------------------------------------

    private MockHttpServletResponse adminPOST(String path, byte[] body, String contentType) throws Exception {
        MockHttpServletRequest req = createRequest(path);
        req.setMethod("POST");
        req.setContentType(contentType);
        req.setContent(body);
        return dispatch(req);
    }

    private MockHttpServletResponse adminPUT(String path, byte[] body, String contentType, int expected)
            throws Exception {
        MockHttpServletRequest req = createRequest(path);
        req.setMethod("PUT");
        req.setContentType(contentType);
        req.setContent(body);
        MockHttpServletResponse r = dispatch(req);
        assertEquals(expected, r.getStatus());
        return r;
    }

    private MockHttpServletResponse adminPUT(String path, byte[] body, String contentType) throws Exception {
        return adminPUT(path, body, contentType, 200);
    }

    private MockHttpServletResponse adminDELETE(String path) throws Exception {
        MockHttpServletRequest req = createRequest(path);
        req.setMethod("DELETE");
        return dispatch(req);
    }

    private String adminGETString(String path, String accept, int expected) throws Exception {
        MockHttpServletRequest req = createRequest(path);
        req.setMethod("GET");
        if (accept != null) req.addHeader("Accept", accept);
        MockHttpServletResponse r = dispatch(req);
        assertEquals(expected, r.getStatus());
        return r.getContentAsString();
    }

    private String adminGETString200(String path) throws Exception {
        return adminGETString(path, null, 200);
    }

    private JSON adminGETJSON(String path) throws Exception {
        String s = adminGETString(path, "application/json", 200);
        return JSONSerializer.toJSON(s);
    }

    // ---------------------------------------------------------------------
    // Payload builders
    // ---------------------------------------------------------------------

    private static String jsonCreate(String name) {
        JSONObject inner = new JSONObject();
        inner.put("name", name);
        inner.put("className", CLASSNAME_UP);
        inner.put("userGroupServiceName", "default");
        JSONObject root = new JSONObject();
        root.put("authprovider", inner);
        return root.toString();
    }

    private static String jsonUpdate(String id, String name) {
        JSONObject inner = new JSONObject();
        if (id != null) inner.put("id", id);
        inner.put("name", name);
        inner.put("className", CLASSNAME_UP);
        inner.put("userGroupServiceName", "default");
        JSONObject root = new JSONObject();
        root.put("authprovider", inner);
        return root.toString();
    }

    private static String xmlCreate(String name) {
        return "<" + FQN_CFG + ">"
                + "<name>" + name + "</name>"
                + "<className>" + CLASSNAME_UP + "</className>"
                + "<userGroupServiceName>default</userGroupServiceName>"
                + "</" + FQN_CFG + ">";
    }

    private static byte[] json(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] xml(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------------
    // JSON shape normalization helpers
    // ---------------------------------------------------------------------

    private static JSONObject unwrapSingle(JSONObject root) {
        if (root.containsKey("authprovider")) {
            return root.getJSONObject("authprovider");
        }
        if (root.size() == 1) {
            String key = (String) root.keySet().iterator().next();
            if (key.startsWith("org.geoserver.security.config.")) {
                return root.getJSONObject(key);
            }
        }
        return root;
    }

    private static JSONArray unwrapList(JSON json) {
        JSONObject root = (JSONObject) json;
        if (root.containsKey("status")) {
            throw new AssertionError("Server returned error: " + root);
        }
        Object ap = root.get("authproviders");
        if (ap instanceof JSONArray arr) {
            JSONArray out = new JSONArray();
            for (Object o : arr) out.add(unwrapSingle((JSONObject) o));
            return out;
        }
        if (ap instanceof JSONObject obj) {
            if (obj.size() == 1) {
                String key = (String) obj.keySet().iterator().next();
                Object val = obj.get(key);
                if (val instanceof JSONArray array) return array;
                JSONArray out = new JSONArray();
                out.add(val);
                return out;
            }
        }
        throw new AssertionError("Unrecognized authproviders JSON shape: " + root);
    }

    private static List<String> names(JSONArray arr) {
        List<String> out = new ArrayList<>();
        for (Object o : arr) out.add(((JSONObject) o).optString("name"));
        return out;
    }

    // ---------------------------------------------------------------------
    // Tests: list / one
    // ---------------------------------------------------------------------

    /** Lists providers in XML/JSON; shapes include the expected elements and created names are present. */
    @Test
    public void list_xml_and_json_shapes() throws Exception {
        String a = "MARSHAL-" + System.nanoTime() + "-listA";
        String b = "MARSHAL-" + System.nanoTime() + "-listB";
        created.add(a);
        created.add(b);

        adminPOST(BASE + "/security/authproviders", xml(xmlCreate(a)), "application/xml");
        adminPOST(BASE + "/security/authproviders", json(jsonCreate(b)), "application/json");

        adminPUT(
                BASE + "/security/authproviders/order",
                json("{\"order\":[\"" + a + "\",\"" + b + "\"]}"),
                "application/json",
                200);

        String xmlList = adminGETString200(BASE + "/security/authproviders.xml");
        assertThat(xmlList, containsString("<authproviders>"));
        assertThat(xmlList, containsString("<" + FQN_CFG + ">"));

        JSON j = adminGETJSON(BASE + "/security/authproviders.json");
        JSONArray items = unwrapList(j);
        boolean sawA = false, sawB = false;
        for (Object o : items) {
            String name = ((JSONObject) o).optString("name");
            if (a.equals(name)) sawA = true;
            if (b.equals(name)) sawB = true;
        }
        assertTrue(sawA && sawB);
    }

    /** Fetches a single provider in XML and JSON and verifies key fields and element names. */
    @Test
    public void get_one_xml_and_json() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-view";
        created.add(name);

        adminPOST(BASE + "/security/authproviders", xml(xmlCreate(name)), "application/xml");

        String xml = adminGETString200(BASE + "/security/authproviders/" + name + ".xml");
        assertThat(xml, containsString("<" + FQN_CFG + ">"));
        assertThat(xml, containsString("<name>" + name + "</name>"));

        JSON j = adminGETJSON(BASE + "/security/authproviders/" + name + ".json");
        JSONObject core = unwrapSingle((JSONObject) j);
        assertEquals(name, core.getString("name"));
        assertEquals(CLASSNAME_UP, core.getString("className"));
        assertTrue(core.has("id"));
    }

    // ---------------------------------------------------------------------
    // Tests: create
    // ---------------------------------------------------------------------

    /** Creates via JSON; expects 201, Location header, and persisted body fields. */
    @Test
    public void create_json_sets_location_and_echoes_body() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-create-json";
        created.add(name);

        MockHttpServletResponse r =
                adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");
        assertEquals(201, r.getStatus());
        String loc = r.getHeader("Location");
        assertNotNull(loc);
        assertTrue(loc.endsWith("/security/authproviders/" + name));

        JSON j = adminGETJSON(BASE + "/security/authproviders/" + name + ".json");
        JSONObject core = unwrapSingle((JSONObject) j);
        assertEquals(name, core.getString("name"));
        assertEquals(CLASSNAME_UP, core.getString("className"));
    }

    /** Envelope form is accepted and materializes in list. */
    @Test
    public void create_json_envelope_is_accepted() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-create-envelope";
        created.add(name);

        adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");

        JSON j = adminGETJSON(BASE + "/security/authproviders.json");
        JSONArray items = unwrapList(j);
        boolean saw = false;
        for (Object o : items) if (name.equals(((JSONObject) o).optString("name"))) saw = true;
        assertTrue(saw);
    }

    /** Duplicate name is rejected with 400 and an explanatory message. */
    @Test
    public void create_duplicate_name_rejected_400() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-dup";
        created.add(name);

        MockHttpServletResponse r1 =
                adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");
        assertEquals(201, r1.getStatus());

        MockHttpServletResponse r2 =
                adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");
        assertEquals(400, r2.getStatus());
        assertThat(r2.getContentAsString(), containsString("already exists"));
    }

    /** XML create with {@code position=0} places provider first in enabled order. */
    @Test
    public void create_xml_position0_isFirst() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-xmlpos0";
        created.add(name);

        MockHttpServletResponse r =
                adminPOST(BASE + "/security/authproviders?position=0", xml(xmlCreate(name)), "application/xml");
        assertEquals(201, r.getStatus());

        JSON j = adminGETJSON(BASE + "/security/authproviders.json");
        JSONArray items = unwrapList(j);
        assertEquals(name, ((JSONObject) items.get(0)).optString("name"));
    }

    /** Reserved name {@code order} is rejected with 400. */
    @Test
    public void create_json_reserved_name_order_400() throws Exception {
        MockHttpServletResponse r =
                adminPOST(BASE + "/security/authproviders", json(jsonCreate("order")), "application/json");
        assertEquals(400, r.getStatus());
        assertThat(r.getContentAsString(), containsString("reserved"));
    }

    /** Missing {@code className} in JSON payload yields 400. */
    @Test
    public void create_json_missing_className_400() throws Exception {
        JSONObject inner = new JSONObject();
        inner.put("name", "MARSHAL-" + System.nanoTime() + "-nocls");
        JSONObject root = new JSONObject();
        root.put("authprovider", inner);

        MockHttpServletResponse r =
                adminPOST(BASE + "/security/authproviders", json(root.toString()), "application/json");
        assertEquals(400, r.getStatus());
    }

    // ---------------------------------------------------------------------
    // Tests: update
    // ---------------------------------------------------------------------

    /** Update with {@code position=1} reorders providers; IDs and classes remain the same. */
    @Test
    public void update_move_position_and_persist_json() throws Exception {
        String a = "MARSHAL-" + System.nanoTime() + "-moveA";
        String b = "MARSHAL-" + System.nanoTime() + "-moveB";
        created.add(a);
        created.add(b);

        adminPOST(BASE + "/security/authproviders", json(jsonCreate(a)), "application/json");
        adminPOST(BASE + "/security/authproviders", json(jsonCreate(b)), "application/json");

        adminPUT(
                BASE + "/security/authproviders/order",
                json("{\"order\":[\"" + a + "\",\"" + b + "\"]}"),
                "application/json",
                200);

        JSON jA = adminGETJSON(BASE + "/security/authproviders/" + a + ".json");
        String idA = unwrapSingle((JSONObject) jA).getString("id");

        MockHttpServletResponse up = adminPUT(
                BASE + "/security/authproviders/" + a + "?position=1", json(jsonUpdate(idA, a)), "application/json");
        assertEquals(200, up.getStatus());

        JSON list = adminGETJSON(BASE + "/security/authproviders.json");
        JSONArray items = unwrapList(list);
        List<String> order = new ArrayList<>();
        for (Object o : items) order.add(((JSONObject) o).optString("name"));
        assertThat(order, contains(b, a));
    }

    /** Changing {@code className} on update is rejected with 400 and a helpful message. */
    @Test
    public void update_rejects_className_change_400() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-cls";
        created.add(name);

        adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");
        JSON j = adminGETJSON(BASE + "/security/authproviders/" + name + ".json");
        String id = unwrapSingle((JSONObject) j).getString("id");

        JSONObject inner = new JSONObject();
        inner.put("id", id);
        inner.put("name", name);
        inner.put("className", "org.geoserver.security.auth.SomeOtherProvider");
        inner.put("userGroupServiceName", "default");
        JSONObject root = new JSONObject();
        root.put("authprovider", inner);

        MockHttpServletResponse up =
                adminPUT(BASE + "/security/authproviders/" + name, json(root.toString()), "application/json", 400);
        assertThat(up.getContentAsString(), containsString("Unsupported className"));
    }

    /** Path/payload name mismatch is rejected with 400. */
    @Test
    public void update_rejects_path_payload_mismatch_400() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-mismatch";
        created.add(name);

        adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");
        JSON j = adminGETJSON(BASE + "/security/authproviders/" + name + ".json");
        String id = unwrapSingle((JSONObject) j).getString("id");

        String other = name + "-other";
        JSONObject inner = new JSONObject();
        inner.put("id", id);
        inner.put("name", other); // mismatch on purpose
        inner.put("className", CLASSNAME_UP);
        inner.put("userGroupServiceName", "default");
        JSONObject root = new JSONObject();
        root.put("authprovider", inner);

        MockHttpServletResponse up =
                adminPUT(BASE + "/security/authproviders/" + name, json(root.toString()), "application/json", 400);
        assertThat(up.getContentAsString(), containsString("path name and payload name differ"));
    }

    /** Out-of-range update position is rejected with 400. */
    @Test
    public void update_position_out_of_range_400() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-posoor";
        created.add(name);
        adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");

        JSON j = adminGETJSON(BASE + "/security/authproviders/" + name + ".json");
        String id = unwrapSingle((JSONObject) j).getString("id");

        MockHttpServletResponse up = adminPUT(
                BASE + "/security/authproviders/" + name + "?position=9999",
                json(jsonUpdate(id, name)),
                "application/json",
                400);
        assertThat(up.getContentAsString(), containsString("position"));
    }

    // ---------------------------------------------------------------------
    // Tests: ordering
    // ---------------------------------------------------------------------

    /** PUT /order enables and disables providers according to the submitted list. */
    @Test
    public void order_put_enables_and_disables() throws Exception {
        String a = "MARSHAL-" + System.nanoTime() + "-ordA";
        String b = "MARSHAL-" + System.nanoTime() + "-ordB";
        created.add(a);
        created.add(b);

        adminPOST(BASE + "/security/authproviders", json(jsonCreate(a)), "application/json");
        adminPOST(BASE + "/security/authproviders", json(jsonCreate(b)), "application/json");

        adminPUT(
                BASE + "/security/authproviders/order",
                json("{\"order\":[\"" + a + "\",\"" + b + "\"]}"),
                "application/json",
                200);

        JSON j1 = adminGETJSON(BASE + "/security/authproviders.json");
        List<String> order1 = names(unwrapList(j1));
        assertThat(order1, contains(a, b));

        adminPUT(BASE + "/security/authproviders/order", json("{\"order\":[\"" + b + "\"]}"), "application/json", 200);

        JSON j2 = adminGETJSON(BASE + "/security/authproviders.json");
        List<String> order2 = names(unwrapList(j2));
        assertThat(order2, contains(b));
        assertThat(order2, not(hasItem(a)));
    }

    // ---------------------------------------------------------------------
    // Tests: delete
    // ---------------------------------------------------------------------

    /** DELETE removes the provider from the list and from subsequent listings. */
    @Test
    public void delete_removes_provider_from_list() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-del";
        created.add(name);
        adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");

        MockHttpServletResponse del = adminDELETE(BASE + "/security/authproviders/" + name);
        assertEquals(200, del.getStatus());
        created.remove(name); // already deleted

        JSON j = adminGETJSON(BASE + "/security/authproviders.json");
        JSONArray items = unwrapList(j);
        boolean present = false;
        for (Object o : items) if (name.equals(((JSONObject) o).optString("name"))) present = true;
        assertFalse(present);
    }

    /** DELETE without authentication is forbidden (403). */
    @Test
    public void delete_forbidden_without_auth_403() throws Exception {
        String name = "MARSHAL-" + System.nanoTime() + "-del403";
        created.add(name);

        adminPOST(BASE + "/security/authproviders", json(jsonCreate(name)), "application/json");

        clearAuth(); // simulate no user
        MockHttpServletRequest req = createRequest(BASE + "/security/authproviders/" + name);
        req.setMethod("DELETE");
        MockHttpServletResponse r = dispatch(req);
        assertEquals(403, r.getStatus());

        asAdmin(); // restore for rest of the suite
    }

    // ---------------------------------------------------------------------
    // Tests: security / method constraints
    // ---------------------------------------------------------------------

    /** GET /security/authproviders without authentication is forbidden (403). */
    @Test
    public void list_not_authorised_403() throws Exception {
        clearAuth(); // IMPORTANT: @Before logs in as admin; clear it here
        MockHttpServletRequest req = createRequest(BASE + "/security/authproviders");
        req.setMethod("GET");
        MockHttpServletResponse r = dispatch(req);
        assertEquals(403, r.getStatus());
    }

    /** GET/POST/DELETE on /order are not allowed; only PUT is supported. */
    @Test
    public void order_wrong_methods_405() throws Exception {
        // GET
        MockHttpServletRequest g = createRequest(BASE + "/security/authproviders/order");
        g.setMethod("GET");
        assertEquals(405, dispatch(g).getStatus());
        // POST
        MockHttpServletRequest p = createRequest(BASE + "/security/authproviders/order");
        p.setMethod("POST");
        assertEquals(405, dispatch(p).getStatus());
        // DELETE
        MockHttpServletRequest d = createRequest(BASE + "/security/authproviders/order");
        d.setMethod("DELETE");
        assertEquals(405, dispatch(d).getStatus());
    }
}
