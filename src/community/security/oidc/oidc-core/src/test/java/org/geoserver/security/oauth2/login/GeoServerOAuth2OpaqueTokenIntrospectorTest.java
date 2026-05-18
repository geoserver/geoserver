/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.oauth2.common.TokenIntrospector;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;

/** Tests for {@link GeoServerOAuth2OpaqueTokenIntrospector}. */
@SuppressWarnings("PMD.UseCollectionIsEmpty")
public class GeoServerOAuth2OpaqueTokenIntrospectorTest {

    private TokenIntrospector mockDelegate;
    private GeoServerSecurityManager mockSecurityManager;
    private GeoServerRoleService mockRoleService;
    private GeoServerOAuth2LoginFilterConfig config;

    @Before
    public void setUp() {
        mockDelegate = mock(TokenIntrospector.class);
        mockSecurityManager = mock(GeoServerSecurityManager.class);
        mockRoleService = mock(GeoServerRoleService.class);

        when(mockSecurityManager.getActiveRoleService()).thenReturn(mockRoleService);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setOidcEnabled(true);
        config.setOidcUserNameAttribute("preferred_username");
    }

    @Test
    public void testConstructorWithValidArgs() {
        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);
        assertNotNull(introspector);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullDelegate() {
        new GeoServerOAuth2OpaqueTokenIntrospector(null, mockSecurityManager, config);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullSecurityManager() {
        new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, null, config);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullConfig() {
        new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, null);
    }

