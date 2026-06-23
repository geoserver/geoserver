/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.legacy;

import java.util.List;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.LegacySecurityAlias;
import org.geoserver.security.SecurityConfigDiagnostics.ComponentType;

/**
 * Declares the legacy XStream aliases of the removed OAuth2/OIDC community plugins (now superseded by the OIDC
 * connector), so an old data directory still loads — the corresponding filters / role services are recognized, disabled
 * and reported for migration by the core security subsystem. This is pure data: the generic mechanism, placeholders and
 * XStream wiring all live in {@code gs-main}.
 *
 * <p>Azure AD was never a separate plugin — it was the OpenID Connect filter pointed at Microsoft Entra — so it shares
 * the {@code openIdConnectAuthentication} alias.
 */
public class OidcLegacyAliasProvider extends GeoServerSecurityProvider {

    private static final List<LegacySecurityAlias> ALIASES = List.of(
            new LegacySecurityAlias(
                    "openIdConnectAuthentication",
                    "org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter",
                    ComponentType.AUTHENTICATION_FILTER,
                    "gs-sec-oauth2-openid-connect (OpenID Connect / Azure AD)"),
            new LegacySecurityAlias(
                    "keycloakAdapter",
                    "org.geoserver.security.keycloak.GeoServerKeycloakFilter",
                    ComponentType.AUTHENTICATION_FILTER,
                    "gs-sec-keycloak (Keycloak)"),
            new LegacySecurityAlias(
                    "keycloakRoleService",
                    "org.geoserver.security.keycloak.KeycloakRoleService",
                    ComponentType.ROLE_SERVICE,
                    "gs-sec-keycloak (Keycloak role service)"),
            new LegacySecurityAlias(
                    "googleOauth2Authentication",
                    "org.geoserver.security.oauth2.GoogleOAuthAuthenticationFilter",
                    ComponentType.AUTHENTICATION_FILTER,
                    "gs-sec-oauth2-google (Google)"),
            new LegacySecurityAlias(
                    "githubOauth2Authentication",
                    "org.geoserver.security.oauth2.GitHubOAuthAuthenticationFilter",
                    ComponentType.AUTHENTICATION_FILTER,
                    "gs-sec-oauth2-github (GitHub)"),
            new LegacySecurityAlias(
                    "geoNodeOauth2Authentication",
                    "org.geoserver.security.oauth2.GeoNodeOAuthAuthenticationFilter",
                    ComponentType.AUTHENTICATION_FILTER,
                    "gs-sec-oauth2-geonode (GeoNode)"));

    @Override
    public List<LegacySecurityAlias> getLegacyAliases() {
        return ALIASES;
    }
}
