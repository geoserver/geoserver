/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link org.geoserver.security.keycloak.KeycloakRoleService}. Does not test the load
 * method.
 */
public class KeycloakRoleServiceTest extends TestCase {

    private static final String accessToken = "generatedAccessToken";
    private static final Gson gson = new Gson();
    private KeycloakRoleService service;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        service = new KeycloakRoleService();
    }

    @Test
    public void testGetAccessTokenWithNullStatusLine() throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(null);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        String retrievedToken = service.getAccessToken(httpClient, gson);
        assertNull(retrievedToken);
    }

    @Test
    public void testGetAccessTokenWithNon200StatusLine() throws IOException {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(401);

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        String retrievedToken = service.getAccessToken(httpClient, gson);
        assertNull(retrievedToken);
    }

    @Test
    public void testGetAccessTokenWithNoResponseEntity() throws IOException {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(null);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        String retrievedToken = service.getAccessToken(httpClient, gson);
        assertNull(retrievedToken);
    }

    @Test
    public void testGetAccessToken() throws IOException {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);

        HttpEntity entity = mock(HttpEntity.class);
        // Return only some of the data that would be returned by the server, for test
        // clarity/simplicity
        when(entity.getContent())
                .thenReturn(
                        new ByteArrayInputStream(
                                ("{access_token: " + accessToken + "}").getBytes()));

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        String retrievedToken = service.getAccessToken(httpClient, gson);
        assertEquals(retrievedToken, accessToken);
    }

    @Test
    public void testGetRolesWithNullStatusLine() throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(null);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        List<GeoServerRole> roles = service.getRoles(httpClient, gson, accessToken);
        assertNull(roles);
    }

    @Test
    public void testGetRolesWithNon200StatusLine() throws IOException {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(401);

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        List<GeoServerRole> roles = service.getRoles(httpClient, gson, accessToken);
        assertNull(roles);
    }

    @Test
    public void testGetRolesWithNoResponseEntity() throws IOException {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(null);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        List<GeoServerRole> roles = service.getRoles(httpClient, gson, accessToken);
        assertNull(roles);
    }

    @Test
    public void testGetRoles() throws IOException {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);

        HttpEntity entity = mock(HttpEntity.class);
        // Return only some of the data that would be returned by the server, for test
        // clarity/simplicity
        when(entity.getContent())
                .thenReturn(
                        new ByteArrayInputStream(
                                ("[{\"id\": \"1\",\"name\": \"AUTHENTICATED\"},"
                                                + " {\"id\": \"2\",\"name\":\"ADMINISTRATOR\"}]")
                                        .getBytes()));
        List<GeoServerRole> expectedRoles = new ArrayList<>();
        expectedRoles.add(new GeoServerRole("AUTHENTICATED"));
        expectedRoles.add(new GeoServerRole("ADMINISTRATOR"));

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);
        List<GeoServerRole> roles = service.getRoles(httpClient, gson, accessToken);
        assertTrue(CollectionUtils.isEqualCollection(expectedRoles, roles));
    }
}
