/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.security.xml.UserGroupServiceSummary;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.xml.XMLUserGroupServiceConfig;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

public class UserGroupServiceControllerTest extends GeoServerTestSupport {

    private static final String TEST_SERVICE_PREFIX = "UGS-TEST-";

    private UserGroupServiceController controller;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        controller = applicationContext.getBean(UserGroupServiceController.class);
    }

    @Before
    public void cleanupTestServices() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        // remove any previously-created throwaway services
        secMgr.listUserGroupServices().stream()
                .filter(name -> name.startsWith(TEST_SERVICE_PREFIX))
                .forEach(name -> {
                    try {
                        SecurityUserGroupServiceConfig cfg = secMgr.loadUserGroupServiceConfig(name);
                        if (cfg != null) {
                            secMgr.removeUserGroupService(cfg);
                        }
                    } catch (Exception e) {
                        fail("Cannot remove test user/group service '" + name + "': " + e.getMessage());
                    }
                });
    }

    // --------------------------
    // LIST
    // --------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void testList() {
        setAdmin();
        try {
            RestWrapper<UserGroupServiceSummary> result = controller.list();
            assertNotNull(result.getObject());
            List<UserGroupServiceSummary> list = (List<UserGroupServiceSummary>) result.getObject();
            list.forEach(s -> {
                assertNotNull(s.getName());
                assertNotNull(s.getCls());
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testList_NotAuthorized() {
        SecurityContextHolder.clearContext();
        try {
            controller.list();
            fail("Expected 403 FORBIDDEN when not authorized");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }

    // --------------------------
    // VIEW
    // --------------------------

    @Test
    public void testView() throws Exception {
        setAdmin();
        try {
            // create a throwaway service first so we can view it
            String name = TEST_SERVICE_PREFIX + "create-" + UUID.randomUUID();
            SecurityUserGroupServiceConfig created = createServiceFromDefault(name);

            RestWrapper<SecurityUserGroupServiceConfig> result = controller.view(created.getName());
            assertNotNull(result.getObject());

            SecurityUserGroupServiceConfig body = (SecurityUserGroupServiceConfig) result.getObject();
            assertEquals("Expected the same name", created.getName(), body.getName());
            assertEquals("Expected same className", created.getClassName(), body.getClassName());
            assertNotNull("Expected id to be set", body.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testView_NotAuthorized() throws Exception {
        // 1) create the resource as admin
        String name = TEST_SERVICE_PREFIX + "view-" + UUID.randomUUID();
        setAdmin();
        createServiceFromDefault(name);
        SecurityContextHolder.clearContext();

        // 2) now, without auth, viewing must be 403
        try {
            controller.view(name);
            fail("Expected 403 FORBIDDEN when not authorized");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }

    @Test
    public void testDelete_NotAuthorised() throws Exception {
        // 1) create the resource as admin
        String name = TEST_SERVICE_PREFIX + "delete-na-" + UUID.randomUUID();
        setAdmin();
        createServiceFromDefault(name);
        SecurityContextHolder.clearContext();

        // 2) now, without auth, deleting must be 403
        try {
            controller.delete(name);
            fail("Expected 403 FORBIDDEN when not authorized");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }

    @Test
    public void testView_NotFound() {
        setAdmin();
        try {
            String name = TEST_SERVICE_PREFIX + "missing-" + UUID.randomUUID();
            try {
                controller.view(name);
                fail("Expected 404 NOT_FOUND for missing service");
            } catch (HttpClientErrorException e) {
                assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // --------------------------
    // CREATE (POST)
    // --------------------------

    @Test
    public void testCreate_FromDefaultXmlService() throws Exception {
        setAdmin();
        try {
            String name = TEST_SERVICE_PREFIX + "create-" + UUID.randomUUID();

            SecurityUserGroupServiceConfig req = cloneDefaultWithName(name);
            // IMPORTANT: no id on POST (let GS assign one)
            req.setId(null);

            RestWrapper<SecurityUserGroupServiceConfig> post = controller.post(req, UriComponentsBuilder.newInstance());

            SecurityUserGroupServiceConfig saved =
                    (SecurityUserGroupServiceConfig) Objects.requireNonNull(post.getObject());
            assertEquals(name, saved.getName());
            assertNotNull(saved.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testCreate_MissingName() throws Exception {
        setAdmin();
        try {
            SecurityUserGroupServiceConfig req = cloneDefaultWithName(null);
            // no name
            try {
                controller.post(req, UriComponentsBuilder.newInstance());
                fail("Expected 400 BAD_REQUEST for missing name");
            } catch (HttpClientErrorException e) {
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testCreate_DuplicateName() throws Exception {
        setAdmin();
        try {
            String name = TEST_SERVICE_PREFIX + "dup-" + UUID.randomUUID();
            // first creation
            SecurityUserGroupServiceConfig first = cloneDefaultWithName(name);
            first.setId(null);
            controller.post(first, UriComponentsBuilder.newInstance());

            // second creation with same name → expect 400 BAD_REQUEST
            SecurityUserGroupServiceConfig dup = cloneDefaultWithName(name);
            dup.setId(null);
            try {
                controller.post(dup, UriComponentsBuilder.newInstance());
                fail("Expected 400 BAD_REQUEST for duplicate name");
            } catch (HttpClientErrorException e) {
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // --------------------------
    // UPDATE (PUT)
    // --------------------------

    @Test
    public void testUpdate() throws Exception {
        setAdmin();
        try {
            String name = TEST_SERVICE_PREFIX + "update-" + UUID.randomUUID();

            // create first
            SecurityUserGroupServiceConfig created = createServiceFromDefault(name);

            // load current, tweak a simple field that is safe to change
            SecurityUserGroupServiceConfig toUpdate = getSecurityManager().loadUserGroupServiceConfig(name);
            assertNotNull(toUpdate);
            // round-trip update
            controller.put(name, toUpdate);

            // verify still there
            RestWrapper<SecurityUserGroupServiceConfig> getResult = controller.view(name);
            SecurityUserGroupServiceConfig body = (SecurityUserGroupServiceConfig) getResult.getObject();
            assertNotNull(body);
            assertEquals(name, body.getName());
            assertEquals(created.getClassName(), body.getClassName());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // --------------------------
    // DELETE
    // --------------------------

    @Test
    public void testDelete() throws Exception {
        setAdmin();
        try {
            String name = TEST_SERVICE_PREFIX + "delete-" + UUID.randomUUID();
            createServiceFromDefault(name);

            controller.delete(name);

            try {
                controller.view(name);
                fail("Expected 404 NOT_FOUND after delete");
            } catch (HttpClientErrorException e) {
                assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDelete_BlacklistedDefault() {
        setAdmin();
        try {
            try {
                controller.delete("default");
                fail("Expected 400 BAD_REQUEST for blacklisted 'default'");
            } catch (HttpClientErrorException e) {
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // --------------------------
    // Helpers
    // --------------------------

    // Creates a brand-new user/group service by cloning the built-in 'default' config and POSTing it.
    private SecurityUserGroupServiceConfig createServiceFromDefault(String name) throws Exception {
        SecurityUserGroupServiceConfig req = cloneDefaultWithName(name);
        req.setId(null); // ensure POST assigns a new id
        RestWrapper<SecurityUserGroupServiceConfig> post = controller.post(req, UriComponentsBuilder.newInstance());
        return (SecurityUserGroupServiceConfig) post.getObject();
    }

    // Deep-clone 'default' via XStreamPersisterFactory, override name/id, set XML fileName if needed.
    private SecurityUserGroupServiceConfig cloneDefaultWithName(String name) throws Exception {
        GeoServerSecurityManager mgr = getSecurityManager();
        SecurityUserGroupServiceConfig base = mgr.loadUserGroupServiceConfig("default");
        if (base == null) {
            fail("Default user/group service not found; test environment is not correctly initialized.");
        }

        // Obtain the factory from Spring (preferred), with a safe fallback
        XStreamPersisterFactory xpf = null;
        if (applicationContext != null) {
            xpf = applicationContext.getBean(XStreamPersisterFactory.class);
        }
        if (xpf == null) {
            xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        }
        if (xpf == null) {
            // last resort (initializers may be missing, but okay for tests)
            xpf = new XStreamPersisterFactory();
        }

        XStreamPersister xp = xpf.createXMLPersister();
        xp.getXStream()
                .allowTypesByWildcard(new String[] {"org.geoserver.security.**", "org.geoserver.security.config.**"});

        // serialize base
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        xp.save(base, bout);
        byte[] xml = bout.toString(StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);

        // deserialize to a fresh instance (preserves concrete subclass)
        SecurityUserGroupServiceConfig copy =
                xp.load(new ByteArrayInputStream(xml), SecurityUserGroupServiceConfig.class);

        // override required fields
        copy.setName(name);
        copy.setId(null); // POST will assign

        // XML implementation requires a fileName
        if (copy instanceof XMLUserGroupServiceConfig config) {
            config.setFileName(name + ".xml");
        }

        return copy;
    }

    private void setAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "geoserver", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
