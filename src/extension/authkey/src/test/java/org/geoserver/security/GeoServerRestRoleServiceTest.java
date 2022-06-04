/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.google.common.util.concurrent.ExecutionError;
import java.io.IOException;
import java.util.SortedSet;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerRestRoleServiceTest extends GeoServerSystemTestSupport {

    public static final String uri = "http://rest.geoserver.org";

    private RestTemplate template;

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() throws Exception {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
        GeoServerEnvironment.reloadAllowEnvParametrization();

        System.setProperty("test_role_service_base_url", "http://example.com/rest");
        System.setProperty("test_role_service_auth_api_key", "letmein");

        template = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(template);

        mockServer
                .expect(requestTo(uri + "/api/roles"))
                .andRespond(
                        withSuccess(
                                "{\"groups\": [\"anonymous\", \"test\", \"admin\"]}",
                                MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo(uri + "/api/adminRole"))
                .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo(uri + "/api/users/test"))
                .andRespond(
                        withSuccess(
                                "{\"users\": [{\"username\": \"test\", \"groups\": [\"test\"]}]}",
                                MediaType.APPLICATION_JSON));

        // Not needed anymore thanks to the internal cache
        /* mockServer.expect(requestTo(uri + "/api/adminRole"))
        .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON)); */

        mockServer
                .expect(requestTo(uri + "/api/users/test@geoserver.org"))
                .andRespond(
                        withSuccess(
                                "{\"users\": [{\"username\": \"test@geoserver.org\", \"groups\": [\"test\"]}]}",
                                MediaType.APPLICATION_JSON));

        // Not needed anymore thanks to the internal cache
        /* mockServer.expect(requestTo(uri + "/api/adminRole"))
        .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON)); */

        mockServer
                .expect(requestTo(uri + "/api/users/admin"))
                .andRespond(
                        withSuccess(
                                "{\"users\": [{\"username\": \"admin\", \"groups\": [\"admin\"]}]}",
                                MediaType.APPLICATION_JSON));

        mockServer
                .expect(requestTo("http://example.com/api/adminRole"))
                .andExpect(header("Authorization", "ApiKey letmein"))
                .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON));
    }

    @After
    public void tearDown() {
        System.clearProperty("ALLOW_ENV_PARAMETRIZATION");
        System.clearProperty("test_role_service_base_url");
        System.clearProperty("test_role_service_auth_api_key");
    }

    @Test
    public void testGeoServerRestRoleService() throws IOException {
        GeoServerRestRoleServiceConfig roleServiceconfig = new GeoServerRestRoleServiceConfig();
        roleServiceconfig.setBaseUrl(uri);

        GeoServerRestRoleService roleService = new GeoServerRestRoleService(roleServiceconfig);
        roleService.setRestTemplate(template);

        final SortedSet<GeoServerRole> roles = roleService.getRoles();
        final GeoServerRole adminRole = roleService.getAdminRole();
        final SortedSet<GeoServerRole> testUserRoles = roleService.getRolesForUser("test");
        final SortedSet<GeoServerRole> testUserEmailRoles =
                roleService.getRolesForUser("test@geoserver.org");
        final SortedSet<GeoServerRole> adminUserRoles = roleService.getRolesForUser("admin");

        assertNotNull(roles);
        assertNotNull(adminRole);
        assertNotNull(testUserRoles);
        assertNotNull(testUserEmailRoles);
        assertNotNull(adminUserRoles);

        assertEquals(3, roles.size());
        assertEquals("ROLE_ADMIN", adminRole.getAuthority());
        assertEquals(testUserEmailRoles.size(), testUserRoles.size());
        assertFalse(testUserRoles.contains(GeoServerRole.ADMIN_ROLE));
        assertFalse(testUserRoles.contains(adminRole));
        assertTrue(adminUserRoles.contains(GeoServerRole.ADMIN_ROLE));

        roleServiceconfig.setBaseUrl("${test_role_service_base_url}");
        roleServiceconfig.setAuthApiKey("${test_role_service_auth_api_key}");
        final GeoServerRole adminRole1 = roleService.getAdminRole();
        assertNotNull(adminRole1);
        assertEquals("ROLE_ADMIN", adminRole1.getAuthority());

        mockServer.verify();
    }

    @Test
    public void testGeoServerRestRoleServiceInternalCache()
            throws IOException, InterruptedException {
        GeoServerRestRoleServiceConfig roleServiceconfig = new GeoServerRestRoleServiceConfig();
        int EXPIRATION = 500;
        roleServiceconfig.setCacheExpirationTime(EXPIRATION);
        roleServiceconfig.setBaseUrl(uri);

        GeoServerRestRoleService roleService = new GeoServerRestRoleService(roleServiceconfig);
        roleService.setRestTemplate(template);

        roleService.getRoles();
        roleService.getAdminRole();
        roleService.getRolesForUser("test");
        Thread.sleep((long) (EXPIRATION * 1.5));
        assertThrows(ExecutionError.class, () -> roleService.getRolesForUser("test@geoserver.org"));
    }
}
