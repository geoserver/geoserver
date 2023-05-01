/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * Tests for {@link org.geoserver.security.keycloak.KeycloakRoleService}. Does not test the load
 * method.
 */
public class KeycloakRoleServiceTest {

    private static WireMockServer keycloakSrv;
    private static String authService;

    private KeycloakRoleService service;

    @BeforeClass
    public static void beforeClass() {
        keycloakSrv =
                new WireMockServer(
                        wireMockConfig().dynamicPort().notifier(new ConsoleNotifier(true)));
        keycloakSrv.start();
        authService = "http://localhost:" + keycloakSrv.port();

        keycloakSrv.stubFor(
                WireMock.post(urlEqualTo("/auth/realms/master/protocol/openid-connect/token"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_token_response.json")));

        keycloakSrv.stubFor(
                WireMock.get(urlEqualTo("/auth/admin/realms/master/roles"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_roles.json")));
        keycloakSrv.stubFor(
                WireMock.get(urlEqualTo("/auth/admin/realms/master/clients/client_id1/roles"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_client_roles.json")));

        keycloakSrv.stubFor(
                WireMock.get(urlEqualTo("/auth/admin/realms/master/clients/client_id2/roles"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_client2_roles.json")));

        keycloakSrv.stubFor(
                WireMock.get(
                                urlEqualTo(
                                        "/auth/admin/realms/master/users?exact=true&username=user1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_users.json")));
        keycloakSrv.stubFor(
                WireMock.get(
                                urlEqualTo(
                                        "/auth/admin/realms/master/users/0e72c14e-53d8-4619-a05a-a605dc2102b9/role-mappings/realm"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_user_roles.json")));

        keycloakSrv.stubFor(
                WireMock.get(
                                urlEqualTo(
                                        "/auth/admin/realms/master/users/0e72c14e-53d8-4619-a05a-a605dc2102b9/role-mappings/clients/client_id2"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_client2_roles.json")));

        keycloakSrv.stubFor(
                WireMock.get(urlEqualTo("/auth/admin/realms/master/roles/test-create"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_one_role.json")));

        keycloakSrv.stubFor(
                WireMock.get(urlEqualTo("/auth/admin/realms/master/roles/client2-role2/users"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("keycloak_two_users.json")));
    }

    @Before
    public void before() throws Exception {
        service = new KeycloakRoleService();
        KeycloakRoleServiceConfig config = new KeycloakRoleServiceConfig();
        config.setAdminRoleName("realm-role1");
        config.setGroupAdminRoleName("realm-role1");
        config.setIdsOfClientsList("client_id1,client_id2");
        config.setClientSecret("client_secret");
        config.setClientID("client_id");
        config.setRealm("master");
        config.setServerURL(authService);
        config.setClassName(KeycloakRoleService.class.getName());
        service.initializeFromConfig(config);
    }

    @Test
    public void testGetRoles() throws IOException {
        SortedSet<GeoServerRole> roles = service.getRoles();
        assertEquals(7, roles.size());
        List<String> strRoles =
                roles.stream().map(r -> r.getAuthority()).collect(Collectors.toList());
        assertTrue(strRoles.contains("admin"));
        assertTrue(strRoles.contains("realm-role1"));
        assertTrue(strRoles.contains("realm-role2"));
        assertTrue(strRoles.contains("client1-role1"));
        assertTrue(strRoles.contains("client1-role2"));
        assertTrue(strRoles.contains("client2-role1"));
        assertTrue(strRoles.contains("client2-role2"));
    }

    @Test
    public void testGetUserRoles() throws IOException {
        SortedSet<GeoServerRole> roles = service.getRolesForUser("user1");
        assertEquals(3, roles.size());
        List<String> strRoles =
                roles.stream().map(r -> r.getAuthority()).collect(Collectors.toList());
        assertTrue(strRoles.contains("client2-role1"));
        assertTrue(strRoles.contains("client2-role2"));
        assertTrue(strRoles.contains("admin"));
    }

    @Test
    public void testGetNotExistingUserRoles() throws IOException {
        SortedSet<GeoServerRole> roles = service.getRolesForUser("not-existing-user");
        assertTrue(roles.isEmpty());
    }

    @Test
    public void testGetRole() throws IOException {
        GeoServerRole role = service.getRoleByName("test-create");
        assertEquals("test-create", role.getAuthority());
    }

    @Test
    public void testGetUsersInRole() throws IOException {
        Set<String> users = service.getUserNamesForRole(new GeoServerRole("client2-role2"));
        assertEquals(2, users.size());
        assertTrue(users.contains("user1"));
        assertTrue(users.contains("user2"));
    }

    @Test
    public void testGetNullRole() throws IOException {
        GeoServerRole role = service.getRoleByName("test-not-existing");
        assertNull(role);
    }

    @Test
    public void testGetUsersInNotExistingRole() throws IOException {
        Set<String> users = service.getUserNamesForRole(new GeoServerRole("not-exists"));
        assertTrue(users.isEmpty());
    }

    @AfterClass
    public static void stop() {
        keycloakSrv.stop();
    }
}
