/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Builds Keycloak Admin REST API endpoint URLs.
 *
 * <p>The {@code serverUrl} is used as-is as the base for all paths. This supports both Keycloak distributions:
 *
 * <ul>
 *   <li>Keycloak 17+ (Quarkus, default): set {@code serverUrl} to {@code http://host:port} — paths will be
 *       {@code /admin/realms/...} and {@code /realms/.../protocol/openid-connect/token}.
 *   <li>Keycloak ≤16 (WildFly) or Quarkus with {@code /auth} re-enabled: set {@code serverUrl} to
 *       {@code http://host:port/auth} — the same paths are appended, producing the expected
 *       {@code /auth/admin/realms/...}.
 * </ul>
 */
class KeycloakUrlBuilder {

    private StringBuilder sb;
    private final String realm;
    private final String serverUrl;

    KeycloakUrlBuilder(String realm, String serverUrl) {
        this.realm = realm;
        this.serverUrl = serverUrl;
        init();
    }

    private void init() {
        StringBuilder sb = new StringBuilder(serverUrl);
        sb.append("/admin/realms/").append(realm);
        this.sb = sb;
    }

    KeycloakUrlBuilder client(String clientId) {
        sb.append("/clients/").append(clientId);
        return this;
    }

    KeycloakUrlBuilder userByName(String userName) {
        users();
        try {
            sb.append("?exact=true&username=").append(URLEncoder.encode(userName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    KeycloakUrlBuilder users() {
        sb.append("/users");
        return this;
    }

    KeycloakUrlBuilder userById(String userId) {
        sb.append("/users/").append(userId);
        return this;
    }

    KeycloakUrlBuilder allRoles() {
        sb.append("/roles");
        return this;
    }

    KeycloakUrlBuilder allUserRoles(boolean realmOnly) {
        sb.append("/role-mappings");
        if (realmOnly) sb.append("/realm");
        return this;
    }

    KeycloakUrlBuilder role(String roleName) {
        allRoles();
        sb.append("/").append(roleName);
        return this;
    }

    String build() {
        String result = sb.toString();
        init();
        return result;
    }

    String buildTokenEndpoint() {
        return new StringBuilder(serverUrl)
                .append("/realms/")
                .append(realm)
                .append("/protocol/openid-connect/token")
                .toString();
    }
}
