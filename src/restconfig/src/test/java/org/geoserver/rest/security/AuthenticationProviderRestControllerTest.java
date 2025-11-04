/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.rest.security.xml.AuthProviderCollection;
import org.geoserver.rest.security.xml.AuthProviderOrder;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Integration-style tests for {@link AuthenticationProviderRestController}.
 *
 * <p>Notes:
 *
 * <ul>
 *   <li>Tests call the controller directly (no MockMvc) to keep them fast and focused on config semantics.
 *   <li>Each test starts from a clean state by removing TEST-* providers from both the on-disk provider list and the
 *       enabled order.
 *   <li>Where relevant, we verify both JSON/XML payload handling and the side effects on &lt;authproviderNames&gt;
 *       (enabled/disabled semantics).
 * </ul>
 */
public class AuthenticationProviderRestControllerTest extends GeoServerTestSupport {

    private static final String TEST_PROVIDER_PREFIX = "TEST-";

    private AuthenticationProviderRestController controller;
    private AuthenticationProviderHelper authenticationProviderHelper;

    /** Track providers created by each test so we can remove them in @After. */
    private final Set<String> created = new LinkedHashSet<>();

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        controller = applicationContext.getBean(AuthenticationProviderRestController.class);
        authenticationProviderHelper = new AuthenticationProviderHelper(getSecurityManager());
    }

    /**
     * Hard reset before each test: - remove TEST-* providers from the active order and from disk - reload security -
     * clear auth
     */
    @Before
    public void resetProviders() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        // 1) Remove any test providers from enabled order
        SecurityManagerConfig smc = secMgr.loadSecurityConfig();
        smc.getAuthProviderNames().removeIf(name -> name != null && name.startsWith(TEST_PROVIDER_PREFIX));
        secMgr.saveSecurityConfig(smc);

        // 2) Remove saved provider configs
        for (String name : new ArrayList<>(secMgr.listAuthenticationProviders())) {
            if (name != null && name.startsWith(TEST_PROVIDER_PREFIX)) {
                try {
                    SecurityAuthProviderConfig cfg = secMgr.loadAuthenticationProviderConfig(name);
                    if (cfg != null) {
                        secMgr.removeAuthenticationProvider(cfg);
                    }
                } catch (Exception e) {
                    fail("Cannot remove security provider '" + name + "': " + e.getMessage());
                }
            }
        }

        secMgr.reload();
        created.clear();
        SecurityContextHolder.clearContext();
    }

    /** Safe teardown: restore order to ["default"], delete created providers, clear auth. */
    @Override
    @After
    public void tearDownInternal() throws Exception {
        try {
            setUser(); // ensure admin for cleanup

            // Restore order to ["default"] (ignore if "default" is unknown)
            try {
                controller.reorder(jsonRequest(Collections.singletonMap("order", List.of("default"))));
            } catch (RuntimeException e) {
                // not fatal for cleanup; tests may have removed "default"
            }

            // Delete any providers created by the test
            for (String name : created) {
                try {
                    controller.delete(name);
                } catch (RuntimeException ignore) {
                    // already gone or never persisted; fine
                }
            }
        } finally {
            created.clear();
            SecurityContextHolder.clearContext();
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private String generateName(String suffix) {
        return TEST_PROVIDER_PREFIX + System.nanoTime() + "-" + suffix;
    }

    private void setUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private MockHttpServletRequest jsonRequest(Object jsonPayload) throws IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setContentType(MediaType.APPLICATION_JSON_VALUE);
        req.setContent(MAPPER.writeValueAsBytes(jsonPayload));
        return req;
    }

    private MockHttpServletRequest xmlRequest(Object beanToMarshal) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setContentType(MediaType.APPLICATION_XML_VALUE);
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        controller.configurePersister(xp, null);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        xp.save(beanToMarshal, bout);
        req.setContent(bout.toByteArray());
        return req;
    }

    private String toXml(Object obj) throws Exception {
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        controller.configurePersister(xp, null);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        xp.save(obj, bout);
        return bout.toString(StandardCharsets.UTF_8);
    }

    private Map<String, Object> providerJson(String name, String className, String ugService) {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("name", name);
        inner.put("className", className);
        if (ugService != null) inner.put("userGroupServiceName", ugService);
        return Collections.singletonMap("authprovider", inner);
    }

    // ---------------------------------------------------------------------
    // list / one
    // ---------------------------------------------------------------------

    /** list(): returns providers in enabled order; XML wrapper shape is correct. */
    @Test
    public void testList_JSON_and_XML_structure() throws Exception {
        try {
            setUser();
            String n1 = generateName("list1");
            String n2 = generateName("list2");
            authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(n1, true);
            authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(n2, true);
            created.add(n1);
            created.add(n2);

            RestWrapper<AuthProviderCollection> result = controller.list();
            assertNotNull(result);
            AuthProviderCollection col = (AuthProviderCollection) result.getObject();
            assertNotNull(col);

            List<String> names = getSecurityManager().loadSecurityConfig().getAuthProviderNames();
            assertThat(names, hasItems(n1, n2));

            String xml = toXml(col);
            assertThat(xml, containsString("<authproviders>"));
            assertThat(
                    xml,
                    containsString("<org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** one(name): returns the provider, and single-object XML element is the FQN. */
    @Test
    public void testOne_XML_structure_and_fields() throws Exception {
        try {
            setUser();
            String name = generateName("view");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
            created.add(name);

            RestWrapper<SecurityAuthProviderConfig> wrap = controller.one(provider.getName());
            SecurityAuthProviderConfig cfg = (SecurityAuthProviderConfig) wrap.getObject();
            assertNotNull(cfg);
            assertEquals(provider.getName(), cfg.getName());
            assertNotNull(cfg.getId());
            assertNotNull(cfg.getClassName());
            assertNotNull(cfg.getUserGroupServiceName());

            String xml = toXml(cfg);
            assertThat(
                    xml.trim(),
                    startsWith("<org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>"));
            assertThat(xml, containsString("<name>" + provider.getName() + "</name>"));
            assertThat(
                    xml,
                    containsString(
                            "<className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ---------------------------------------------------------------------
    // create
    // ---------------------------------------------------------------------

    /** create(JSON): 201 + Location header + persisted object. */
    @Test
    public void testCreate_JSON_setsLocationHeader_and201() throws Exception {
        try {
            setUser();
            String name = generateName("create-json");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);

            ResponseEntity<RestWrapper<SecurityAuthProviderConfig>> resp = controller.create(
                    jsonRequest(providerJson(
                            provider.getName(), provider.getClassName(), provider.getUserGroupServiceName())),
                    null,
                    UriComponentsBuilder.newInstance());

            assertEquals(201, resp.getStatusCode().value());
            HttpHeaders headers = resp.getHeaders();
            assertNotNull(headers.getLocation());
            assertThat(headers.getLocation().getPath(), is("/security/authproviders/" + name));

            SecurityAuthProviderConfig createdCfg =
                    (SecurityAuthProviderConfig) resp.getBody().getObject();
            assertEquals(name, createdCfg.getName());
            assertNotNull(createdCfg.getId());
            created.add(name);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** create(XML): round-trip; position=0 places it first in &lt;authproviderNames&gt;. */
    @Test
    public void testCreate_XML_roundtrip() throws Exception {
        try {
            setUser();
            String name = generateName("create-xml");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);

            ResponseEntity<RestWrapper<SecurityAuthProviderConfig>> resp =
                    controller.create(xmlRequest(provider), 0, UriComponentsBuilder.newInstance());
            assertEquals(201, resp.getStatusCode().value());
            created.add(name);

            List<String> names = getSecurityManager().loadSecurityConfig().getAuthProviderNames();
            assertThat(names.get(0), is(name));

            String xml = toXml(resp.getBody().getObject());
            assertThat(xml, containsString("<name>" + name + "</name>"));
            assertThat(
                    xml,
                    containsString(
                            "<className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** create(JSON): reserved name "order" must be rejected with 400. */
    @Test(expected = IllegalArgumentException.class)
    public void testCreate_reservedName_order_isRejected() throws Exception {
        try {
            setUser();
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig("order", false);
            controller.create(jsonRequest(provider), null, UriComponentsBuilder.newInstance());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** create(JSON): duplicate provider name must be rejected with DuplicateProviderName (400). */
    @Test(expected = AuthenticationProviderRestController.DuplicateProviderName.class)
    public void testCreate_duplicateNameRejected() throws Exception {
        setUser();
        String name = generateName("dup");
        UsernamePasswordAuthenticationProviderConfig p =
                authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);

        controller.create(
                jsonRequest(Collections.singletonMap("authprovider", p)), null, UriComponentsBuilder.newInstance());
        created.add(name); // first create succeeded; track it

        controller.create(
                jsonRequest(Collections.singletonMap("authprovider", p)), null, UriComponentsBuilder.newInstance());
    }

    /** create(JSON): missing className -> 400 BadRequest. */
    @Test(expected = AuthenticationProviderRestController.BadRequest.class)
    public void testCreate_JSON_missingClassName() throws Exception {
        setUser();
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("name", generateName("bad"));
        controller.create(
                jsonRequest(Collections.singletonMap("authprovider", inner)), null, UriComponentsBuilder.newInstance());
    }

    /** create(JSON): position out of range -> 400 BadRequest. */
    @Test(expected = AuthenticationProviderRestController.BadRequest.class)
    public void testCreate_positionOutOfRange_isBadRequest() throws Exception {
        setUser();
        String name = generateName("pos");
        UsernamePasswordAuthenticationProviderConfig p =
                authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
        controller.create(
                jsonRequest(Collections.singletonMap("authprovider", p)), 9999, UriComponentsBuilder.newInstance());
    }

    // ---------------------------------------------------------------------
    // update
    // ---------------------------------------------------------------------

    /** update(..., position=1): reorders and persists &lt;authproviderNames&gt; without touching IDs/classes. */
    @Test
    public void testUpdate_movePosition_andPersist() throws Exception {
        try {
            setUser();
            String a = generateName("a");
            String b = generateName("b");

            authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(a, true);
            authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(b, true);
            created.add(a);
            created.add(b);

            // initial order contains "default"
            List<String> before = getSecurityManager().loadSecurityConfig().getAuthProviderNames();
            assertThat(before, is(Arrays.asList("default", a, b)));

            // normalize active order to only our test providers
            controller.reorder(jsonRequest(Collections.singletonMap("order", Arrays.asList(a, b))));

            // move 'a' to position 1
            UsernamePasswordAuthenticationProviderConfig providerA =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(a, false);
            RestWrapper<SecurityAuthProviderConfig> updated =
                    controller.update(a, jsonRequest(Collections.singletonMap("authprovider", providerA)), 1);
            assertEquals(a, ((SecurityAuthProviderConfig) updated.getObject()).getName());

            List<String> after = getSecurityManager().loadSecurityConfig().getAuthProviderNames();
            assertThat(after, is(Arrays.asList(b, a)));

            // enabled == names present in <authproviderNames>
            Set<String> enabled = new LinkedHashSet<>(after);
            Set<String> saved = getSecurityManager().listAuthenticationProviders();
            assertThat(enabled, is(new LinkedHashSet<>(Arrays.asList(b, a))));
            assertTrue(saved.contains(a));
            assertTrue(saved.contains(b));
            assertFalse(enabled.contains("default"));

            // disable 'a' by ordering to [b]
            controller.reorder(jsonRequest(Collections.singletonMap("order", List.of(b))));
            List<String> onlyB = getSecurityManager().loadSecurityConfig().getAuthProviderNames();
            assertThat(onlyB, is(List.of(b)));
            assertTrue(getSecurityManager().listAuthenticationProviders().contains(a));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** update: path/payload name mismatch -> 400 BadRequest. */
    @Test(expected = AuthenticationProviderRestController.BadRequest.class)
    public void testUpdate_badPathPayloadMismatch() throws Exception {
        try {
            setUser();
            String a = generateName("mismatch");
            authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(a, true);
            created.add(a);

            UsernamePasswordAuthenticationProviderConfig wrong =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(
                            a + "-other", false);
            controller.update(a, jsonRequest(Collections.singletonMap("authprovider", wrong)), null);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** update: changing className must be rejected with 400. */
    @Test(expected = AuthenticationProviderRestController.BadRequest.class)
    public void testUpdate_rejectsClassNameChange() throws Exception {
        setUser();
        String a = generateName("a");
        authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(a, true);
        created.add(a);

        UsernamePasswordAuthenticationProviderConfig payload =
                authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(a, false);
        payload.setClassName("org.geoserver.security.auth.SomeOtherProvider");
        controller.update(a, jsonRequest(Collections.singletonMap("authprovider", payload)), null);
    }

    /** update: out-of-range position -> 400 BadRequest (controller validates/clamps per implementation). */
    @Test(expected = AuthenticationProviderRestController.BadRequest.class)
    public void testUpdate_positionOutOfRange_isBadRequest() throws Exception {
        setUser();
        String a = generateName("a");
        authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(a, true);
        created.add(a);

        UsernamePasswordAuthenticationProviderConfig payload =
                authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(a, false);
        controller.update(a, jsonRequest(Collections.singletonMap("authprovider", payload)), 9999);
    }

    // ---------------------------------------------------------------------
    // delete
    // ---------------------------------------------------------------------

    /** delete(name): removed from config and from disk. */
    @Test
    public void testDelete_removesProvider() throws Exception {
        try {
            setUser();
            String name = generateName("delete");
            authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
            // track then remove
            created.add(name);
            controller.delete(name);
            created.remove(name);

            try {
                controller.one(name);
                fail("Expected ProviderNotFound");
            } catch (AuthenticationProviderRestController.ProviderNotFound expected) {
                // ok
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ---------------------------------------------------------------------
    // reorder
    // ---------------------------------------------------------------------

    /** reorder(XML): sets enabled list exactly as provided. */
    @Test
    public void testOrder_XML_updatesEnabledList() throws Exception {
        setUser();
        String a = generateName("a");
        String b = generateName("b");
        authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(a, true);
        authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(b, true);
        created.add(a);
        created.add(b);

        AuthProviderOrder ord = new AuthProviderOrder();
        ord.setOrder(Arrays.asList(a, b));
        ResponseEntity<Void> resp = controller.reorder(xmlRequest(ord));
        assertEquals(200, resp.getStatusCode().value());

        assertThat(getSecurityManager().loadSecurityConfig().getAuthProviderNames(), is(Arrays.asList(a, b)));
    }

    /** reorder(JSON): unknown name -> 400 BadRequest. */
    @Test(expected = AuthenticationProviderRestController.BadRequest.class)
    public void testOrder_JSON_unknownName_isBadRequest() throws Exception {
        setUser();
        controller.reorder(jsonRequest(Collections.singletonMap("order", List.of("does-not-exist"))));
    }

    // ---------------------------------------------------------------------
    // security / method constraints
    // ---------------------------------------------------------------------

    /** list(): requires admin role. */
    @Test(expected = AuthenticationProviderRestController.NotAuthorised.class)
    public void testList_NoUser_unauthorized() throws Exception {
        try {
            SecurityContextHolder.clearContext();
            controller.list();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** /order GET/POST/DELETE: 405 Method Not Allowed. */
    @Test
    public void testOrder_methods_not_allowed() {
        ResponseEntity<Void> g = controller.orderGetNotAllowed();
        ResponseEntity<Void> p = controller.orderPostNotAllowed();
        ResponseEntity<Void> d = controller.orderDeleteNotAllowed();
        assertEquals(405, g.getStatusCode().value());
        assertEquals(405, p.getStatusCode().value());
        assertEquals(405, d.getStatusCode().value());
    }
}
