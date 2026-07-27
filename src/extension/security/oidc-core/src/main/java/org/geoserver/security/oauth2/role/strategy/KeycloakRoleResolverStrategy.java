/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.role.strategy;

import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.config.OpenIdRoleSource;
import org.geoserver.security.oauth2.role.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.role.provider.keycloak.KeycloakRolesResolver;
import org.geotools.util.logging.Logging;

/**
 * Resolves roles by calling Keycloak's Admin REST API via a service-account client. The Keycloak server URL and realm
 * are derived from the OIDC URIs already configured on the filter for the login flow — admins only need to provide the
 * dedicated admin client credentials inline on this filter.
 */
public class KeycloakRoleResolverStrategy implements OpenIdRoleResolverStrategy {

    private static final Logger LOGGER = Logging.getLogger(KeycloakRoleResolverStrategy.class);

    /**
     * Pattern matching any Keycloak realm-scoped URL (discovery, token, authorization, etc.). Group 1 captures the
     * server base URL (with any {@code /auth} suffix for Keycloak &lt; 17); group 2 captures the realm name.
     */
    private static final Pattern KEYCLOAK_REALM_URL =
            Pattern.compile("^(.+?)/realms/([^/]+)/(?:\\.well-known/openid-configuration|protocol/openid-connect/.+)$");

    /** Supplier of {@link KeycloakRolesResolver} — visible to tests via {@link #setResolverSupplier(Supplier)}. */
    private Supplier<KeycloakRolesResolver> resolverSupplier = KeycloakRolesResolver::new;

    @Override
    public OpenIdRoleSource getRoleSource() {
        return OpenIdRoleSource.KeycloakAPI;
    }

    @Override
    public List<String> resolveRoleNames(OAuth2ResolverParam param, GeoServerOAuth2LoginFilterConfig config) {
        String user = param.getPrincipal();
        String[] serverAndRealm = deriveServerAndRealm(config);
        if (serverAndRealm == null) {
            LOGGER.log(
                    SEVERE,
                    ("Role source KeycloakAPI could not derive the Keycloak server URL + realm from the filter's "
                                    + "oidcDiscoveryUri or oidcTokenUri (expected pattern "
                                    + "'{server}/realms/{realm}/...'). User: '%s'.")
                            .formatted(user));
            return Collections.emptyList();
        }
        // Reuse the OIDC login client's credentials for the admin REST API call. The OIDC client must have
        // service-accounts-enabled=true and the relevant realm-management roles assigned to its service-account user.
        String adminClientId = config.getOidcClientId();
        String adminClientSecret = config.getOidcClientSecret();
        if (adminClientId == null
                || adminClientId.isBlank()
                || adminClientSecret == null
                || adminClientSecret.isBlank()) {
            LOGGER.log(
                    SEVERE,
                    ("Role source KeycloakAPI requires the OIDC client id + secret to be configured on this filter "
                                    + "(the client itself must have service-accounts-enabled=true + realm-management "
                                    + "roles assigned to its service account). User: '%s'.")
                            .formatted(user));
            return Collections.emptyList();
        }
        String serverUrl = serverAndRealm[0];
        String realm = serverAndRealm[1];
        List<String> roles = new ArrayList<>();
        try {
            roles = new ArrayList<>(resolverSupplier
                    .get()
                    .resolveRoles(
                            serverUrl,
                            realm,
                            adminClientId,
                            adminClientSecret,
                            config.getKeycloakAdminClientIdsOfRoleScopes(),
                            config.isKeycloakUseCompositeRoles(),
                            user));
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Role assignments for '%s' from Keycloak Admin API (realm '%s'): %s"
                        .formatted(user, realm, roles.stream().collect(joining(","))));
            }
        } catch (Exception e) {
            LOGGER.log(
                    SEVERE,
                    "Resolving roles from Keycloak Admin API failed for user '%s' (realm '%s').".formatted(user, realm),
                    e);
        }
        return roles;
    }

    /**
     * Pulls the Keycloak server base URL + realm name out of whichever OIDC URI on the filter looks like a Keycloak
     * realm URL. Tries {@code oidcDiscoveryUri} first (set after the user clicks the "Discover" button), then falls
     * back to {@code oidcTokenUri}. Returns {@code null} if neither matches the expected pattern.
     */
    static String[] deriveServerAndRealm(GeoServerOAuth2LoginFilterConfig config) {
        String[] candidates = {config.getOidcDiscoveryUri(), config.getOidcTokenUri()};
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) continue;
            Matcher m = KEYCLOAK_REALM_URL.matcher(candidate.trim());
            if (m.matches()) {
                return new String[] {m.group(1), m.group(2)};
            }
        }
        return null;
    }

    public void setResolverSupplier(Supplier<KeycloakRolesResolver> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier for KeycloakRolesResolver must not be null.");
        }
        this.resolverSupplier = supplier;
    }
}
