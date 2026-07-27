/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.config;

import org.geoserver.security.config.RoleSource;

/**
 * Where the OIDC plugin pulls the logged-in user's roles from.
 *
 * <p>Three sources read from the OAuth2/OIDC tokens themselves; two call out to an external IdP admin API:
 *
 * <ul>
 *   <li>{@link #IdToken} — claim on the OIDC ID token (typical for standard OIDC providers)
 *   <li>{@link #AccessToken} — claim on the OAuth2 access token (typical for Keycloak-style JWT access tokens)
 *   <li>{@link #UserInfo} — call the IdP's UserInfo endpoint with the access token
 *   <li>{@link #MSGraphAPI} — call Microsoft Graph (groups / app-role-assignments) with the user's access token
 *   <li>{@link #KeycloakAPI} — call Keycloak's Admin REST API via a service-account client, returns flattened composite
 *       + group-inherited roles via the {@code …/role-mappings/.../composite} endpoints
 * </ul>
 *
 * <p>Resolution per value is implemented by an {@code OpenIdRoleResolverStrategy} keyed off this enum; adding a new IdP
 * = one new enum value + one new strategy class, no edit to the dispatcher.
 */
public enum OpenIdRoleSource implements RoleSource {
    IdToken,
    AccessToken,
    MSGraphAPI,
    KeycloakAPI,
    UserInfo;

    @Override
    public boolean equals(RoleSource other) {
        return other != null && other.toString().equals(toString());
    }
}
