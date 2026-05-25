/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.role.provider.keycloak;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Resolves the logged-in user's roles by calling Keycloak's Admin REST API via a service-account client. Stateless, one
 * instance per resolution call (mirrors {@code MSGraphRolesResolver}'s pattern).
 *
 * <p>Why service-account credentials? Keycloak's admin endpoints don't accept the user's delegated access token — they
 * require a confidential client with {@code service-accounts-enabled=true} and the relevant {@code realm-management}
 * roles ({@code view-realm}, {@code view-users}, {@code view-clients}). This resolver performs:
 *
 * <ol>
 *   <li>OAuth2 client-credentials grant against {@code /realms/{realm}/protocol/openid-connect/token} to obtain a
 *       short-lived access token bound to the admin client's service account.
 *   <li>{@code GET /admin/realms/{realm}/users?exact=true&username=<principal>} to resolve the user's UUID.
 *   <li>{@code GET /admin/realms/{realm}/users/{uuid}/role-mappings/realm[/composite]} (and the client variant per
 *       configured client) to enumerate the user's assigned roles. The {@code /composite} suffix flattens
 *       group-inherited and composite-parent roles server-side when {@code useCompositeRoles} is on.
 * </ol>
 *
 * <p>No token cache, no connection pool — symmetric with MS Graph's per-call lifecycle. If admin-side latency becomes a
 * concern in production, consider adding a per-filter token cache.
 */
public class KeycloakRolesResolver {

    private static final Logger LOGGER = Logging.getLogger(KeycloakRolesResolver.class);

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final HttpClient httpClient;

    public KeycloakRolesResolver() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    /** Visible for testing — accepts a pre-configured HttpClient. */
    KeycloakRolesResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Resolves the realm + client role names assigned to {@code username}.
     *
     * @param serverUrl Keycloak server base URL (e.g. {@code http://kc:8180}; include {@code /auth} for Keycloak ≤ 16)
     * @param realm Keycloak realm name
     * @param adminClientId service-account-enabled admin client id
     * @param adminClientSecret admin client secret
     * @param clientIdsOfRoleScopes comma-separated public clientIds whose client-roles should also be pulled in; may be
     *     {@code null}/blank for realm-roles-only
     * @param useCompositeRoles {@code true} to hit the transitive {@code /composite} variants — surfaces group +
     *     composite-parent roles server-side
     * @param username GeoServer principal to resolve
     * @return assigned role names (never null; never modified)
     */
    public List<String> resolveRoles(
            String serverUrl,
            String realm,
            String adminClientId,
            String adminClientSecret,
            String clientIdsOfRoleScopes,
            boolean useCompositeRoles,
            String username) {
        List<String> roles = new ArrayList<>();
        try {
            KeycloakUrlBuilder urls = new KeycloakUrlBuilder(normalize(serverUrl), realm);
            String adminToken = fetchAdminToken(urls.tokenEndpoint(), adminClientId, adminClientSecret);
            if (adminToken == null) return roles;

            String userId = findUserId(urls, adminToken, username);
            if (userId == null) {
                LOGGER.log(
                        Level.FINE,
                        () -> "Keycloak admin lookup: user '" + username + "' not found in realm '" + realm + "'");
                return roles;
            }

            // realm roles
            roles.addAll(toRoleNames(adminGetList(urls.userRealmRoleMappings(userId, useCompositeRoles), adminToken)));

            // per-configured-client client roles
            for (String clientId : splitCsv(clientIdsOfRoleScopes)) {
                String clientUuid = findClientUuid(urls, adminToken, clientId);
                if (clientUuid == null) {
                    LOGGER.log(
                            Level.FINE,
                            () -> "Keycloak client '" + clientId + "' not found in realm '" + realm
                                    + "'; skipping its client-roles");
                    continue;
                }
                roles.addAll(toRoleNames(
                        adminGetList(urls.userClientRoleMappings(userId, clientUuid, useCompositeRoles), adminToken)));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Keycloak admin role lookup failed for user '" + username + "'", e);
        }
        return roles;
    }

    private String fetchAdminToken(URI tokenEndpoint, String clientId, String secret)
            throws IOException, InterruptedException {
        String body = "grant_type=client_credentials"
                + "&client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(secret);
        HttpRequest req = HttpRequest.newBuilder(tokenEndpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            LOGGER.severe("Keycloak token endpoint " + tokenEndpoint + " returned HTTP " + resp.statusCode()
                    + " — body: " + resp.body());
            return null;
        }
        Map<String, Object> json = mapper.readValue(resp.body(), new TypeReference<>() {});
        Object token = json.get("access_token");
        return token == null ? null : token.toString();
    }

    private String findUserId(KeycloakUrlBuilder urls, String adminToken, String username)
            throws IOException, InterruptedException {
        List<Map<String, Object>> matches = adminGetList(urls.userByUsername(username), adminToken);
        if (matches == null || matches.isEmpty()) return null;
        Object id = matches.get(0).get("id");
        return id == null ? null : id.toString();
    }

    private String findClientUuid(KeycloakUrlBuilder urls, String adminToken, String clientId)
            throws IOException, InterruptedException {
        List<Map<String, Object>> matches = adminGetList(urls.clientsByClientId(clientId), adminToken);
        if (matches == null || matches.isEmpty()) return null;
        Object id = matches.get(0).get("id");
        return id == null ? null : id.toString();
    }

    private List<Map<String, Object>> adminGetList(URI url, String adminToken)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(url)
                .header("Authorization", "Bearer " + adminToken)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            LOGGER.fine(() -> "Non-2xx from " + url + ": " + resp.statusCode() + " — body: " + resp.body());
            return Collections.emptyList();
        }
        if (resp.body() == null || resp.body().isBlank()) return Collections.emptyList();
        return mapper.readValue(resp.body(), new TypeReference<>() {});
    }

    private static List<String> toRoleNames(List<Map<String, Object>> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<String> names = new ArrayList<>(raw.size());
        for (Map<String, Object> r : raw) {
            Object n = r.get("name");
            if (n != null) names.add(n.toString());
        }
        return names;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String normalize(String url) {
        if (url == null) return "";
        String u = url.trim();
        if (!u.matches("(?i)^https?://.*$")) u = "http://" + u;
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }
}
