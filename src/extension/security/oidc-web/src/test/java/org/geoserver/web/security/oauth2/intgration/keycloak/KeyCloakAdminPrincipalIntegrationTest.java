/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.config.OpenIdRoleSource;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kordamp.json.JSONObject;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

/**
 * End-to-end coverage for the {@code allowAdminLogin} option against a real Keycloak.
 *
 * <p>The three code paths that can map an identity-provider principal onto a GeoServer identity each carry the same
 * guard, and each is exercised here against live tokens:
 *
 * <ul>
 *   <li>the interactive authorization-code login, where roles come from {@code GeoServerOAuth2RoleResolver}
 *   <li>the bearer JWT path, validated against the realm's JWK set
 *   <li>the bearer opaque path, validated by RFC 7662 introspection against the realm
 * </ul>
 *
 * <p>Unlike {@link KeyCloakIntegrationTest}, which maps the principal from the {@code email} claim and therefore never
 * reaches the guard, this test maps it from {@code preferred_username}, so the Keycloak users {@code admin} and
 * {@code root} become the literal GeoServer principals {@code admin} and {@code root}. Both are provisioned in gs-realm
 * with the {@code geoserverAdmin} client role, so a guard that failed to fire would hand out {@code ROLE_ADMINISTRATOR}
 * and the assertions below would show it.
 */
public class KeyCloakAdminPrincipalIntegrationTest extends KeyCloakIntegrationTestSupport {

    private static final String FILTER_NAME = "openidconnect";

    private static final String ROLE_AUTHENTICATED = "ROLE_AUTHENTICATED";
    private static final String ROLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR";

    @BeforeClass
    public static void beforeClassLocal() {
        // Must match the baseRedirectUri set in onSetUp(), since the transient "explicitly set" flag resets on reload.
        System.setProperty("OPENID_TEST_GS_PROXY_BASE", "http://localhost:8080/geoserver");
    }

    @Override
    protected String getLogConfiguration() {
        return "VERBOSE_LOGGING";
    }

    // ==================== interactive login ====================

    /** The default: the provider is authoritative for "admin" and the account gets the roles the realm asserts. */
    @Test
    public void interactiveLoginAsAdminIsAuthorizedByDefault() throws Exception {
        reconfigure(c -> {
            c.setAllowAdminLogin(null);
            c.setOidcIntrospectionUrl(null);
        });

        OAuth2AuthenticationToken auth = login(gsAdminUser, gsAdminPassword);

        assertEquals("admin", auth.getName());
        assertTrue(auth.isAuthenticated());
        assertAuthorities(auth, ROLE_AUTHENTICATED, ROLE_ADMINISTRATOR);
    }

    /** Turning the option off leaves the login authenticated but with nothing granted. */
    @Test
    public void interactiveLoginAsAdminGetsNoRolesWhenDisallowed() throws Exception {
        reconfigure(c -> {
            c.setAllowAdminLogin(Boolean.FALSE);
            c.setOidcIntrospectionUrl(null);
        });

        OAuth2AuthenticationToken auth = login(gsAdminUser, gsAdminPassword);

        assertEquals("admin", auth.getName());
        assertAuthorities(auth);
    }

    /** "root" is refused even where "admin" is deliberately allowed through. */
    @Test
    public void interactiveLoginAsRootGetsNoRolesEvenWhenAdminIsAllowed() throws Exception {
        reconfigure(c -> {
            c.setAllowAdminLogin(Boolean.TRUE);
            c.setOidcIntrospectionUrl(null);
        });

        OAuth2AuthenticationToken auth = login("root", "root");

        assertEquals("root", auth.getName());
        assertAuthorities(auth);
    }

    /** Disallowing the administrator account must leave everybody else alone. */
    @Test
    public void interactiveLoginAsRegularUserIsUnaffected() throws Exception {
        reconfigure(c -> {
            c.setAllowAdminLogin(Boolean.FALSE);
            c.setOidcIntrospectionUrl(null);
        });

        OAuth2AuthenticationToken auth = login(normalUserName, normalUserPassword);

        assertEquals(normalUserName, auth.getName());
        assertAuthorities(auth, ROLE_AUTHENTICATED);
    }

