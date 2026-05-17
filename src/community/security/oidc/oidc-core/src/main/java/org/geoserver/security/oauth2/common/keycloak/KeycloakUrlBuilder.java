/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common.keycloak;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Stateless helper that assembles Keycloak Admin REST API URLs. Each builder method returns a fresh {@link URI}; this
 * class holds no per-request state so it's safe to share across threads.
 *
 * <p>Server URL convention: pass the URL up to but not including {@code /realms/...}. Keycloak ≤ 16 expects the
 * trailing {@code /auth} to be part of {@code serverUrl} (e.g. {@code https://kc.example.com/auth}); Keycloak ≥ 17
 * (Quarkus) uses the bare URL ({@code https://kc.example.com}).
 */
final class KeycloakUrlBuilder {

    private final String serverUrl;
    private final String realm;

    KeycloakUrlBuilder(String serverUrl, String realm) {
        this.serverUrl = serverUrl;
        this.realm = realm;
    }

    /** {@code {serverUrl}/realms/{realm}/protocol/openid-connect/token} */
    URI tokenEndpoint() {
        return URI.create(serverUrl + "/realms/" + enc(realm) + "/protocol/openid-connect/token");
    }

    /** {@code .../admin/realms/{realm}/users?exact=true&username=...} — lookup user by exact username. */
    URI userByUsername(String username) {
        return URI.create(adminBase() + "/users?exact=true&username=" + enc(username));
    }

    /** Per-user realm role mappings; {@code composite} switches to the transitive endpoint. */
    URI userRealmRoleMappings(String userId, boolean composite) {
        String base = adminBase() + "/users/" + enc(userId) + "/role-mappings/realm";
        return URI.create(composite ? base + "/composite" : base);
    }

    /** Per-user client role mappings; {@code composite} switches to the transitive endpoint. */
    URI userClientRoleMappings(String userId, String clientUuid, boolean composite) {
        String base = adminBase() + "/users/" + enc(userId) + "/role-mappings/clients/" + enc(clientUuid);
        return URI.create(composite ? base + "/composite" : base);
    }

    /** {@code .../admin/realms/{realm}/clients?clientId=...} — resolve a client UUID from its public clientId. */
    URI clientsByClientId(String clientId) {
        return URI.create(adminBase() + "/clients?clientId=" + enc(clientId));
    }

    private String adminBase() {
        return serverUrl + "/admin/realms/" + enc(realm);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
