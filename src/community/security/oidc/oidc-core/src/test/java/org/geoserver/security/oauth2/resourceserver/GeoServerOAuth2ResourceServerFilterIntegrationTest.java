/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * Integration tests for {@link GeoServerOAuth2ResourceServerAuthenticationProvider} that test the actual filter
 * creation with mocked OIDC endpoints using WireMock.
 *
 * <p>These tests verify that the createFiltersImpl() method works correctly when connecting to a real (mocked) OIDC
 * provider.
 */
@TestSetup(run = TestSetupFrequency.REPEAT)
public class GeoServerOAuth2ResourceServerFilterIntegrationTest extends GeoServerSystemTestSupport {

    private WireMockServer openIdService;
    private String issuerUri;

    @Before
    public void setupWireMock() throws Exception {
        openIdService = new WireMockServer(wireMockConfig().dynamicPort());
        openIdService.start();

        issuerUri = "http://localhost:" + openIdService.port();

        // Stub the OIDC discovery endpoint
        String discoveryResponse = createDiscoveryResponse(issuerUri);
        openIdService.stubFor(WireMock.get(urlEqualTo("/.well-known/openid-configuration"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(discoveryResponse)));

        // Stub the JWKS endpoint
        String jwksResponse = createJwksResponse();
        openIdService.stubFor(WireMock.get(urlEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jwksResponse)));
    }

    @After
    public void tearDown() {
        if (openIdService != null) {
            openIdService.stop();
        }
    }

    @Test
    public void testCreateFilterWithValidIssuer() throws IOException {
        // Given
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2ResourceServerAuthenticationProvider provider =
                new GeoServerOAuth2ResourceServerAuthenticationProvider(manager);

        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setName("testResourceServerFilter");
        config.setIssuerUri(issuerUri);
        config.setRoleSource(OpenIdRoleSource.IdToken);

        // When
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                (GeoServerOAuth2ResourceServerAuthenticationFilter) provider.createFilter(config);

        // Then
        assertNotNull("Filter should be created", filter);
        assertTrue("Filter should be applicable for HTML", filter.applicableForHtml());
        assertTrue("Filter should be applicable for services", filter.applicableForServices());
    }

    @Test
    public void testCreateFilterWithAccessTokenRoleSource() throws IOException {
        // Given
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2ResourceServerAuthenticationProvider provider =
                new GeoServerOAuth2ResourceServerAuthenticationProvider(manager);

        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setName("testResourceServerFilter");
        config.setIssuerUri(issuerUri);
        config.setRoleSource(OpenIdRoleSource.AccessToken);

        // When
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                (GeoServerOAuth2ResourceServerAuthenticationFilter) provider.createFilter(config);

        // Then
        assertNotNull("Filter should be created with AccessToken source", filter);
    }

    @Test
    public void testCreateFilterWithRoleServiceSource() throws IOException {
        // Given
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2ResourceServerAuthenticationProvider provider =
                new GeoServerOAuth2ResourceServerAuthenticationProvider(manager);

        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setName("testResourceServerFilter");
        config.setIssuerUri(issuerUri);
        config.setRoleSource(PreAuthenticatedUserNameRoleSource.RoleService);
        config.setRoleServiceName("default");

        // When
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                (GeoServerOAuth2ResourceServerAuthenticationFilter) provider.createFilter(config);

        // Then
        assertNotNull("Filter should be created with RoleService source", filter);
    }

    @Test
    public void testCreateFilterWithUserGroupServiceSource() throws IOException {
        // Given
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2ResourceServerAuthenticationProvider provider =
                new GeoServerOAuth2ResourceServerAuthenticationProvider(manager);

        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setName("testResourceServerFilter");
        config.setIssuerUri(issuerUri);
        config.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        config.setUserGroupServiceName("default");

        // When
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                (GeoServerOAuth2ResourceServerAuthenticationFilter) provider.createFilter(config);

        // Then
        assertNotNull("Filter should be created with UserGroupService source", filter);
    }

    @Test
    public void testCreateFilterWithUserInfoSource() throws IOException {
        // Given
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2ResourceServerAuthenticationProvider provider =
                new GeoServerOAuth2ResourceServerAuthenticationProvider(manager);

        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setName("testResourceServerFilter");
        config.setIssuerUri(issuerUri);
        config.setRoleSource(OpenIdRoleSource.UserInfo);

        // When
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                (GeoServerOAuth2ResourceServerAuthenticationFilter) provider.createFilter(config);

        // Then
        assertNotNull("Filter should be created with UserInfo source", filter);
    }