    // ==================== bearer JWT ====================

    @Test
    public void bearerJwtAsAdminIsAuthorizedByDefault() throws Exception {
        configureBearerJwt(null);

        Authentication auth = authenticateWithBearerToken(accessTokenFor(gsAdminUser, gsAdminPassword));

        assertEquals("admin", auth.getName());
        assertAuthorities(auth, ROLE_AUTHENTICATED, ROLE_ADMINISTRATOR);
    }

    @Test
    public void bearerJwtAsAdminGetsNoRolesWhenDisallowed() throws Exception {
        configureBearerJwt(Boolean.FALSE);

        Authentication auth = authenticateWithBearerToken(accessTokenFor(gsAdminUser, gsAdminPassword));

        assertEquals("admin", auth.getName());
        assertAuthorities(auth);
    }

    @Test
    public void bearerJwtAsRootGetsNoRolesEvenWhenAdminIsAllowed() throws Exception {
        configureBearerJwt(Boolean.TRUE);

        Authentication auth = authenticateWithBearerToken(accessTokenFor("root", "root"));

        assertEquals("root", auth.getName());
        assertAuthorities(auth);
    }

    @Test
    public void bearerJwtAsRegularUserIsUnaffected() throws Exception {
        configureBearerJwt(Boolean.FALSE);

        Authentication auth = authenticateWithBearerToken(accessTokenFor(normalUserName, normalUserPassword));

        assertEquals(normalUserName, auth.getName());
        assertAuthorities(auth, ROLE_AUTHENTICATED);
    }

    // ==================== bearer opaque (RFC 7662 introspection) ====================

    @Test
    public void bearerOpaqueAsAdminIsAuthorizedByDefault() throws Exception {
        configureBearerOpaque(null);

        Authentication auth = authenticateWithBearerToken(accessTokenFor(gsAdminUser, gsAdminPassword));

        assertEquals("admin", auth.getName());
        assertAuthorities(auth, ROLE_AUTHENTICATED, ROLE_ADMINISTRATOR);
    }

    @Test
    public void bearerOpaqueAsAdminGetsNoRolesWhenDisallowed() throws Exception {
        configureBearerOpaque(Boolean.FALSE);

        Authentication auth = authenticateWithBearerToken(accessTokenFor(gsAdminUser, gsAdminPassword));

        assertEquals("admin", auth.getName());
        assertAuthorities(auth);
    }

    @Test
    public void bearerOpaqueAsRootGetsNoRolesEvenWhenAdminIsAllowed() throws Exception {
        configureBearerOpaque(Boolean.TRUE);

        Authentication auth = authenticateWithBearerToken(accessTokenFor("root", "root"));

        assertEquals("root", auth.getName());
        assertAuthorities(auth);
    }

    @Test
    public void bearerOpaqueAsRegularUserIsUnaffected() throws Exception {
        configureBearerOpaque(Boolean.FALSE);

        Authentication auth = authenticateWithBearerToken(accessTokenFor(normalUserName, normalUserPassword));

        assertEquals(normalUserName, auth.getName());
        assertAuthorities(auth, ROLE_AUTHENTICATED);
    }

    // ==================== helpers ====================

    /** Bearer JWT mode: no introspection endpoint, so the builder wires the JWK-set decoder. */
    private void configureBearerJwt(Boolean allowAdminLogin) throws Exception {
        reconfigure(c -> {
            c.setEnableResourceServerMode(true);
            c.setOidcIntrospectionUrl(null);
            c.setAllowAdminLogin(allowAdminLogin);
        });
    }

    /** Bearer opaque mode: an introspection endpoint takes precedence over the JWK-set decoder. */
    private void configureBearerOpaque(Boolean allowAdminLogin) throws Exception {
        reconfigure(c -> {
            c.setEnableResourceServerMode(true);
            c.setOidcIntrospectionUrl(realmEndpoint("/token/introspect"));
            c.setAllowAdminLogin(allowAdminLogin);
        });
    }

