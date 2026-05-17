/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common.keycloak;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Pure-function tests of the Keycloak Admin REST API URL builder. */
public class KeycloakUrlBuilderTest {

    private final KeycloakUrlBuilder urls = new KeycloakUrlBuilder("http://kc:8180", "myrealm");

    @Test
    public void tokenEndpoint() {
        assertEquals(
                "http://kc:8180/realms/myrealm/protocol/openid-connect/token",
                urls.tokenEndpoint().toString());
    }

    @Test
    public void userByUsername() {
        assertEquals(
                "http://kc:8180/admin/realms/myrealm/users?exact=true&username=alice",
                urls.userByUsername("alice").toString());
    }

    @Test
    public void userByUsernameEncodesSpecialChars() {
        // Plus, slash and space all require URL-encoding in the query string
        assertEquals(
                "http://kc:8180/admin/realms/myrealm/users?exact=true&username=user%2B%2Falice",
                urls.userByUsername("user+/alice").toString());
    }

    @Test
    public void userRealmRoleMappings_nonComposite() {
        assertEquals(
                "http://kc:8180/admin/realms/myrealm/users/uuid-1/role-mappings/realm",
                urls.userRealmRoleMappings("uuid-1", false).toString());
    }

    @Test
    public void userRealmRoleMappings_composite() {
        assertEquals(
                "http://kc:8180/admin/realms/myrealm/users/uuid-1/role-mappings/realm/composite",
                urls.userRealmRoleMappings("uuid-1", true).toString());
    }

    @Test
    public void userClientRoleMappings_composite() {
        assertEquals(
                "http://kc:8180/admin/realms/myrealm/users/uuid-1/role-mappings/clients/client-uuid/composite",
                urls.userClientRoleMappings("uuid-1", "client-uuid", true).toString());
    }

    @Test
    public void clientsByClientId() {
        assertEquals(
                "http://kc:8180/admin/realms/myrealm/clients?clientId=gs-client",
                urls.clientsByClientId("gs-client").toString());
    }

    @Test
    public void realmNameUrlEncoded() {
        KeycloakUrlBuilder weird = new KeycloakUrlBuilder("http://kc:8180", "realm with space");
        assertEquals(
                "http://kc:8180/realms/realm+with+space/protocol/openid-connect/token",
                weird.tokenEndpoint().toString());
    }

    /**
     * Keycloak ≤ 16 (Wildfly) deployments include the {@code /auth} suffix as part of the server URL — the builder
     * should treat that as the prefix and append admin/realm paths after it.
     */
    @Test
    public void serverUrlWithAuthPrefix() {
        KeycloakUrlBuilder legacy = new KeycloakUrlBuilder("https://kc.example.com/auth", "myrealm");
        assertEquals(
                "https://kc.example.com/auth/realms/myrealm/protocol/openid-connect/token",
                legacy.tokenEndpoint().toString());
        assertEquals(
                "https://kc.example.com/auth/admin/realms/myrealm/users/uuid-1/role-mappings/realm/composite",
                legacy.userRealmRoleMappings("uuid-1", true).toString());
    }
}
