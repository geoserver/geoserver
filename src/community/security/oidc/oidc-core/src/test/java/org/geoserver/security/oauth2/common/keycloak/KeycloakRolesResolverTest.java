/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNotNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * End-to-end test of {@link KeycloakRolesResolver} against a WireMock-simulated Keycloak Admin REST API. Covers:
 *
 * <ul>
 *   <li>Token endpoint → admin user lookup → realm role mappings happy path
 *   <li>Composite vs non-composite endpoint selection
 *   <li>Client roles pull-in (one extra HTTP round trip per configured public clientId)
 *   <li>Unknown user → empty roles (no exception)
 *   <li>Token endpoint failure → empty roles + warning logged
 * </ul>
 */
public class KeycloakRolesResolverTest {

    private WireMockServer kc;
    private String serverUrl;

    private static final String REALM = "demo";
    private static final String ADMIN_CLIENT = "admin-client";
    private static final String ADMIN_SECRET = "s3cret";

    @Before
    public void setUp() {
        kc = new WireMockServer(wireMockConfig().dynamicPort());
        kc.start();
        serverUrl = "http://localhost:" + kc.port();
    }

    @After
    public void tearDown() {
        if (kc != null) kc.stop();
    }

    private void stubTokenEndpoint() {
        kc.stubFor(post(urlEqualTo("/realms/" + REALM + "/protocol/openid-connect/token"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_id=" + ADMIN_CLIENT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"admin-tok\",\"expires_in\":300}")));
    }

