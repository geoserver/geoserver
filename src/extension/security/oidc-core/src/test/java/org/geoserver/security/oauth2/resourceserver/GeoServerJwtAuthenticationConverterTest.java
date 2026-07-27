/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Tests for {@link GeoServerJwtAuthenticationConverter}. */
public class GeoServerJwtAuthenticationConverterTest {

    private GeoServerRoleResolvers.ResolverContext mockContext;
    private GeoServerSecurityManager mockSecurityManager;
    private GeoServerRoleService mockRoleService;

    @Before
    public void setUp() {
        mockSecurityManager = mock(GeoServerSecurityManager.class);
        mockRoleService = mock(GeoServerRoleService.class);

        // Mock the active role service to avoid "role service Service is null" error
        when(mockSecurityManager.getActiveRoleService()).thenReturn(mockRoleService);

        mockContext = new GeoServerRoleResolvers.DefaultResolverContext(
                mockSecurityManager,
                null, // roleServiceName
                null, // userGroupServiceName
                null, // rolesHeaderAttribute
                null, // roleConverter
                PreAuthenticatedUserNameRoleSource.RoleService);
    }

    @Test
    public void testConstructor() {
        GeoServerJwtAuthenticationConverter converter = new GeoServerJwtAuthenticationConverter(mockContext);
        assertNotNull(converter);
    }

    @Test
    public void testConvertWithValidJwt() {
        GeoServerJwtAuthenticationConverter converter = new GeoServerJwtAuthenticationConverter(mockContext);

        // Create a valid JWT
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertTrue(result instanceof JwtAuthenticationToken);
        assertEquals("testuser", result.getName());
    }

    @Test
    public void testSetPrincipalClaimName() {
        GeoServerJwtAuthenticationConverter converter = new GeoServerJwtAuthenticationConverter(mockContext);

        // Set a custom principal claim name
        converter.setPrincipalClaimName("email");

        // Create a JWT with email claim
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("email", "user@example.com");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertEquals("user@example.com", result.getName());
    }

    @Test
    public void testConvertReturnsJwtAuthenticationTokenWithRoles() {
        GeoServerJwtAuthenticationConverter converter = new GeoServerJwtAuthenticationConverter(mockContext);

        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testuser");
        claims.put("iss", "https://issuer.example.com");

        Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertNotNull(result);
        assertNotNull(result.getAuthorities());
    }
}
