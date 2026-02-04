/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.common.TokenIntrospector;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Extended integration tests using Keycloak testcontainer.
 *
 * <p>These tests cover:
 *
 * <ul>
 *   <li>Token introspection endpoint
 *   <li>Direct Grant (Resource Owner Password Credentials) token acquisition
 *   <li>Invalid credential handling
 *   <li>UserInfo endpoint
 *   <li>OIDC Discovery and JWKS endpoints
 * </ul>
 */
public class KeyCloakExtendedIntegrationTest extends KeyCloakIntegrationTestSupport {

    @BeforeClass
    public static void beforeClassLocal() {
        System.setProperty("OPENID_TEST_GS_PROXY_BASE", "http://localhost/geoserver");
    }

    @Override
    protected String getLogConfiguration() {
        return "VERBOSE_LOGGING";
    }

    // ==================== Token Acquisition Helper ====================

    /**
     * Get an access token from Keycloak using the Resource Owner Password Credentials Grant. This is useful for testing
     * bearer token authentication without going through the browser flow.
     *
     * @param username the username
     * @param password the password
     * @return JSON response containing access_token, refresh_token, id_token, etc.
     */
    protected JSONObject getTokenFromKeycloak(String username, String password) throws Exception {
        String tokenUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/token";

        URL url = new URL(tokenUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String body = "grant_type=password"
                + "&client_id=" + oidcClient
                + "&client_secret=" + oidcClientSecret
                + "&username=" + username
                + "&password=" + password
                + "&scope=openid profile email";

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        connection.disconnect();

        return JSONObject.fromObject(response);
    }

    // ==================== Token Introspection Tests ====================

    /** Test token introspection with a valid access token from Keycloak. */
    @Test
    public void test_token_introspection_valid_token() throws Exception {
        // Get a real token from Keycloak
        JSONObject tokenResponse = getTokenFromKeycloak("admin", "admin");
        String accessToken = tokenResponse.getString("access_token");
        assertNotNull("Should get access token", accessToken);

        // Use TokenIntrospector to introspect the token
        String introspectionUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/token/introspect";
        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, oidcClient, oidcClientSecret);

        Map<String, Object> introspectionResult = introspector.introspectToken(accessToken);

        assertNotNull("Introspection result should not be null", introspectionResult);
        assertEquals("Token should be active", true, introspectionResult.get("active"));
        assertEquals("admin", introspectionResult.get("preferred_username"));
        assertEquals("admin@example.com", introspectionResult.get("email"));
    }

    /** Test token introspection with an invalid/expired token. */
    @Test
    public void test_token_introspection_invalid_token() throws Exception {
        String introspectionUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/token/introspect";
        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, oidcClient, oidcClientSecret);

        Map<String, Object> introspectionResult = introspector.introspectToken("invalid-token-12345");

