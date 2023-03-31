/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("PMD.CloseResource") // full of mock closeables
public class KeycloakRESTClientTest {

    private static final String accessToken = "generatedAccessToken";
    private KeycloakRESTClient client;

    @Before
    public void setUp() throws Exception {
        client =
                new KeycloakRESTClient(
                        "http://localhost:8080",
                        "realm",
                        "clientID",
                        "clientSecret",
                        Collections.emptyList());
    }

    @Test
    public void testGetAccessTokenWithNullStatusLine() throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(null);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
        String retrievedToken = client.getAccessToken(httpClient);
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
        String retrievedToken = client.getAccessToken(httpClient);
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
        String retrievedToken = client.getAccessToken(httpClient);
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
        String retrievedToken = client.getAccessToken(httpClient);
        assertEquals(retrievedToken, accessToken);
    }
}
