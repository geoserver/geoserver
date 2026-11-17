/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.geoserver.wms.CacheConfiguration;
import org.geoserver.wms.WMS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

@RunWith(Parameterized.class)
public class GetMapKvpRequestReaderProxyTest {

    @Parameterized.Parameters(name = "cache={0}, scheme={1}, bypass={2}")
    public static Object[][] parameters() {
        return new Object[][] {
            {false, "http", false},
            {false, "https", false},
            {true, "http", false},
            {true, "https", false},
            {false, "http", true},
            {false, "https", true},
            {true, "http", true},
            {true, "https", true}
        };
    }

    private final boolean cacheEnabled;
    private final String scheme;
    private final boolean bypass;
    private final Map<String, String> originalProperties = new HashMap<>();

    public GetMapKvpRequestReaderProxyTest(boolean cacheEnabled, String scheme, boolean bypass) {
        this.cacheEnabled = cacheEnabled;
        this.scheme = scheme;
        this.bypass = bypass;
    }

    @Before
    public void configureProxy() {
        setProperty("http.proxyHost", "proxy.example");
        setProperty("http.proxyPort", "3128");
        setProperty("https.proxyHost", "proxy.example");
        setProperty("https.proxyPort", "3129");
        setProperty("http.nonProxyHosts", bypass ? "styles.example" : "");
    }

    private void setProperty(String name, String value) {
        originalProperties.put(name, System.getProperty(name));
        System.setProperty(name, value);
    }

    @After
    public void restoreProperties() {
        originalProperties.forEach((name, value) -> {
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        });
    }

    @Test
    public void testJvmProxyRoute() throws Exception {
        HttpClientConnectionManager manager = mock(HttpClientConnectionManager.class);
        RuntimeException stopBeforeConnecting = new RuntimeException("Stop after selecting the route");
        when(manager.lease(any(), any(), any(), any())).thenThrow(stopBeforeConnecting);

        GetMapKvpRequestReader reader = new GetMapKvpRequestReader(mock(WMS.class), manager);
        HttpClientBuilder builder = HttpClientBuilder.create().setConnectionManager(manager);
        // The non-caching factory has no connection-manager argument. Supply a real builder
        // with our mock manager so both branches stop before any DNS lookup or socket access.
        try (MockedStatic<HttpClientBuilder> factory = mockStatic(HttpClientBuilder.class)) {
            factory.when(HttpClientBuilder::create).thenReturn(builder);
            try (CloseableHttpClient client =
                    reader.createHttpClient(RequestConfig.DEFAULT, new CacheConfiguration(cacheEnabled))) {
                HttpHost target = new HttpHost(scheme, "styles.example", scheme.equals("https") ? 443 : 80);
                HttpGet request = new HttpGet(scheme + "://styles.example/style.sld");
                assertSame(
                        stopBeforeConnecting,
                        assertThrows(RuntimeException.class, () -> client.executeOpen(target, request, null)));

                ArgumentCaptor<HttpRoute> route = ArgumentCaptor.forClass(HttpRoute.class);
                verify(manager).lease(any(), route.capture(), any(), any());
                HttpHost expectedProxy =
                        bypass ? null : new HttpHost("http", "proxy.example", scheme.equals("https") ? 3129 : 3128);
                assertEquals(target, route.getValue().getTargetHost());
                assertEquals(expectedProxy, route.getValue().getProxyHost());
            }
        }
    }
}
