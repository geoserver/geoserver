/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link GeoServerOAuth2LoginFilterConfig}, specifically covering the fixes in GEOS-11994:
 *
 * <ul>
 *   <li>Bug 1: {@code Optional.toString()} leak producing literal {@code "Optional[http://...]"} in redirect URIs
 *   <li>Bug 2: {@code PROXY_BASE_URL} environment variable / system property not consulted
 *   <li>JIRA #2: Stale redirect URIs after Proxy Base URL change (dynamic resolution)
 *   <li>JIRA #6: {@code oidcUserInfoUri} (and other optional fields) missing from {@code config.xml} on first save
 * </ul>
 */
public class GeoServerOAuth2LoginFilterConfigTest {

    @Before
    public void setup() {
        // Ensure a clean state — no leftover system properties from other tests
        System.clearProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE);
        System.clearProperty("PROXY_BASE_URL");
    }

    @After
    public void tearDown() {
        System.clearProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE);
        System.clearProperty("PROXY_BASE_URL");
    }

    // ── Bug 1: Optional.toString() leak ─────────────────────────────────────

    /** Verify that baseRedirectUri never contains the literal "Optional[" wrapper. */
    @Test
    public void testBaseRedirectUri_noOptionalToStringLeak() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        String base = config.getBaseRedirectUri();
        assertNotNull(base);
        assertFalse("baseRedirectUri must not contain Optional.toString() wrapper", base.contains("Optional["));
    }

    /** Verify that per-provider redirect URIs never contain the literal "Optional[" wrapper. */
    @Test
    public void testRedirectUris_noOptionalToStringLeak() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        assertFalse(config.getOidcRedirectUri().contains("Optional["));
        assertFalse(config.getGoogleRedirectUri().contains("Optional["));
        assertFalse(config.getGitHubRedirectUri().contains("Optional["));
        assertFalse(config.getMsRedirectUri().contains("Optional["));
    }

    // ── Bug 2: PROXY_BASE_URL takes priority ────────────────────────────────

    /** PROXY_BASE_URL system property should take priority over the test fallback. */
    @Test
    public void testProxyBaseUrlSystemProperty_takesPriority() {
        System.setProperty("PROXY_BASE_URL", "https://proxy.example.com/geoserver");
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://fallback/geoserver");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        assertEquals("https://proxy.example.com/geoserver/", config.getBaseRedirectUri());
    }

    /** PROXY_BASE_URL should have a trailing slash appended if missing. */
    @Test
    public void testProxyBaseUrl_trailingSlashAppended() {
        System.setProperty("PROXY_BASE_URL", "https://proxy.example.com/geoserver");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        assertTrue(config.getBaseRedirectUri().endsWith("/"));
    }

    /** PROXY_BASE_URL should not double-add a trailing slash if already present. */
    @Test
    public void testProxyBaseUrl_trailingSlashNotDoubled() {
        System.setProperty("PROXY_BASE_URL", "https://proxy.example.com/geoserver/");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        assertEquals("https://proxy.example.com/geoserver/", config.getBaseRedirectUri());
    }

    // ── JIRA #2: Dynamic redirect URI resolution ────────────────────────────

    /** Redirect URIs should reflect the current base after calling calculateRedirectUris(). */
    @Test
    public void testRedirectUris_refreshFromCurrentBase() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://host-a/geoserver");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        assertEquals(
                "http://host-a/geoserver/web/login/oauth2/code/oidc",
                config.getOidcRedirectUri());

        // Simulate a Proxy Base URL change (e.g. admin updated global settings)
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://host-b/geoserver");

        // Before refresh — per-provider fields still contain old values
        assertEquals(
                "http://host-a/geoserver/web/login/oauth2/code/oidc",
                config.getOidcRedirectUri());

        // After refresh (as done by createFilter() or the Wicket AJAX handler) — updated
        config.calculateRedirectUris();
        assertEquals(
                "http://host-b/geoserver/web/login/oauth2/code/oidc",
                config.getOidcRedirectUri());
    }

    /** When setBaseRedirectUri is called explicitly, the set value should be honored. */
    @Test
    public void testSetBaseRedirectUri_honorsExplicitValue() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://dynamic/geoserver");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        config.setBaseRedirectUri("http://explicit:8080/geoserver/");
        config.calculateRedirectUris();

        assertEquals("http://explicit:8080/geoserver/", config.getBaseRedirectUri());
        assertEquals(
                "http://explicit:8080/geoserver/web/login/oauth2/code/oidc",
                config.getOidcRedirectUri());
    }

    /** All four provider redirect URIs should use the same dynamic base. */
    @Test
    public void testAllProviderRedirectUris_useSameBase() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://example/gs");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        String base = "http://example/gs/web/login/oauth2/code/";
        assertEquals(base + "oidc", config.getOidcRedirectUri());
        assertEquals(base + "google", config.getGoogleRedirectUri());
        assertEquals(base + "gitHub", config.getGitHubRedirectUri());
        assertEquals(base + "microsoft", config.getMsRedirectUri());
    }

    // ── JIRA #6: Empty-to-null normalization ────────────────────────────────

    /** Setting an optional URI to empty string should normalize to null. */
    @Test
    public void testSetOidcUserInfoUri_emptyNormalizesToNull() {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        config.setOidcUserInfoUri("http://example.com/userinfo");
        assertEquals("http://example.com/userinfo", config.getOidcUserInfoUri());

        config.setOidcUserInfoUri("");
        assertNull(config.getOidcUserInfoUri());
    }

    /** Setting an optional URI to blank string should normalize to null. */
    @Test
    public void testSetOidcUserInfoUri_blankNormalizesToNull() {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        config.setOidcUserInfoUri("   ");
        assertNull(config.getOidcUserInfoUri());
    }

    /** Setting an optional URI to null should stay null (not throw). */
    @Test
    public void testSetOidcUserInfoUri_nullStaysNull() {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        config.setOidcUserInfoUri(null);
        assertNull(config.getOidcUserInfoUri());
    }

    /** Verify normalization applies to all optional string setters. */
    @Test
    public void testEmptyNormalization_allOptionalSetters() {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        config.setOidcLogoutUri("");
        assertNull("oidcLogoutUri", config.getOidcLogoutUri());

        config.setOidcResponseMode("");
        assertNull("oidcResponseMode", config.getOidcResponseMode());

        config.setOidcDiscoveryUri("");
        assertNull("oidcDiscoveryUri", config.getOidcDiscoveryUri());

        config.setOidcJwsAlgorithmName("");
        assertNull("oidcJwsAlgorithmName", config.getOidcJwsAlgorithmName());

        config.setOidcIntrospectionUrl("");
        assertNull("oidcIntrospectionUrl", config.getOidcIntrospectionUrl());

        config.setPostLogoutRedirectUri("");
        assertNull("postLogoutRedirectUri", config.getPostLogoutRedirectUri());

        config.setTokenRolesClaim("");
        assertNull("tokenRolesClaim", config.getTokenRolesClaim());

        config.setRoleConverterString("");
        assertNull("roleConverterString", config.getRoleConverterString());

        config.setMsGraphAppRoleAssignmentsObjectId("");
        assertNull("msGraphAppRoleAssignmentsObjectId", config.getMsGraphAppRoleAssignmentsObjectId());
    }

    /** Valid non-empty values should be preserved, not normalized away. */
    @Test
    public void testNormalization_validValuesPreserved() {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        config.setOidcUserInfoUri("http://example.com/userinfo");
        assertEquals("http://example.com/userinfo", config.getOidcUserInfoUri());

        config.setOidcLogoutUri("http://example.com/logout");
        assertEquals("http://example.com/logout", config.getOidcLogoutUri());

        config.setOidcResponseMode("query");
        assertEquals("query", config.getOidcResponseMode());
    }

    // ── Dropdown selector (new in v2) ───────────────────────────────────────

    /** Verify that setSelectedProvider enables only the selected provider. */
    @Test
    public void testSelectedProvider_mutuallyExclusive() {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

        config.setSelectedProvider("google");
        assertTrue(config.isGoogleEnabled());
        assertFalse(config.isOidcEnabled());
        assertFalse(config.isGitHubEnabled());
        assertFalse(config.isMsEnabled());
        assertEquals("google", config.getSelectedProvider());

        config.setSelectedProvider("oidc");
        assertTrue(config.isOidcEnabled());
        assertFalse(config.isGoogleEnabled());
        assertEquals("oidc", config.getSelectedProvider());
    }

    /** Default provider should be OIDC when none is explicitly enabled. */
    @Test
    public void testSelectedProvider_defaultsToOidc() {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        assertEquals("oidc", config.getSelectedProvider());
    }
}
