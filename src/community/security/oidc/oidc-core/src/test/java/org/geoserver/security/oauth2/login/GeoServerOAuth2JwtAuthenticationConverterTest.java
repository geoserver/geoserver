/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Tests for {@link GeoServerOAuth2JwtAuthenticationConverter}. */
@SuppressWarnings("PMD.UseCollectionIsEmpty")
public class GeoServerOAuth2JwtAuthenticationConverterTest {

    private GeoServerSecurityManager mockSecurityManager;
    private GeoServerRoleService mockRoleService;
    private GeoServerOAuth2LoginFilterConfig config;

    @Before
    public void setUp() {
        mockSecurityManager = mock(GeoServerSecurityManager.class);
        mockRoleService = mock(GeoServerRoleService.class);

        when(mockSecurityManager.getActiveRoleService()).thenReturn(mockRoleService);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setOidcEnabled(true);
        config.setOidcUserNameAttribute("preferred_username");
    }

    private Jwt createJwt(Map<String, Object> claims) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }

    @Test
    public void testConstructorWithValidArgs() {
        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);
        assertNotNull(converter);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullSecurityManager() {
        new GeoServerOAuth2JwtAuthenticationConverter(null, config);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullConfig() {
        new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, null);
    }

    @Test
    public void testConvertWithNullJwt() {
        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(null);
        assertNull(result);
    }

    @Test
    public void testConvertWithValidJwt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("preferred_username", "test.user");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertTrue(result instanceof JwtAuthenticationToken);
        assertEquals("test.user", result.getName());
    }

    @Test
    public void testConvertWithSubAsFallback() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser-sub");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        // Config with no enabled providers to test fallback
        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, emptyConfig);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("testuser-sub", result.getName());
    }

    @Test
    public void testConvertWithEmailAsPrincipal() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "user@example.com");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, emptyConfig);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("user@example.com", result.getName());
    }

    @Test
    public void testConvertWithClientIdAsPrincipal() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_id", "my-service-account");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, emptyConfig);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("my-service-account", result.getName());
    }

    @Test
    public void testConvertWithAzpAsPrincipal() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("azp", "authorized-party");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, emptyConfig);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("authorized-party", result.getName());
    }

    @Test
    public void testConvertWithAdminPrincipalGrantsNoRoles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "admin");
        claims.put("preferred_username", "admin");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("admin", result.getName());
        assertTrue("Admin should have no roles", result.getAuthorities().isEmpty());
    }

    @Test
    public void testConvertWithRootPrincipalGrantsNoRoles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "root");
        claims.put("preferred_username", "root");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("root", result.getName());
        assertTrue("Root should have no roles", result.getAuthorities().isEmpty());
    }

    @Test
    public void testConvertWithRolesFromClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        claims.put("roles", List.of("ROLE_USER", "ROLE_EDITOR"));

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("roles");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities);
        assertTrue("Should have ROLE_AUTHENTICATED", authorities.stream().anyMatch(a -> a.getAuthority()
                .equals("ROLE_AUTHENTICATED")));
    }

    @Test
    public void testConvertWithScopeAsRoleClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        claims.put("scope", "read write admin");

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("scope");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities);
        assertTrue(authorities.size() >= 1);
    }

    @Test
    public void testConvertWithScpAsRoleClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        claims.put("scp", "read write");

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("scp");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
    }

    @Test
    public void testConvertWithNoRoleClaimConfigured() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim(null);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertTrue("Should have ROLE_AUTHENTICATED", authorities.stream().anyMatch(a -> a.getAuthority()
                .equals("ROLE_AUTHENTICATED")));
    }

    @Test
    public void testConvertWithEmptyRoleClaimConfigured() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertTrue("Should have ROLE_AUTHENTICATED", authorities.stream().anyMatch(a -> a.getAuthority()
                .equals("ROLE_AUTHENTICATED")));
    }

    @Test
    public void testConvertWithMsProviderEnabled() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "ms-user-id");
        claims.put("preferred_username", "ms.user@tenant.com");
        claims.put("iss", "https://login.microsoftonline.com/tenant/v2.0");

        Jwt jwt = createJwt(claims);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setMsEnabled(true);
        config.setMsUserNameAttribute("preferred_username");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("ms.user@tenant.com", result.getName());
    }

    @Test
    public void testConvertWithGoogleProviderEnabled() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "google-user-id");
        claims.put("email", "user@gmail.com");
        claims.put("iss", "https://accounts.google.com");

        Jwt jwt = createJwt(claims);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setGoogleEnabled(true);
        config.setGoogleUserNameAttribute("email");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("user@gmail.com", result.getName());
    }

    @Test
    public void testConvertWithGitHubProviderEnabled() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "github-user-id");
        claims.put("id", "12345");
        claims.put("iss", "https://github.com");

        Jwt jwt = createJwt(claims);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setGitHubEnabled(true);
        config.setGitHubUserNameAttribute("id");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("12345", result.getName());
    }

    @Test
    public void testConvertWithMultipleProvidersEnabled() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("preferred_username", "multi-provider-user");
        claims.put("sub", "sub-value");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        // Multiple providers enabled - should fallback to common claims
        config = new GeoServerOAuth2LoginFilterConfig();
        config.setOidcEnabled(true);
        config.setMsEnabled(true);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        // With multiple providers, it falls back to common claim order: preferred_username first
        assertEquals("multi-provider-user", result.getName());
    }

    @Test
    public void testConvertWithUnknownPrincipal() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://issuer.example.com");
        // No principal-related claims at all

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, emptyConfig);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        // Should use "unknown" as fallback
        assertEquals("unknown", result.getName());
    }

    @Test
    public void testConvertReturnsJwtAuthenticationToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertTrue(result instanceof JwtAuthenticationToken);

        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) result;
        assertNotNull(jwtToken.getToken());
        assertEquals(jwt, jwtToken.getToken());
    }

    @Test
    public void testConvertWithRoleConverterConfiguration() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        claims.put("roles", List.of("user", "editor"));

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("roles");
        config.setRoleConverterString("user=ROLE_USER;editor=ROLE_EDITOR");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
    }

    @Test
    public void testConvertWithOnlyExternalListedRoles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        claims.put("roles", List.of("external_role", "another_role"));

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("roles");
        config.setOnlyExternalListedRoles(true);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
    }

    @Test
    public void testConvertWithNestedClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");

        // Nested claims structure (e.g., realm_access.roles in Keycloak)
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("realm_admin", "realm_user"));
        claims.put("realm_access", realmAccess);

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("realm_access.roles");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
    }

    @Test
    public void testConvertWithScopeFallbackToScp() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        // scp as space-delimited string (alternate claim for scope)
        claims.put("scp", "api.read api.write");

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("scope"); // configured as scope but data is in scp

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
    }

    @Test
    public void testConvertWithScpFallbackToScope() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        claims.put("scope", "read write"); // scope instead of scp

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("scp"); // configured as scp but data is in scope

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
    }

    @Test
    public void testConvertPreservesJwtCredentials() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        claims.put("aud", "geoserver-api");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) result;
        assertEquals("geoserver-api", jwtToken.getToken().getAudience().get(0));
    }

    @Test
    public void testConvertWithCaseInsensitiveAdminCheck() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "ADMIN"); // uppercase
        claims.put("preferred_username", "ADMIN");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertTrue(
                "ADMIN (uppercase) should have no roles",
                result.getAuthorities().isEmpty());
    }

    @Test
    public void testConvertWithCaseInsensitiveRootCheck() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "ROOT"); // uppercase
        claims.put("preferred_username", "ROOT");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = createJwt(claims);

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertTrue(
                "ROOT (uppercase) should have no roles", result.getAuthorities().isEmpty());
    }

    @Test
    public void testConvertAlwaysAddsAuthenticatedRole() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");
        claims.put("roles", List.of("ROLE_CUSTOM_ROLE")); // Custom role without ROLE_AUTHENTICATED

        Jwt jwt = createJwt(claims);

        config.setTokenRolesClaim("roles");

        GeoServerOAuth2JwtAuthenticationConverter converter =
                new GeoServerOAuth2JwtAuthenticationConverter(mockSecurityManager, config);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertTrue("Should always have ROLE_AUTHENTICATED", authorities.stream().anyMatch(a -> a.getAuthority()
                .equals("ROLE_AUTHENTICATED")));
    }
}