    @Test
    public void testCreateFilterMultipleTimes() throws IOException {
        // Given - ensure the filter can be created multiple times (no state leakage)
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2ResourceServerAuthenticationProvider provider =
                new GeoServerOAuth2ResourceServerAuthenticationProvider(manager);

        GeoServerOAuth2ResourceServerFilterConfig config1 = new GeoServerOAuth2ResourceServerFilterConfig();
        config1.setName("testResourceServerFilter1");
        config1.setIssuerUri(issuerUri);

        GeoServerOAuth2ResourceServerFilterConfig config2 = new GeoServerOAuth2ResourceServerFilterConfig();
        config2.setName("testResourceServerFilter2");
        config2.setIssuerUri(issuerUri);

        // When
        GeoServerOAuth2ResourceServerAuthenticationFilter filter1 =
                (GeoServerOAuth2ResourceServerAuthenticationFilter) provider.createFilter(config1);
        GeoServerOAuth2ResourceServerAuthenticationFilter filter2 =
                (GeoServerOAuth2ResourceServerAuthenticationFilter) provider.createFilter(config2);

        // Then
        assertNotNull(filter1);
        assertNotNull(filter2);
        // They should be different instances
        assertNotSame("Filters should be different instances", filter1, filter2);
    }

    @Test
    public void testCreateFilterWithHeaderRoleSource() throws IOException {
        // Given - test with Header role source which triggers the converter loading path
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2ResourceServerAuthenticationProvider provider =
                new GeoServerOAuth2ResourceServerAuthenticationProvider(manager);

        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setName("testResourceServerFilter");
        config.setIssuerUri(issuerUri);
        config.setRoleSource(PreAuthenticatedUserNameRoleSource.Header);
        config.setRolesHeaderAttribute("X-Roles");

        // When
        GeoServerOAuth2ResourceServerAuthenticationFilter filter =
                (GeoServerOAuth2ResourceServerAuthenticationFilter) provider.createFilter(config);

        // Then
        assertNotNull("Filter should be created with Header role source", filter);
    }

    private String createDiscoveryResponse(String issuerUri) {
        return String.format(
                """
            {
              "issuer": "%s",
              "authorization_endpoint": "%s/authorize",
              "token_endpoint": "%s/token",
              "userinfo_endpoint": "%s/userinfo",
              "jwks_uri": "%s/jwks",
              "response_types_supported": ["code", "token", "id_token"],
              "subject_types_supported": ["public"],
              "id_token_signing_alg_values_supported": ["RS256"],
              "scopes_supported": ["openid", "profile", "email"],
              "token_endpoint_auth_methods_supported": ["client_secret_basic", "client_secret_post"],
              "claims_supported": ["sub", "iss", "name", "email"]
            }
            """,
                issuerUri, issuerUri, issuerUri, issuerUri, issuerUri);
    }

    private String createJwksResponse() {
        // Return a valid JWKS with an RSA key
        return """
            {
              "keys": [
                {
                  "kty": "RSA",
                  "alg": "RS256",
                  "use": "sig",
                  "kid": "test-key-id",
                  "n": "pB-AhRkieLN5sAgc2hhsMWvScc329YmuJ1LpsW7LmgezwpWWYKzUIjkdzF1TVfVuhdQ_sI0-qBRzqO0zpFSNtiP33912UxNBd-VFBxlkbYkOC3WccDj03ndi2sdxdgxMpd2NAoLlCm6trEoIbx2HIIDOmo9zed1QbJwYf5Ha1EQy8dUWKgSC-hb5IW_1f7_7vVCoWTNAg0EXn_RWe0fKvYnvXJ2wzo9XU_XeuJIiSGLU62htIDq7OCyPuCitBGbuUe1KNOdyCu5HzWrFoQ5JfMsTWJA8cH3CLgHA5i4C5wCOLX1uW3ibsPv8O-TzvxMM8LJ76aV2gM-3t1n_INclhQ",
                  "e": "AQAB"
                }
              ]
            }
            """;
    }
}
