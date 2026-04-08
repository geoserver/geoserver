/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

/** Tests for {@link TokenIntrospector}. */
public class TokenIntrospectorTest {

    private WireMockServer wireMockServer;
    private String introspectionUrl;

    @Before
    public void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        introspectionUrl = "http://localhost:" + wireMockServer.port() + "/introspect";
    }

    @After
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    public void testConstructorWithThreeParams() {
        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "client-id", "client-secret");

        assertNotNull(introspector);
    }

    @Test
    public void testConstructorWithFourParams() {
        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "client-id", "client-secret", true);

        assertNotNull(introspector);
    }

    @Test
    public void testIntrospectTokenWithNullUrl() {
        TokenIntrospector introspector = new TokenIntrospector(null, "client-id", "client-secret");

        try {
            introspector.introspectToken("some-token");
            fail("Expected RuntimeException for null URL");
        } catch (RuntimeException e) {
            assertEquals("Cannot introspect token: the introspection endpoint URL is not set", e.getMessage());
        }
    }

    @Test
    public void testIntrospectTokenWithClientSecretBasic() {
        // Setup WireMock to respond to introspection request
        wireMockServer.stubFor(post(urlEqualTo("/introspect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"active\": true, \"sub\": \"user123\", \"scope\": \"openid profile\"}")));

        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "my-client", "my-secret", false);

        Map<String, Object> result = introspector.introspectToken("test-access-token");

        assertNotNull(result);
        assertEquals(true, result.get("active"));
        assertEquals("user123", result.get("sub"));
        assertEquals("openid profile", result.get("scope"));

        // Verify the request had Authorization header (Basic auth)
        wireMockServer.verify(
                postRequestedFor(urlEqualTo("/introspect")).withHeader("Authorization", containing("Basic ")));
    }

    @Test
    public void testIntrospectTokenWithClientSecretPost() {
        // Setup WireMock to respond to introspection request
        wireMockServer.stubFor(post(urlEqualTo("/introspect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"active\": true, \"client_id\": \"my-client\"}")));

        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "my-client", "my-secret", true);

        Map<String, Object> result = introspector.introspectToken("test-access-token");

        assertNotNull(result);
        assertEquals(true, result.get("active"));

        // Verify the request had client_id and client_secret in the body (no Basic auth header)
        wireMockServer.verify(postRequestedFor(urlEqualTo("/introspect"))
                .withRequestBody(containing("client_id=my-client"))
                .withRequestBody(containing("client_secret=my-secret")));
    }

    @Test
    public void testIntrospectTokenWithNullToken() {
        wireMockServer.stubFor(post(urlEqualTo("/introspect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"active\": false}")));

        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "client", "secret");

        Map<String, Object> result = introspector.introspectToken(null);

        assertNotNull(result);
        assertEquals(false, result.get("active"));

        // Verify empty token was sent
        wireMockServer.verify(postRequestedFor(urlEqualTo("/introspect")).withRequestBody(containing("token=")));
    }

    @Test
    public void testIntrospectTokenWithNullClientCredentials() {
        wireMockServer.stubFor(post(urlEqualTo("/introspect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"active\": true}")));

        // Test with null client credentials - should still work (converts to empty strings)
        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, null, null, true);

        Map<String, Object> result = introspector.introspectToken("token");

        assertNotNull(result);

        // Verify empty client_id and client_secret were sent
        wireMockServer.verify(postRequestedFor(urlEqualTo("/introspect"))
                .withRequestBody(containing("client_id="))
                .withRequestBody(containing("client_secret=")));
    }

    @Test
    public void testIntrospectTokenContentType() {
        wireMockServer.stubFor(post(urlEqualTo("/introspect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"active\": true}")));

        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "client", "secret");

        introspector.introspectToken("token");

        // Verify Content-Type is application/x-www-form-urlencoded
        wireMockServer.verify(postRequestedFor(urlEqualTo("/introspect"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded")));
    }

    @Test
    public void testIntrospectTokenIncludesTokenTypeHint() {
        wireMockServer.stubFor(post(urlEqualTo("/introspect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"active\": true}")));

        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "client", "secret");

        introspector.introspectToken("my-token");

        // Verify token_type_hint is included
        wireMockServer.verify(postRequestedFor(urlEqualTo("/introspect"))
                .withRequestBody(containing("token_type_hint=access_token")));
    }

    @Test
    public void testIntrospectTokenInactiveToken() {
        wireMockServer.stubFor(post(urlEqualTo("/introspect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"active\": false}")));

        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "client", "secret");

        Map<String, Object> result = introspector.introspectToken("expired-token");

        assertNotNull(result);
        assertEquals(false, result.get("active"));
    }

    @Test
    public void testIntrospectTokenWithFullResponse() {
        // Test with a full RFC 7662 compliant response
        String fullResponse =
                """
            {
                "active": true,
                "sub": "Z5O3upPC88QrAjx00dis",
                "client_id": "l238j323ds-23ij4",
                "username": "jdoe",
                "token_type": "bearer",
                "exp": 1419356238,
                "iat": 1419350238,
                "nbf": 1419350238,
                "aud": "https://protected.example.net/resource",
                "iss": "https://server.example.com/",
                "scope": "read write dolphin"
            }
            """;

        wireMockServer.stubFor(post(urlEqualTo("/introspect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(fullResponse)));

        TokenIntrospector introspector = new TokenIntrospector(introspectionUrl, "client", "secret");

        Map<String, Object> result = introspector.introspectToken("valid-token");

        assertNotNull(result);
        assertEquals(true, result.get("active"));
        assertEquals("Z5O3upPC88QrAjx00dis", result.get("sub"));
        assertEquals("jdoe", result.get("username"));
        assertEquals("bearer", result.get("token_type"));
        assertEquals("read write dolphin", result.get("scope"));
        assertEquals("https://server.example.com/", result.get("iss"));
    }
}
