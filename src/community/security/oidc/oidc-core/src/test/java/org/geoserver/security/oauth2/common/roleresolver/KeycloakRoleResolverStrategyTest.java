/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common.roleresolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.geoserver.security.oauth2.common.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.common.keycloak.KeycloakRolesResolver;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.junit.Test;

/**
 * Tests for the strategy's contract — primarily the OIDC-URI parsing logic
 * ({@link KeycloakRoleResolverStrategy#deriveServerAndRealm}) and the config-validation paths in
 * {@code resolveRoleNames}. Actual HTTP behaviour is covered by
 * {@link org.geoserver.security.oauth2.common.keycloak.KeycloakRolesResolverTest}; here we inject a mock resolver via
 * the supplier hook.
 */
public class KeycloakRoleResolverStrategyTest {

    @Test
    public void roleSource_isKeycloakAPI() {
        assertThat(new KeycloakRoleResolverStrategy().getRoleSource(), equalTo(OpenIdRoleSource.KeycloakAPI));
    }

    // --- deriveServerAndRealm ---

    @Test
    public void deriveServerAndRealm_fromDiscoveryUri_keycloak17_noAuthPrefix() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri("http://kc:8180/realms/demo/.well-known/openid-configuration");

        String[] r = KeycloakRoleResolverStrategy.deriveServerAndRealm(cfg);
        assertThat(r[0], equalTo("http://kc:8180"));
        assertThat(r[1], equalTo("demo"));
    }

    @Test
    public void deriveServerAndRealm_fromDiscoveryUri_keycloak16_withAuthPrefix() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri("https://kc.example.com/auth/realms/demo/.well-known/openid-configuration");

        String[] r = KeycloakRoleResolverStrategy.deriveServerAndRealm(cfg);
        assertThat(r[0], equalTo("https://kc.example.com/auth"));
        assertThat(r[1], equalTo("demo"));
    }

    @Test
    public void deriveServerAndRealm_fromTokenUri_whenDiscoveryUriEmpty() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri(null);
        cfg.setOidcTokenUri("http://kc:8180/realms/staging/protocol/openid-connect/token");

        String[] r = KeycloakRoleResolverStrategy.deriveServerAndRealm(cfg);
        assertThat(r[0], equalTo("http://kc:8180"));
        assertThat(r[1], equalTo("staging"));
    }

    @Test
    public void deriveServerAndRealm_discoveryWins_overTokenUri() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri("http://primary:8180/realms/from-discovery/.well-known/openid-configuration");
        cfg.setOidcTokenUri("http://secondary:9090/realms/from-token/protocol/openid-connect/token");

        String[] r = KeycloakRoleResolverStrategy.deriveServerAndRealm(cfg);
        assertThat(r[1], equalTo("from-discovery"));
    }

    @Test
    public void deriveServerAndRealm_returnsNull_whenNoUriMatches() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri("https://login.microsoftonline.com/tenant/v2.0/.well-known/openid-configuration");
        cfg.setOidcTokenUri(null);

        assertThat(KeycloakRoleResolverStrategy.deriveServerAndRealm(cfg), nullValue());
    }

    @Test
    public void deriveServerAndRealm_returnsNull_whenBothUrisBlank() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri(null);
        cfg.setOidcTokenUri(null);

        assertThat(KeycloakRoleResolverStrategy.deriveServerAndRealm(cfg), nullValue());
    }

    // --- resolveRoleNames config-validation paths ---

    @Test
    public void resolveRoleNames_returnsEmpty_whenOidcUrisMissing() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcClientId("any");
        cfg.setOidcClientSecret("any");
        // no oidcDiscoveryUri / oidcTokenUri set

        KeycloakRoleResolverStrategy strat = new KeycloakRoleResolverStrategy();
        assertThat(strat.resolveRoleNames(paramFor("alice"), cfg), empty());
    }

    @Test
    public void resolveRoleNames_returnsEmpty_whenOidcClientIdMissing() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri("http://kc:8180/realms/demo/.well-known/openid-configuration");
        cfg.setOidcClientId(null);
        cfg.setOidcClientSecret("s3cret");

        KeycloakRoleResolverStrategy strat = new KeycloakRoleResolverStrategy();
        assertThat(strat.resolveRoleNames(paramFor("alice"), cfg), empty());
    }

    @Test
    public void resolveRoleNames_returnsEmpty_whenOidcClientSecretMissing() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri("http://kc:8180/realms/demo/.well-known/openid-configuration");
        cfg.setOidcClientId("clientA");
        cfg.setOidcClientSecret(null);

        KeycloakRoleResolverStrategy strat = new KeycloakRoleResolverStrategy();
        assertThat(strat.resolveRoleNames(paramFor("alice"), cfg), empty());
    }

    // --- supplier hook ---

    @Test
    public void resolveRoleNames_delegatesToInjectedResolver_withDerivedServerAndRealm() {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcDiscoveryUri("http://kc:8180/realms/demo/.well-known/openid-configuration");
        cfg.setOidcClientId("gs-client");
        cfg.setOidcClientSecret("s3cret");
        cfg.setKeycloakAdminClientIdsOfRoleScopes("scoped-client");
        cfg.setKeycloakUseCompositeRoles(true);

        KeycloakRolesResolver mockResolver = mock(KeycloakRolesResolver.class);
        when(mockResolver.resolveRoles(any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(Arrays.asList("geoserver-editor", "geoserver-viewer"));

        KeycloakRoleResolverStrategy strat = new KeycloakRoleResolverStrategy();
        strat.setResolverSupplier(() -> mockResolver);

        assertThat(strat.resolveRoleNames(paramFor("alice"), cfg), contains("geoserver-editor", "geoserver-viewer"));
        verify(mockResolver)
                .resolveRoles(
                        eq("http://kc:8180"),
                        eq("demo"),
                        eq("gs-client"),
                        eq("s3cret"),
                        eq("scoped-client"),
                        eq(true),
                        eq("alice"));
    }

    /** Build a minimal {@link OAuth2ResolverParam} carrying just the principal — we don't exercise the OAuth2 flow. */
    private static OAuth2ResolverParam paramFor(String username) {
        return new OAuth2ResolverParam(username, null, null, null);
    }
}