        assertNotNull("Introspection result should not be null", introspectionResult);
        assertEquals("Invalid token should not be active", false, introspectionResult.get("active"));
    }

    /** Test token introspection with client_secret_post authentication method. */
    @Test
    public void test_token_introspection_client_secret_post() throws Exception {
        JSONObject tokenResponse = getTokenFromKeycloak("user_sample1", "user_sample1");
        String accessToken = tokenResponse.getString("access_token");

        String introspectionUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/token/introspect";
        TokenIntrospector introspector =
                new TokenIntrospector(introspectionUrl, oidcClient, oidcClientSecret, true); // client_secret_post

        Map<String, Object> introspectionResult = introspector.introspectToken(accessToken);

        assertNotNull("Introspection result should not be null", introspectionResult);
        assertEquals("Token should be active", true, introspectionResult.get("active"));
        assertEquals("user_sample1", introspectionResult.get("preferred_username"));
    }

    /** Test introspecting tokens for different users returns different results. */
    @Test
    public void test_token_introspection_different_users() throws Exception {
        String introspectionUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/token/introspect";
        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, oidcClient, oidcClientSecret);

        // Admin token
        JSONObject adminTokenResponse = getTokenFromKeycloak("admin", "admin");
        Map<String, Object> adminIntrospection =
                introspector.introspectToken(adminTokenResponse.getString("access_token"));

        // User token
        JSONObject userTokenResponse = getTokenFromKeycloak("user_sample1", "user_sample1");
        Map<String, Object> userIntrospection =
                introspector.introspectToken(userTokenResponse.getString("access_token"));

        // Verify different users
        assertEquals("admin", adminIntrospection.get("preferred_username"));
        assertEquals("user_sample1", userIntrospection.get("preferred_username"));
        assertNotEquals("Should be different users", adminIntrospection.get("sub"), userIntrospection.get("sub"));
    }

    // ==================== UserInfo Endpoint Tests ====================

    /** Test fetching user info from Keycloak's userinfo endpoint. */
    @Test
    public void test_userinfo_endpoint_admin() throws Exception {
        JSONObject tokenResponse = getTokenFromKeycloak("admin", "admin");
        String accessToken = tokenResponse.getString("access_token");

        String userInfoUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/userinfo";

        URL url = new URL(userInfoUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        connection.disconnect();

        JSONObject userInfo = JSONObject.fromObject(response);

        assertEquals("admin@example.com", userInfo.getString("email"));
        assertEquals("admin", userInfo.getString("preferred_username"));
        assertEquals("admin", userInfo.getString("family_name"));
        assertEquals("admin", userInfo.getString("given_name"));
        assertTrue(userInfo.getBoolean("email_verified"));
    }

    /** Test fetching user info for a regular user. */
    @Test
    public void test_userinfo_endpoint_regular_user() throws Exception {
        JSONObject tokenResponse = getTokenFromKeycloak("user_sample1", "user_sample1");
        String accessToken = tokenResponse.getString("access_token");

        String userInfoUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/userinfo";

        URL url = new URL(userInfoUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        connection.disconnect();

        JSONObject userInfo = JSONObject.fromObject(response);

        assertEquals("user_sample1@example.com", userInfo.getString("email"));
        assertEquals("user_sample1", userInfo.getString("preferred_username"));
        assertEquals("sample1", userInfo.getString("family_name"));
        assertEquals("user", userInfo.getString("given_name"));
    }

    /** Test userinfo endpoint with invalid token returns error. */
    @Test
    public void test_userinfo_endpoint_invalid_token() throws Exception {
        String userInfoUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/userinfo";

        URL url = new URL(userInfoUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer invalid-token");

        int responseCode = connection.getResponseCode();
        connection.disconnect();

        assertEquals("Invalid token should return 401", 401, responseCode);
    }

    // ==================== Direct Grant Token Tests ====================

    /** Test getting tokens for different users. */
    @Test
    public void test_direct_grant_different_users() throws Exception {
        // Admin user
        JSONObject adminToken = getTokenFromKeycloak("admin", "admin");
        assertNotNull(adminToken.getString("access_token"));
        assertNotNull(adminToken.getString("id_token"));
        assertNotNull(adminToken.getString("refresh_token"));
        assertEquals("Bearer", adminToken.getString("token_type"));

        // Regular user
        JSONObject userToken = getTokenFromKeycloak("user_sample1", "user_sample1");
        assertNotNull(userToken.getString("access_token"));
        assertNotNull(userToken.getString("id_token"));

        // Tokens should be different
        assertNotEquals(
                "Different users should get different tokens",
                adminToken.getString("access_token"),
                userToken.getString("access_token"));
    }

    /** Test token response contains expected fields. */
    @Test
    public void test_direct_grant_token_response_structure() throws Exception {
        JSONObject tokenResponse = getTokenFromKeycloak("admin", "admin");

        // Standard OAuth2 fields
        assertTrue("Should have access_token", tokenResponse.has("access_token"));
        assertTrue("Should have token_type", tokenResponse.has("token_type"));
        assertTrue("Should have expires_in", tokenResponse.has("expires_in"));

        // OIDC fields
        assertTrue("Should have id_token", tokenResponse.has("id_token"));
        assertTrue("Should have refresh_token", tokenResponse.has("refresh_token"));
        assertTrue("Should have scope", tokenResponse.has("scope"));

        // Verify token type
        assertEquals("Bearer", tokenResponse.getString("token_type"));

        // Verify scope contains requested scopes
        String scope = tokenResponse.getString("scope");
        assertTrue("Scope should contain openid", scope.contains("openid"));
        assertTrue("Scope should contain profile", scope.contains("profile"));
        assertTrue("Scope should contain email", scope.contains("email"));
    }

    /** Test that invalid credentials fail to get a token. */
    @Test
    public void test_direct_grant_invalid_credentials() throws Exception {
        String tokenUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/token";

        URL url = new URL(tokenUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String body = "grant_type=password"
                + "&client_id=" + oidcClient
                + "&client_secret=" + oidcClientSecret
                + "&username=admin"
                + "&password=wrongpassword"
                + "&scope=openid";

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        // Should get 401 Unauthorized
        int responseCode = connection.getResponseCode();
        assertEquals("Invalid credentials should return 401", 401, responseCode);

        connection.disconnect();
    }

    /** Test that non-existent user fails to get a token. */
    @Test
    public void test_direct_grant_nonexistent_user() throws Exception {
        String tokenUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/token";

        URL url = new URL(tokenUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String body = "grant_type=password"
                + "&client_id=" + oidcClient
                + "&client_secret=" + oidcClientSecret
                + "&username=nonexistent_user"
                + "&password=somepassword"
                + "&scope=openid";

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        assertEquals("Non-existent user should return 401", 401, responseCode);

        connection.disconnect();
    }

    // ==================== JWKS Endpoint Test ====================

    /** Test that the JWKS endpoint is accessible and returns valid keys. */
    @Test
    public void test_jwks_endpoint_accessible() throws Exception {
        String jwksUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/certs";

        URL url = new URL(jwksUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(200, connection.getResponseCode());

        String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        connection.disconnect();

        JSONObject jwks = JSONObject.fromObject(response);
        assertTrue("JWKS should contain keys", jwks.has("keys"));
        assertFalse(
                "JWKS should have at least one key", jwks.getJSONArray("keys").isEmpty());

        // Verify key structure
        JSONObject firstKey = jwks.getJSONArray("keys").getJSONObject(0);
        assertTrue("Key should have 'kty' (key type)", firstKey.has("kty"));
        assertTrue("Key should have 'kid' (key id)", firstKey.has("kid"));
        assertTrue("Key should have 'use' (usage)", firstKey.has("use"));
    }

    // ==================== Discovery Endpoint Test ====================

    /** Test that the OpenID Connect discovery endpoint is accessible. */
    @Test
    public void test_oidc_discovery_endpoint() throws Exception {
        String discoveryUrl = authServerUrl + "/realms/gs-realm/.well-known/openid-configuration";

        URL url = new URL(discoveryUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(200, connection.getResponseCode());

        String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        connection.disconnect();

        JSONObject discovery = JSONObject.fromObject(response);

        // Required OIDC Discovery fields
        assertTrue("Should have issuer", discovery.has("issuer"));
        assertTrue("Should have authorization_endpoint", discovery.has("authorization_endpoint"));
        assertTrue("Should have token_endpoint", discovery.has("token_endpoint"));
        assertTrue("Should have userinfo_endpoint", discovery.has("userinfo_endpoint"));
        assertTrue("Should have jwks_uri", discovery.has("jwks_uri"));
        assertTrue("Should have introspection_endpoint", discovery.has("introspection_endpoint"));
        assertTrue("Should have end_session_endpoint", discovery.has("end_session_endpoint"));

        // Verify issuer matches our realm
        assertTrue(
                "Issuer should contain gs-realm", discovery.getString("issuer").contains("gs-realm"));
    }

    /** Test discovery endpoint returns supported scopes. */
    @Test
    public void test_oidc_discovery_supported_scopes() throws Exception {
        String discoveryUrl = authServerUrl + "/realms/gs-realm/.well-known/openid-configuration";

        URL url = new URL(discoveryUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        connection.disconnect();

        JSONObject discovery = JSONObject.fromObject(response);

        assertTrue("Should have scopes_supported", discovery.has("scopes_supported"));

        String scopesSupported = discovery.getJSONArray("scopes_supported").toString();
        assertTrue("Should support openid scope", scopesSupported.contains("openid"));
        assertTrue("Should support profile scope", scopesSupported.contains("profile"));
        assertTrue("Should support email scope", scopesSupported.contains("email"));
    }

    // ==================== ID Token Claims Test ====================

    /** Test that ID token contains expected claims. */
    @Test
    public void test_id_token_contains_expected_claims() throws Exception {
        JSONObject tokenResponse = getTokenFromKeycloak("admin", "admin");
        String idToken = tokenResponse.getString("id_token");

        // Decode JWT (just the payload, we're not validating signature here)
        String[] parts = idToken.split("\\.");
        assertEquals("JWT should have 3 parts", 3, parts.length);

        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JSONObject payload = JSONObject.fromObject(payloadJson);

        // Standard OIDC claims
        assertTrue("Should have 'iss' (issuer)", payload.has("iss"));
        assertTrue("Should have 'sub' (subject)", payload.has("sub"));
        assertTrue("Should have 'aud' (audience)", payload.has("aud"));
        assertTrue("Should have 'exp' (expiration)", payload.has("exp"));
        assertTrue("Should have 'iat' (issued at)", payload.has("iat"));

        // User claims (from profile/email scopes)
        assertEquals("admin@example.com", payload.getString("email"));
        assertEquals("admin", payload.getString("preferred_username"));
        assertTrue(payload.getBoolean("email_verified"));

        // Verify audience contains our client
        String aud = payload.get("aud").toString();
        assertTrue("Audience should contain gs-client", aud.contains(oidcClient));
    }

    // ==================== Setup ====================

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        String baseKeycloakUrl = keycloakContainer.getAuthServerUrl() + "/realms/gs-realm/protocol/openid-connect";

        GeoServerSecurityManager manager = getSecurityManager();

        // Setup OAuth2 Login filter
        GeoServerOAuth2LoginFilterConfig loginConfig = new GeoServerOAuth2LoginFilterConfig();
        loginConfig.setName("openidconnect");
        loginConfig.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        loginConfig.setOidcEnabled(true);
        loginConfig.setOidcClientId(oidcClient);
        loginConfig.setOidcClientSecret(oidcClientSecret);
        loginConfig.setBaseRedirectUri("http://localhost:8080/geoserver/");
        loginConfig.calculateRedirectUris();
        loginConfig.setOidcTokenUri(baseKeycloakUrl + "/token");
        loginConfig.setOidcAuthorizationUri(baseKeycloakUrl + "/authorize");
        loginConfig.setOidcUserInfoUri(baseKeycloakUrl + "/userinfo");
        loginConfig.setOidcLogoutUri(baseKeycloakUrl + "/endSession");
        loginConfig.setOidcJwkSetUri(baseKeycloakUrl + "/certs");
        loginConfig.setOidcScopes("openid profile email phone address");
        loginConfig.setEnableRedirectAuthenticationEntryPoint(false);
        loginConfig.setOidcUserNameAttribute("email");
        loginConfig.setRoleSource(GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.IdToken);
        loginConfig.setTokenRolesClaim("resource_access.gs-client.roles");
        loginConfig.setRoleConverterString("geoserverAdmin=ROLE_ADMINISTRATOR");
        loginConfig.setOnlyExternalListedRoles(true);
        loginConfig.setOidcForceAuthorizationUriHttps(false);
        loginConfig.setOidcForceTokenUriHttps(false);
        manager.saveFilter(loginConfig);

        // Configure filter chains
        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();

        // Web chain uses login filter
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("openidconnect", "anonymous");

        manager.saveSecurityConfig(config);
    }
}
