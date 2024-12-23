/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import static org.junit.Assert.assertEquals;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.HTTPHeadersCollector;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class GeoServerCasAuthenticationFilterTest extends GeoServerSystemTestSupport {

    @Test
    public void testRetrieveServiceWithNoProxyBaseUrl() {
        GeoServer geoServer = getGeoServer();
        String oldValue = System.getProperty("PROXY_BASE_URL");
        try {
            setProxyBase(geoServer, null);
            setSystemProperty("PROXY_BASE_URL", null);

            MockHttpServletRequest request =
                    buildMockRequest("http", "localhost", 8080, "/geoserver", "/geoserver/myworkspace/wms?SERVICE=WMS");

            String serviceUrl = GeoServerCasAuthenticationFilter.retrieveService(request);

            assertEquals("http://localhost:8080/geoserver/myworkspace/wms?SERVICE=WMS", serviceUrl);
        } finally {
            // Reset to make sure we don't interfere with other tests at all
            setSystemProperty("PROXY_BASE_URL", oldValue);
        }
    }

    @Test
    public void testRetrieveServiceWithProxyBaseUrlSystemProperty() {

        String oldValue = System.getProperty("PROXY_BASE_URL");
        try {
            setSystemProperty("PROXY_BASE_URL", "https://example.com/geoserver");

            MockHttpServletRequest request =
                    buildMockRequest("http", "localhost", 8080, "/geoserver", "/geoserver/myworkspace/wms?SERVICE=WMS");

            String serviceUrl = GeoServerCasAuthenticationFilter.retrieveService(request);

            assertEquals("https://example.com/geoserver/myworkspace/wms?SERVICE=WMS", serviceUrl);
        } finally {
            // Reset to make sure we don't interfere with other tests at all
            setSystemProperty("PROXY_BASE_URL", oldValue);
        }
    }

    @Test
    public void testRetrieveServiceWithProxyBaseUrlConfig() {
        GeoServer geoServer = getGeoServer();
        try {
            setProxyBase(geoServer, "https://example.com/geoserver");

            MockHttpServletRequest request =
                    buildMockRequest("http", "localhost", 8080, "/geoserver", "/geoserver/myworkspace/wms?SERVICE=WMS");

            String serviceUrl = GeoServerCasAuthenticationFilter.retrieveService(request);

            assertEquals("https://example.com/geoserver/myworkspace/wms?SERVICE=WMS", serviceUrl);
        } finally {
            setProxyBase(geoServer, null);
        }
    }

    @Test
    public void testRetrieveServiceWithProxyBaseUrlConfigAndParameterization() {
        GeoServer geoServer = getGeoServer();
        try {
            setProxyBase(geoServer, "https://${X-Forwarded-Host}/geoserver");

            MockHttpServletRequest request =
                    buildMockRequest("http", "localhost", 8080, "/geoserver", "/geoserver/myworkspace/wms?SERVICE=WMS");

            // Add headers
            request.addHeader(ProxifyingURLMangler.Headers.FORWARDED_HOST.asString(), "example.com");
            HTTPHeadersCollector filter = new HTTPHeadersCollector();
            filter.collectHeaders(request);

            String serviceUrl = GeoServerCasAuthenticationFilter.retrieveService(request);

            assertEquals("https://example.com/geoserver/myworkspace/wms?SERVICE=WMS", serviceUrl);
        } finally {
            setProxyBase(geoServer, null);
        }
    }

    private MockHttpServletRequest buildMockRequest(
            String scheme, String serverName, int port, String contextPath, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName(serverName);
        request.setServerPort(port);
        request.setContextPath(contextPath);
        request.setRequestURI(uri);
        return request;
    }

    private void setProxyBase(GeoServer gs, String s) {
        final GeoServerInfo global = gs.getGlobal();
        global.getSettings().setProxyBaseUrl(s);
        global.getSettings().setUseHeadersProxyURL(s != null);
        gs.save(global);
    }

    private void setSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
