/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.google.common.util.concurrent.ExecutionError;
import java.io.IOException;
import java.util.SortedSet;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerRestRoleServiceTest {

    public static final String uri = "http://rest.geoserver.org";

    private RestTemplate template;

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() throws Exception {
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
                .expect(requestTo(uri + "/api/adminRole"))
                .andRespond(withSuccess("{\"adminRole\": \"admin\"}", MediaType.APPLICATION_JSON));
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
        assertTrue(!testUserRoles.contains(GeoServerRole.ADMIN_ROLE));
        assertTrue(!testUserRoles.contains(adminRole));
        assertTrue(adminUserRoles.contains(GeoServerRole.ADMIN_ROLE));
    }

    @Test
    public void testGeoServerRestRoleServiceInternalCache()
            throws IOException, InterruptedException {
        GeoServerRestRoleServiceConfig roleServiceconfig = new GeoServerRestRoleServiceConfig();
        roleServiceconfig.setBaseUrl(uri);

        GeoServerRestRoleService roleService = new GeoServerRestRoleService(roleServiceconfig);
        roleService.setRestTemplate(template);

        roleService.getRoles();
        roleService.getAdminRole();
        roleService.getRolesForUser("test");
        Thread.sleep(31 * 1000);
        try {
            roleService.getRolesForUser("test@geoserver.org");
            fail("Expecting ExecutionError to be thrown");
        } catch (ExecutionError e) {
            // OK
        }
    }
}