    /**
     * Applies a change to the stored filter configuration. Saving a filter that is referenced by a chain makes the
     * security manager fire a change event, which rebuilds the filter chain proxy, so the next request already sees the
     * new setting.
     */
    private void reconfigure(Consumer<GeoServerOAuth2LoginFilterConfig> mutator) throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig config =
                (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig(FILTER_NAME, true);
        assertNotNull("Filter '" + FILTER_NAME + "' must exist", config);
        mutator.accept(config);
        manager.saveFilter(config);
    }

    /** Obtains a real access token from Keycloak via the direct grant, for the machine-to-machine paths. */
    private String accessTokenFor(String username, String password) throws Exception {
        JSONObject tokenResponse = getTokenFromKeycloak(username, password);
        String accessToken = tokenResponse.getString("access_token");
        assertNotNull("Keycloak should issue an access token for " + username, accessToken);
        return accessToken;
    }

    /** Sends a bearer-token request through the security filter chain and returns the resulting authentication. */
    private Authentication authenticateWithBearerToken(String accessToken) throws Exception {
        MockHttpServletRequest request = createRequest("web/", true);
        request.addHeader("Authorization", "Bearer " + accessToken);

        AtomicReference<Authentication> authRef = new AtomicReference<>();
        executeOnSecurityFiltersCapturingAuth(request, authRef);

        Authentication auth = authRef.get();
        assertNotNull("Bearer token should have produced an authentication", auth);
        return auth;
    }

    /**
     * Compares the GeoServer roles carried by an authentication against the expected set.
     *
     * <p>Spring's own resource-server pipeline stamps a {@code FACTOR_BEARER} authority onto opaque-token
     * authentications to record how the request authenticated. It is not a GeoServer role and grants nothing, so it is
     * filtered out here; every other authority has to match exactly, so an unexpected grant still fails.
     */
    private void assertAuthorities(Authentication auth, String... expected) {
        List<String> actual = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("FACTOR_"))
                .sorted()
                .toList();
        List<String> wanted = List.of(expected).stream().sorted().toList();
        assertEquals("Granted authorities for '" + auth.getName() + "'", wanted, actual);
    }

    private static String realmEndpoint(String suffix) {
        return keycloakContainer.getAuthServerUrl() + "/realms/gs-realm/protocol/openid-connect" + suffix;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerSecurityManager manager = getSecurityManager();

        GeoServerOAuth2LoginFilterConfig filterConfig = new GeoServerOAuth2LoginFilterConfig();
        filterConfig.setName(FILTER_NAME);
        filterConfig.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        filterConfig.setOidcEnabled(true);
        filterConfig.setOidcClientId(oidcClient);
        filterConfig.setOidcClientSecret(oidcClientSecret);
        filterConfig.setBaseRedirectUri("http://localhost:8080/geoserver/");
        filterConfig.calculateRedirectUris();
        filterConfig.setOidcTokenUri(realmEndpoint("/token"));
        filterConfig.setOidcAuthorizationUri(realmEndpoint("/authorize"));
        filterConfig.setOidcUserInfoUri(realmEndpoint("/userinfo"));
        filterConfig.setOidcLogoutUri(realmEndpoint("/endSession"));
        filterConfig.setOidcJwkSetUri(realmEndpoint("/certs"));
        filterConfig.setOidcScopes("openid profile email phone address");
        filterConfig.setEnableRedirectAuthenticationEntryPoint(false);

        // The point of this test: the GeoServer principal is the Keycloak username, not the e-mail address,
        // so "admin" and "root" arrive at the guard as themselves.
        filterConfig.setOidcUserNameAttribute("preferred_username");

        filterConfig.setRoleSource(OpenIdRoleSource.IdToken);
        filterConfig.setTokenRolesClaim("resource_access.gs-client.roles");
        filterConfig.setRoleConverterString("geoserverAdmin=" + ROLE_ADMINISTRATOR);
        filterConfig.setOnlyExternalListedRoles(true);

        // Hybrid mode, so the same filter also answers Authorization: Bearer requests.
        filterConfig.setEnableResourceServerMode(true);

        filterConfig.setOidcForceAuthorizationUriHttps(false);
        filterConfig.setOidcForceTokenUriHttps(false);
        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames(FILTER_NAME, "anonymous");

        manager.saveSecurityConfig(config);
    }
}
