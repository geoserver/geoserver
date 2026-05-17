/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.geoserver.security.oauth2.common.keycloak.KeycloakRolesResolver;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end integration test of {@link KeycloakRolesResolver} against a real Testcontainers Keycloak instance.
 *
 * <p>Avoids Keycloak 26's realm-import-then-startup crash ("Session not bound to a realm") by spinning up an empty
 * Keycloak and provisioning the entire test realm — roles, group, users, client, service-account role mappings — via
 * the Admin REST API in {@link #beforeAll()}. Equivalent setup to what an operator does in the Keycloak admin console
 * for a production GeoServer deployment, but scripted.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>Composite-role expansion: {@code admin → editor → viewer}
 *   <li>Group-inherited roles: {@code via-group-editor} (no direct role) resolves to {@code editor, viewer} via the
 *       {@code /composite} endpoint
 *   <li>The non-composite endpoint returns ONLY direct mappings (group-inherited roles invisible)
 *   <li>Unknown user → empty result, no exception
 * </ul>
 */
public class KeycloakAdminApiIntegrationTest {

    private static final String REALM = "admin-api-test";
    private static final String CLIENT_ID = "gs-client";
    private static final String CLIENT_SECRET = "secret";

    private static KeycloakContainer keycloak;
    private static String serverUrl;
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeClass
    public static void beforeAll() throws Exception {
        boolean dockerAvailable;
        try {
            DockerClientFactory.instance().client();
            dockerAvailable = true;
        } catch (Throwable t) {
            dockerAvailable = false;
        }
        Assume.assumeTrue("Docker not available — skipping", dockerAvailable);

        // Start a vanilla Keycloak — no realm-import, no fixtures. dasniko's container sets admin/admin via env vars.
        keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0").withCustomCommand("--log-level=INFO");
        keycloak.start();
        serverUrl = keycloak.getAuthServerUrl();

        provisionRealmViaAdminAPI();
    }

    @AfterClass
    public static void afterAll() {
        if (keycloak != null) keycloak.stop();
    }

    // --- Realm provisioning via Admin REST API ---

    private static void provisionRealmViaAdminAPI() throws Exception {
        String admin = adminToken();

        // 1. Create realm
        adminPost(
                URI.create(serverUrl + "/admin/realms"),
                admin,
                """
                {"realm":"%s","enabled":true,"sslRequired":"none"}""".formatted(REALM));

        // 2. Create realm roles (viewer, editor with composite=viewer, admin with composite=editor)
        adminPost(
                URI.create(serverUrl + "/admin/realms/" + REALM + "/roles"),
                admin,
                """
                {"name":"viewer"}""");
        adminPost(
                URI.create(serverUrl + "/admin/realms/" + REALM + "/roles"),
                admin,
                """
                {"name":"editor","composite":true}""");
        adminPost(
                URI.create(serverUrl + "/admin/realms/" + REALM + "/roles"),
                admin,
                """
                {"name":"admin","composite":true}""");

        // 3. Wire the composite relationships: editor → [viewer], admin → [editor]
        Map<String, Object> viewerRole = getJson(serverUrl + "/admin/realms/" + REALM + "/roles/viewer", admin);
        Map<String, Object> editorRole = getJson(serverUrl + "/admin/realms/" + REALM + "/roles/editor", admin);
        adminPost(
                URI.create(
                        serverUrl + "/admin/realms/" + REALM + "/roles-by-id/" + editorRole.get("id") + "/composites"),
                admin,
                "[" + MAPPER.writeValueAsString(viewerRole) + "]");
        Map<String, Object> adminRole = getJson(serverUrl + "/admin/realms/" + REALM + "/roles/admin", admin);
        adminPost(
                URI.create(
                        serverUrl + "/admin/realms/" + REALM + "/roles-by-id/" + adminRole.get("id") + "/composites"),
                admin,
                "[" + MAPPER.writeValueAsString(editorRole) + "]");

        // 4. Create the gs-client client (service-accounts enabled, confidential)
        adminPost(
                URI.create(serverUrl + "/admin/realms/" + REALM + "/clients"),
                admin,
                """
                {"clientId":"%s","enabled":true,"protocol":"openid-connect","publicClient":false,
                 "clientAuthenticatorType":"client-secret","secret":"%s","standardFlowEnabled":true,
                 "directAccessGrantsEnabled":true,"serviceAccountsEnabled":true,
                 "redirectUris":["*"],"webOrigins":["*"]}"""
                        .formatted(CLIENT_ID, CLIENT_SECRET));

        // 5. Grant realm-management roles to the service-account-gs-client user (auto-created on
        // serviceAccountsEnabled)
        String realmMgmtUuid =
                lookupId(serverUrl + "/admin/realms/" + REALM + "/clients?clientId=realm-management", admin);
        String saUserId = lookupId(
                serverUrl + "/admin/realms/" + REALM + "/users?exact=true&username=service-account-gs-client", admin);
        String[] mgmtRoles = {"view-realm", "view-users", "view-clients", "query-groups", "query-users", "query-clients"
        };
        StringBuilder rolesPayload = new StringBuilder("[");
        for (int i = 0; i < mgmtRoles.length; i++) {
            Map<String, Object> r = getJson(
                    serverUrl + "/admin/realms/" + REALM + "/clients/" + realmMgmtUuid + "/roles/" + mgmtRoles[i],
                    admin);
            if (i > 0) rolesPayload.append(',');
            rolesPayload.append(MAPPER.writeValueAsString(r));
        }
        rolesPayload.append("]");
        adminPost(
                URI.create(serverUrl + "/admin/realms/" + REALM + "/users/" + saUserId + "/role-mappings/clients/"
                        + realmMgmtUuid),
                admin,
                rolesPayload.toString());

        // 6. Create users with credentials + realm-role mappings
        createUser(admin, "direct-admin", "admin");
        createUser(admin, "direct-editor", "editor");
        createUser(admin, "direct-viewer", "viewer");

        // 7. Create editors-group with the editor role + user via the group
        adminPost(
                URI.create(serverUrl + "/admin/realms/" + REALM + "/groups"),
                admin,
                """
                {"name":"editors-group"}""");
        String groupId =
                lookupId(serverUrl + "/admin/realms/" + REALM + "/groups?exact=true&search=editors-group", admin);
        adminPost(
                URI.create(serverUrl + "/admin/realms/" + REALM + "/groups/" + groupId + "/role-mappings/realm"),
                admin,
                "[" + MAPPER.writeValueAsString(editorRole) + "]");
        createUser(admin, "via-group-editor", null);
        String viaGroupUserId =
                lookupId(serverUrl + "/admin/realms/" + REALM + "/users?exact=true&username=via-group-editor", admin);
        adminPut(
                URI.create(serverUrl + "/admin/realms/" + REALM + "/users/" + viaGroupUserId + "/groups/" + groupId),
                admin,
                "");
    }

    private static void createUser(String adminTok, String username, String realmRole) throws Exception {
        String body =
                """
                {"username":"%s","enabled":true,"emailVerified":true,
                 "credentials":[{"type":"password","value":"%s","temporary":false}]}"""
                        .formatted(username, username);
        adminPost(URI.create(serverUrl + "/admin/realms/" + REALM + "/users"), adminTok, body);
        if (realmRole != null) {
            String userId =
                    lookupId(serverUrl + "/admin/realms/" + REALM + "/users?exact=true&username=" + username, adminTok);
            Map<String, Object> role = getJson(serverUrl + "/admin/realms/" + REALM + "/roles/" + realmRole, adminTok);
            adminPost(
                    URI.create(serverUrl + "/admin/realms/" + REALM + "/users/" + userId + "/role-mappings/realm"),
                    adminTok,
                    "[" + MAPPER.writeValueAsString(role) + "]");
        }
    }

    // --- HTTP helpers ---

    private static String adminToken() throws Exception {
        String body = "grant_type=password&client_id=admin-cli&username=" + keycloak.getAdminUsername() + "&password="
                + keycloak.getAdminPassword();
        HttpResponse<String> resp = HTTP.send(
                HttpRequest.newBuilder(URI.create(serverUrl + "/realms/master/protocol/openid-connect/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IllegalStateException("master admin auth: " + resp.body());
        return MAPPER.readValue(resp.body(), new TypeReference<Map<String, Object>>() {})
                .get("access_token")
                .toString();
    }

    private static void adminPost(URI url, String token, String json) throws Exception {
        HttpResponse<String> resp = HTTP.send(
                HttpRequest.newBuilder(url)
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300 && resp.statusCode() != 409) {
            // 409 Conflict = already exists, fine for idempotent re-runs
            throw new IllegalStateException("POST " + url + " → HTTP " + resp.statusCode() + ": " + resp.body());
        }
    }

    private static void adminPut(URI url, String token, String json) throws Exception {
        HttpResponse<String> resp = HTTP.send(
                HttpRequest.newBuilder(url)
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IllegalStateException("PUT " + url + " → HTTP " + resp.statusCode() + ": " + resp.body());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getJson(String url, String token) throws Exception {
        HttpResponse<String> resp = HTTP.send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (resp.body().startsWith("[")) {
            return MAPPER.readValue(resp.body(), new TypeReference<List<Map<String, Object>>>() {})
                    .get(0);
        }
        return MAPPER.readValue(resp.body(), new TypeReference<>() {});
    }

    private static String lookupId(String url, String token) throws Exception {
        return getJson(url, token).get("id").toString();
    }

    // --- the actual tests ---

    @Test
    public void directAdmin_fullCompositeChain() {
        assertThat(resolve("direct-admin", true), containsInAnyOrder("admin", "editor", "viewer"));
    }

    @Test
    public void directEditor_editorAndViewer() {
        assertThat(resolve("direct-editor", true), containsInAnyOrder("editor", "viewer"));
    }

    @Test
    public void directViewer_viewerOnly() {
        assertThat(resolve("direct-viewer", true), containsInAnyOrder("viewer"));
    }

    @Test
    public void viaGroupEditor_compositeEnabled_resolvesEditorAndViewer() {
        assertThat(resolve("via-group-editor", true), containsInAnyOrder("editor", "viewer"));
    }

    @Test
    public void viaGroupEditor_compositeDisabled_resolvesNothing() {
        assertThat(resolve("via-group-editor", false), empty());
    }

    @Test
    public void unknownUser_returnsEmpty() {
        assertThat(resolve("does-not-exist", true), empty());
    }

    /**
     * Resolve roles and strip Keycloak's auto-assigned defaults ({@code offline_access}, {@code uma_authorization},
     * {@code default-roles-<realm>}) — those are noise common to every user in every realm and would clutter the
     * per-test assertions which are checking only the test-fixture-specific composite/group behaviour.
     */
    private static List<String> resolve(String username, boolean useComposite) {
        return new KeycloakRolesResolver()
                .resolveRoles(serverUrl, REALM, CLIENT_ID, CLIENT_SECRET, null, useComposite, username).stream()
                        .filter(r -> !"offline_access".equals(r))
                        .filter(r -> !"uma_authorization".equals(r))
                        .filter(r -> !r.startsWith("default-roles-"))
                        .toList();
    }
}