    private void stubUserLookup(String username, String userId) {
        kc.stubFor(get(urlEqualTo("/admin/realms/" + REALM + "/users?exact=true&username=" + username))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"" + userId + "\",\"username\":\"" + username + "\"}]")));
    }

    private void stubRealmRoleMappings(String userId, boolean composite, String... roleNames) {
        String suffix = composite ? "/composite" : "";
        StringBuilder body = new StringBuilder("[");
        for (int i = 0; i < roleNames.length; i++) {
            if (i > 0) body.append(',');
            body.append("{\"name\":\"").append(roleNames[i]).append("\"}");
        }
        body.append("]");
        kc.stubFor(get(urlEqualTo("/admin/realms/" + REALM + "/users/" + userId + "/role-mappings/realm" + suffix))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body.toString())));
    }

    @Test
    public void happyPath_realmRolesOnly_composite() {
        stubTokenEndpoint();
        stubUserLookup("alice", "uuid-alice");
        stubRealmRoleMappings("uuid-alice", true, "geoserver-editor", "geoserver-viewer");

        KeycloakRolesResolver resolver = new KeycloakRolesResolver();
        List<String> roles = resolver.resolveRoles(serverUrl, REALM, ADMIN_CLIENT, ADMIN_SECRET, null, true, "alice");

        assertThat(roles, containsInAnyOrder("geoserver-editor", "geoserver-viewer"));
        kc.verify(postRequestedFor(urlEqualTo("/realms/" + REALM + "/protocol/openid-connect/token")));
        kc.verify(getRequestedFor(urlPathEqualTo("/admin/realms/" + REALM + "/users"))
                .withHeader("Authorization", equalTo("Bearer admin-tok")));
    }

    @Test
    public void useCompositeFalse_hitsPlainEndpoint() {
        stubTokenEndpoint();
        stubUserLookup("alice", "uuid-alice");
        stubRealmRoleMappings("uuid-alice", false, "geoserver-viewer");

        KeycloakRolesResolver resolver = new KeycloakRolesResolver();
        List<String> roles = resolver.resolveRoles(serverUrl, REALM, ADMIN_CLIENT, ADMIN_SECRET, null, false, "alice");

        assertThat(roles, containsInAnyOrder("geoserver-viewer"));
        // The /composite suffix path should NOT have been requested
        kc.verify(
                0,
                getRequestedFor(
                        urlEqualTo("/admin/realms/" + REALM + "/users/uuid-alice/role-mappings/realm/composite")));
    }

    @Test
    public void clientRolesPulledIn_whenScopedClientConfigured() {
        stubTokenEndpoint();
        stubUserLookup("alice", "uuid-alice");
        stubRealmRoleMappings("uuid-alice", true, "geoserver-editor");

        // Client UUID lookup for "scoped-client" → returns one matching client
        kc.stubFor(get(urlEqualTo("/admin/realms/" + REALM + "/clients?clientId=scoped-client"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"client-uuid\",\"clientId\":\"scoped-client\"}]")));
        // Client-role mappings for the user under that client
        kc.stubFor(get(urlEqualTo(
                        "/admin/realms/" + REALM + "/users/uuid-alice/role-mappings/clients/client-uuid/composite"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"client-editor\"},{\"name\":\"client-viewer\"}]")));

        KeycloakRolesResolver resolver = new KeycloakRolesResolver();
        List<String> roles =
                resolver.resolveRoles(serverUrl, REALM, ADMIN_CLIENT, ADMIN_SECRET, "scoped-client", true, "alice");

        assertThat(roles, containsInAnyOrder("geoserver-editor", "client-editor", "client-viewer"));
    }

    @Test
    public void userNotFound_returnsEmpty_noException() {
        stubTokenEndpoint();
        // user lookup returns []
        kc.stubFor(get(urlEqualTo("/admin/realms/" + REALM + "/users?exact=true&username=ghost"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        KeycloakRolesResolver resolver = new KeycloakRolesResolver();
        List<String> roles = resolver.resolveRoles(serverUrl, REALM, ADMIN_CLIENT, ADMIN_SECRET, null, true, "ghost");

        assertNotNull(roles);
        assertThat(roles, empty());
    }

    @Test
    public void tokenEndpointFailure_returnsEmpty_noException() {
        kc.stubFor(post(urlEqualTo("/realms/" + REALM + "/protocol/openid-connect/token"))
                .willReturn(aResponse().withStatus(401).withBody("{\"error\":\"invalid_client\"}")));

        KeycloakRolesResolver resolver = new KeycloakRolesResolver();
        List<String> roles = resolver.resolveRoles(serverUrl, REALM, ADMIN_CLIENT, "wrong-secret", null, true, "alice");

        assertNotNull(roles);
        assertThat(roles, empty());
    }

    @Test
    public void csvWhitespaceAndEmptiesTolerated() {
        stubTokenEndpoint();
        stubUserLookup("alice", "uuid-alice");
        stubRealmRoleMappings("uuid-alice", true);
        // Both "scoped-a" and "scoped-b" found, gaps ignored
        kc.stubFor(get(urlEqualTo("/admin/realms/" + REALM + "/clients?clientId=scoped-a"))
                .willReturn(aResponse().withStatus(200).withBody("[{\"id\":\"a-uuid\"}]")));
        kc.stubFor(get(urlEqualTo("/admin/realms/" + REALM + "/clients?clientId=scoped-b"))
                .willReturn(aResponse().withStatus(200).withBody("[{\"id\":\"b-uuid\"}]")));
        kc.stubFor(
                get(urlEqualTo("/admin/realms/" + REALM + "/users/uuid-alice/role-mappings/clients/a-uuid/composite"))
                        .willReturn(aResponse().withStatus(200).withBody("[{\"name\":\"role-a\"}]")));
        kc.stubFor(
                get(urlEqualTo("/admin/realms/" + REALM + "/users/uuid-alice/role-mappings/clients/b-uuid/composite"))
                        .willReturn(aResponse().withStatus(200).withBody("[{\"name\":\"role-b\"}]")));

        KeycloakRolesResolver resolver = new KeycloakRolesResolver();
        List<String> roles = resolver.resolveRoles(
                serverUrl, REALM, ADMIN_CLIENT, ADMIN_SECRET, " scoped-a , , scoped-b ,", true, "alice");

        assertThat(roles, containsInAnyOrder("role-a", "role-b"));
    }
}
