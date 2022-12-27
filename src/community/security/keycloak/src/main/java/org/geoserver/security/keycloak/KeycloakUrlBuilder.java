/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.keycloak;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** A builder for a keycloak rest endpoint. */
class KeycloakUrlBuilder {

    private StringBuilder sb;
    private String realm;
    private String serverUrl;

    KeycloakUrlBuilder(String realm, String serverUrl) {
        this.realm = realm;
        this.serverUrl = serverUrl;
        init();
    }

    private void init() {
        StringBuilder sb = new StringBuilder(serverUrl);
        sb.append("/auth/admin/realms/").append(realm);
        this.sb = sb;
    }

    /**
     * Adds the client path part.
     *
     * @param clientId the client id.
     * @return this builder.
     */
    KeycloakUrlBuilder client(String clientId) {
        sb.append("/clients/").append(clientId);
        return this;
    }

    /**
     * add the path part and query string to retrieve a user by its username.
     *
     * @param userName the username.
     * @return this builder.
     */
    KeycloakUrlBuilder userByName(String userName) {
        users();
        try {
            sb.append("?exact=true&username=").append(URLEncoder.encode(userName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /**
     * Adds the users path part.
     *
     * @return this builder.
     */
    KeycloakUrlBuilder users() {
        sb.append("/users");
        return this;
    }

    /**
     * Adds the user and userid path part.
     *
     * @param userId the user id.
     * @return this builder.
     */
    KeycloakUrlBuilder userById(String userId) {
        sb.append("/users/").append(userId);
        return this;
    }

    /**
     * Adds the roles path part.
     *
     * @return this builder.
     */
    KeycloakUrlBuilder allRoles() {
        sb.append("/roles");
        return this;
    }

    /**
     * Add the roles path part when requesting user's roles.
     *
     * @param realmOnly if true will append the realm path part to limit the result to realm roles
     *     only.
     * @return this builder.
     */
    KeycloakUrlBuilder allUserRoles(boolean realmOnly) {
        sb.append("/role-mappings");
        if (realmOnly) sb.append("/realm");
        return this;
    }

    /**
     * add the role path part.
     *
     * @param roleName the role name.
     * @return this builder.
     */
    KeycloakUrlBuilder role(String roleName) {
        allRoles();
        sb.append("/").append(roleName);
        return this;
    }

    /**
     * Build the url.
     *
     * @return the url as a string.
     */
    String build() {
        String result = sb.toString();
        init();
        return result;
    }

    /**
     * Build the access token endpoint.
     *
     * @return the access token endpoint.
     */
    String buildTokenEndpoint() {
        StringBuilder sb = new StringBuilder(serverUrl);
        sb.append("/auth/realms/").append(realm);
        String result = sb.append("/protocol/openid-connect/token").toString();
        return result;
    }
}
