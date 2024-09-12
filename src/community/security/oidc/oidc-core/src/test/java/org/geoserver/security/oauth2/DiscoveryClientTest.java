/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryClientTest {

    @Mock RestTemplate restTemplate;
    JSONObject discovery;

    @Before
    public void setupDiscovery() throws IOException {
        String json = IOUtils.toString(getClass().getResourceAsStream("discovery.json"), "UTF-8");
        this.discovery = (JSONObject) JSONSerializer.toJSON(json);
    }

    @Test
    public void testServerURL() throws Exception {
        testDiscoveryClient("https://server.example.com");
    }

    @Test
    public void testFullURL() throws Exception {
        testDiscoveryClient("https://server.example.com/.well-known/openid-configuration");
    }

    private void testDiscoveryClient(String location) {
        DiscoveryClient client = new DiscoveryClient(location, restTemplate);
        Mockito.when(
                        restTemplate.getForObject(
                                "https://server.example.com/.well-known/openid-configuration",
                                Map.class))
                .thenReturn(discovery);
        OpenIdConnectFilterConfig config = new OpenIdConnectFilterConfig();
        client.autofill(config);

        assertEquals(
                "https://server.example.com/connect/userinfo", config.getCheckTokenEndpointUrl());
        assertEquals("https://server.example.com/jwks.json", config.getJwkURI());
        assertEquals(
                "https://server.example.com/connect/authorize", config.getUserAuthorizationUri());
        assertEquals("https://server.example.com/connect/token", config.getAccessTokenUri());
        assertEquals("openid profile email address phone offline_access", config.getScopes());
    }
}
