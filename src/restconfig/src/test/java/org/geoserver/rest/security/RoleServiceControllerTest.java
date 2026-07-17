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
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestException;
import org.geoserver.rest.security.xml.RoleServiceSummary;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;
import org.geoserver.security.impl.MemoryRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class RoleServiceControllerTest extends GeoServerTestSupport {

    private static final String TEST_SERVICE_PREFIX = "RS-TEST-";

    private RoleServiceController controller;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        controller = applicationContext.getBean(RoleServiceController.class);
    }

    @Before
    public void cleanupTestServices() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        // remove any previously-created throwaway services
        secMgr.listRoleServices().stream()
                .filter(name -> name.startsWith(TEST_SERVICE_PREFIX))
                .forEach(name -> {
                    try {
                        SecurityRoleServiceConfig cfg = secMgr.loadRoleServiceConfig(name);
                        if (cfg != null) {
                            secMgr.removeRoleService(cfg);
                        }
                    } catch (Exception e) {
                        fail("Cannot remove test role service '" + name + "': " + e.getMessage());
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
            RestWrapper<RoleServiceSummary> result = controller.list();
            assertNotNull(result.getObject());
            List<RoleServiceSummary> list = (List<RoleServiceSummary>) result.getObject();
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
        } catch (RestException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
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
            SecurityRoleServiceConfig created = createServiceFromDefault(name);

            RestWrapper<SecurityRoleServiceConfig> result = controller.view(created.getName());
            assertNotNull(result.getObject());

            SecurityRoleServiceConfig body = (SecurityRoleServiceConfig) result.getObject();
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
        } catch (RestException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
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
        } catch (RestException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
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
            } catch (ResourceNotFoundException e) {
                assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
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

            SecurityRoleServiceConfig req = cloneDefaultWithName(name);
            // IMPORTANT: no id on POST (let GS assign one)
            req.setId(null);

            RestWrapper<SecurityRoleServiceConfig> post = controller.post(req);

            SecurityRoleServiceConfig saved = (SecurityRoleServiceConfig) Objects.requireNonNull(post.getObject());
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
            SecurityRoleServiceConfig req = cloneDefaultWithName(null);
            // no name
            try {
                controller.post(req);
                fail("Expected 400 BAD_REQUEST for missing name");
            } catch (RestException e) {
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
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
            SecurityRoleServiceConfig first = cloneDefaultWithName(name);
            first.setId(null);
            controller.post(first);

            // second creation with same name → expect 400 BAD_REQUEST
            SecurityRoleServiceConfig dup = cloneDefaultWithName(name);
            dup.setId(null);
            try {
                controller.post(dup);
                fail("Expected 400 BAD_REQUEST for duplicate name");
            } catch (RestException e) {
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
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
            SecurityRoleServiceConfig created = createServiceFromDefault(name);

            // load current, tweak a simple field that is safe to change
            SecurityRoleServiceConfig toUpdate = getSecurityManager().loadRoleServiceConfig(name);
            assertNotNull(toUpdate);
            // round-trip update
            controller.put(name, toUpdate);

            // verify still there
            RestWrapper<SecurityRoleServiceConfig> getResult = controller.view(name);
            SecurityRoleServiceConfig body = (SecurityRoleServiceConfig) getResult.getObject();
            assertNotNull(body);
            assertEquals(name, body.getName());
            assertEquals(created.getClassName(), body.getClassName());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testUpdate_ActiveServiceClassChangeRejected() throws Exception {
        setAdmin();
        GeoServerSecurityManager secMgr = getSecurityManager();
        String name = TEST_SERVICE_PREFIX + "active-class-" + UUID.randomUUID();
        String originalActiveRoleService = secMgr.loadSecurityConfig().getRoleServiceName();
        try {
            SecurityRoleServiceConfig created = createServiceFromDefault(name);

            SecurityManagerConfig mconfig = secMgr.loadSecurityConfig();
            mconfig.setRoleServiceName(name);
            secMgr.saveSecurityConfig(mconfig);

            MemoryRoleServiceConfigImpl swapped = new MemoryRoleServiceConfigImpl();
            swapped.setId(created.getId());
            swapped.setName(name);
            swapped.setClassName(MemoryRoleService.class.getName());

            try {
                controller.put(name, swapped);
                fail("Expected 400 BAD_REQUEST when changing the active role service's implementation class");
            } catch (RestException e) {
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
            }
        } finally {
            // restore the original active role service before the next test's cleanup runs,
            // since the manager refuses to remove whichever role service is currently active
            SecurityManagerConfig restore = secMgr.loadSecurityConfig();
            restore.setRoleServiceName(originalActiveRoleService);
            secMgr.saveSecurityConfig(restore);
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
            } catch (ResourceNotFoundException e) {
                assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
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
            } catch (RestException e) {
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDelete_UsedByFilter() throws Exception {
        setAdmin();
        String name = TEST_SERVICE_PREFIX + "used-by-filter-" + UUID.randomUUID();
        String filterName = "RS-TEST-FILTER-" + UUID.randomUUID();
        GeoServerSecurityManager secMgr = getSecurityManager();
        try {
            createServiceFromDefault(name);

            RequestHeaderAuthenticationFilterConfig filterConfig = new RequestHeaderAuthenticationFilterConfig();
            filterConfig.setName(filterName);
            filterConfig.setClassName(GeoServerRequestHeaderAuthenticationFilter.class.getName());
            filterConfig.setPrincipalHeaderAttribute("principal");
            filterConfig.setRoleSource(
                    PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.RoleService);
            filterConfig.setRoleServiceName(name);
            secMgr.saveFilter(filterConfig);

            try {
                controller.delete(name);
                fail("Expected 400 BAD_REQUEST for a role service still referenced by a filter");
            } catch (RestException e) {
                assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
            }
        } finally {
            SecurityNamedServiceConfig savedFilterConfig = secMgr.loadFilterConfig(filterName, true);
            if (savedFilterConfig != null) {
                secMgr.removeFilter(savedFilterConfig);
            }
            SecurityContextHolder.clearContext();
        }
    }

    // --------------------------
    // Helpers
    // --------------------------

    // Creates a brand-new role service by cloning the built-in 'default' config and POSTing it.
    private SecurityRoleServiceConfig createServiceFromDefault(String name) throws Exception {
        SecurityRoleServiceConfig req = cloneDefaultWithName(name);
        req.setId(null); // ensure POST assigns a new id
        RestWrapper<SecurityRoleServiceConfig> post = controller.post(req);
        return (SecurityRoleServiceConfig) post.getObject();
    }

    // Deep-clone 'default' via XStreamPersisterFactory, override name/id, set XML fileName if needed.
    private SecurityRoleServiceConfig cloneDefaultWithName(String name) throws Exception {
        GeoServerSecurityManager mgr = getSecurityManager();
        SecurityRoleServiceConfig base = mgr.loadRoleServiceConfig("default");
        if (base == null) {
            fail("Default role service not found; test environment is not correctly initialized.");
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
        SecurityRoleServiceConfig copy = xp.load(new ByteArrayInputStream(xml), SecurityRoleServiceConfig.class);

        // override required fields
        copy.setName(name);
        copy.setId(null); // POST will assign

        // XML implementation requires a fileName
        if (copy instanceof XMLRoleServiceConfig config) {
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