    @Test(expected = BadOpaqueTokenException.class)
    public void testIntrospectWithNullToken() {
        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);
        introspector.introspect(null);
    }

    @Test(expected = BadOpaqueTokenException.class)
    public void testIntrospectWithEmptyToken() {
        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);
        introspector.introspect("");
    }

    @Test(expected = BadOpaqueTokenException.class)
    public void testIntrospectWithBlankToken() {
        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);
        introspector.introspect("   ");
    }

    @Test(expected = BadOpaqueTokenException.class)
    public void testIntrospectWithNullResponse() {
        when(mockDelegate.introspectToken(anyString())).thenReturn(null);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);
        introspector.introspect("valid-token");
    }

    @Test(expected = BadOpaqueTokenException.class)
    public void testIntrospectWithEmptyResponse() {
        when(mockDelegate.introspectToken(anyString())).thenReturn(new HashMap<>());

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);
        introspector.introspect("valid-token");
    }

    @Test(expected = BadOpaqueTokenException.class)
    public void testIntrospectWithInactiveToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", false);
        claims.put("sub", "testuser");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);
        introspector.introspect("valid-token");
    }

    @Test(expected = BadOpaqueTokenException.class)
    public void testIntrospectWithMissingActiveClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);
        introspector.introspect("valid-token");
    }

    @Test
    public void testIntrospectWithActiveToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("preferred_username", "test.user");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("test.user", principal.getName());
    }

    @Test
    public void testIntrospectWithSubAsFallbackPrincipal() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser-sub");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        // Config with no enabled providers to avoid preferred attribute lookup
        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, emptyConfig);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("testuser-sub", principal.getName());
    }

    @Test
    public void testIntrospectNormalizesExpToInstant() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("exp", 1700000000L); // Unix timestamp in seconds

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Object exp = principal.getAttribute("exp");
        assertTrue("exp should be Instant", exp instanceof Instant);
    }

    @Test
    public void testIntrospectNormalizesIatToInstant() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("iat", 1700000000L);

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Object iat = principal.getAttribute("iat");
        assertTrue("iat should be Instant", iat instanceof Instant);
    }

    @Test
    public void testIntrospectNormalizesNbfToInstant() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("nbf", 1700000000L);

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Object nbf = principal.getAttribute("nbf");
        assertTrue("nbf should be Instant", nbf instanceof Instant);
    }

    @Test
    public void testIntrospectWithTimestampInMillis() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("exp", 1700000000000L); // Large value = millis

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Object exp = principal.getAttribute("exp");
        assertTrue("exp should be Instant", exp instanceof Instant);
    }

    @Test
    public void testIntrospectWithTimestampAsString() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("exp", "1700000000");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Object exp = principal.getAttribute("exp");
        assertTrue("exp should be Instant", exp instanceof Instant);
    }

    @Test
    public void testIntrospectNormalizesScopeStringToList() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("scope", "openid profile email");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Object scope = principal.getAttribute("scope");
        assertTrue("scope should be List", scope instanceof List);
        @SuppressWarnings("unchecked")
        List<String> scopes = (List<String>) scope;
        assertEquals(3, scopes.size());
        assertTrue(scopes.contains("openid"));
        assertTrue(scopes.contains("profile"));
        assertTrue(scopes.contains("email"));
    }

    @Test
    public void testIntrospectWithAdminPrincipalGrantsNoRoles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "admin");
        claims.put("preferred_username", "admin");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("admin", principal.getName());
        assertTrue("Admin should have no roles", principal.getAuthorities().isEmpty());
    }

    @Test
    public void testIntrospectWithRootPrincipalGrantsNoRoles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "root");
        claims.put("preferred_username", "root");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("root", principal.getName());
        assertTrue("Root should have no roles", principal.getAuthorities().isEmpty());
    }

    @Test
    public void testIntrospectWithRolesFromClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("roles", List.of("ROLE_USER", "ROLE_EDITOR"));

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config.setTokenRolesClaim("roles");

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        assertNotNull(authorities);
        assertTrue("Should have ROLE_AUTHENTICATED", authorities.stream().anyMatch(a -> a.getAuthority()
                .equals("ROLE_AUTHENTICATED")));
    }

    @Test
    public void testIntrospectWithScopeAsRoleClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("scope", "read write admin");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config.setTokenRolesClaim("scope");

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        assertNotNull(authorities);
        assertTrue(authorities.size() >= 1);
    }

    @Test
    public void testIntrospectWithScpAsRoleClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("scp", "read write");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config.setTokenRolesClaim("scp");

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
    }

    @Test
    public void testIntrospectWithNoRoleClaimConfigured() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config.setTokenRolesClaim(null);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        assertTrue("Should have ROLE_AUTHENTICATED", authorities.stream().anyMatch(a -> a.getAuthority()
                .equals("ROLE_AUTHENTICATED")));
    }

    @Test
    public void testIntrospectWithEmailAsPrincipal() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("email", "user@example.com");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, emptyConfig);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("user@example.com", principal.getName());
    }

    @Test
    public void testIntrospectWithClientIdAsPrincipal() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("client_id", "my-client-app");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, emptyConfig);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("my-client-app", principal.getName());
    }

    @Test
    public void testIntrospectWithMsProviderEnabled() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "ms-user-id");
        claims.put("preferred_username", "ms.user@tenant.com");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setMsEnabled(true);
        config.setMsUserNameAttribute("preferred_username");

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("ms.user@tenant.com", principal.getName());
    }

    @Test
    public void testIntrospectWithGoogleProviderEnabled() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "google-user-id");
        claims.put("email", "user@gmail.com");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setGoogleEnabled(true);
        config.setGoogleUserNameAttribute("email");

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("user@gmail.com", principal.getName());
    }

    @Test
    public void testIntrospectWithGitHubProviderEnabled() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "github-user-id");
        claims.put("id", "12345");
        claims.put("login", "githubuser");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setGitHubEnabled(true);
        config.setGitHubUserNameAttribute("id");

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("12345", principal.getName());
    }

    @Test
    public void testIntrospectWithMultipleProvidersEnabled() {
        // When multiple providers are enabled, fallback to common claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("preferred_username", "multi-provider-user");
        claims.put("sub", "sub-value");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config = new GeoServerOAuth2LoginFilterConfig();
        config.setOidcEnabled(true);
        config.setMsEnabled(true);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("multi-provider-user", principal.getName());
    }

    @Test
    public void testIntrospectWithAzpAsPrincipalFallback() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("azp", "authorized-party-client");

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, emptyConfig);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("authorized-party-client", principal.getName());
    }

    @Test
    public void testIntrospectWithUnknownPrincipal() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        // No principal-related claims

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2LoginFilterConfig emptyConfig = new GeoServerOAuth2LoginFilterConfig();

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, emptyConfig);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        assertEquals("unknown", principal.getName());
    }

    @Test
    public void testIntrospectWithRoleConverterConfiguration() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        claims.put("roles", List.of("user", "editor"));

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        config.setTokenRolesClaim("roles");
        config.setRoleConverterString("user=ROLE_USER;editor=ROLE_EDITOR");

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
    }

    @Test
    public void testIntrospectPreservesInstantClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("active", true);
        claims.put("sub", "testuser");
        Instant now = Instant.now();
        claims.put("exp", now); // Already an Instant

        when(mockDelegate.introspectToken(anyString())).thenReturn(claims);

        GeoServerOAuth2OpaqueTokenIntrospector introspector =
                new GeoServerOAuth2OpaqueTokenIntrospector(mockDelegate, mockSecurityManager, config);

        OAuth2AuthenticatedPrincipal principal = introspector.introspect("valid-token");

        assertNotNull(principal);
        Object exp = principal.getAttribute("exp");
        assertEquals(now, exp);
    }
}
