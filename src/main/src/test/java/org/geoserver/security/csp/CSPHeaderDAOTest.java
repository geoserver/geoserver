/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.util.Requests;

public class CSPHeaderDAOTest {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private static XStreamPersisterFactory xpf = null;

    private static GeoServerDataDirectory dd = null;

    private static CSPHeaderDAO dao = null;

    private static SettingsInfo settings = null;

    @BeforeClass
    public static void initDAO() throws IOException {
        dd = new GeoServerDataDirectory(folder.getRoot());
        xpf = new XStreamPersisterFactory();
        GeoServer geoServer = mock(GeoServer.class);
        settings = new SettingsInfoImpl();
        settings.setUseHeadersProxyURL(false);
        when(geoServer.getSettings()).thenReturn(settings);
        dao = new CSPHeaderDAO(geoServer, dd, xpf);
        GeoServerExtensionsHelper.singleton("proxyfier", new ProxifyingURLMangler(geoServer), URLMangler.class);
    }

    @AfterClass
    public static void clearExtensions() {
        GeoServerExtensionsHelper.clear();
    }

    @Before
    public void resetDAO() throws Exception {
        dao.reset();
        CSPConfiguration config = dao.getConfig();
        config.setReportOnly(false);
        dao.setConfig(config);
    }

    @After
    public void remoteThreadLocal() {
        CSPHeaderDAO.removeProxyPolicy();
    }

    @Before
    @After
    public void resetProperties() {
        System.clearProperty(CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES);
        System.clearProperty(Requests.PROXY_PARAM);
    }

    @Test
    public void testInitializeFromExistingFiles() throws Exception {
        long configLastModified = configFile().lastmodified();
        long defaultLastModified = defaultFile().lastmodified();
        Thread.sleep(5);
        new CSPHeaderDAO(null, dd, xpf);
        assertEquals(
                "csp.xml was unexpectedly updated",
                configLastModified,
                configFile().lastmodified());
        assertEquals(
                "csp_default.xml was unexpectedly updated",
                defaultLastModified,
                defaultFile().lastmodified());
    }

    @Test
    public void testInitializeDoNotUpdatedFile() throws Exception {
        Files.write(configFile().file().toPath(), "<config></config>".getBytes());
        long configLastModified = configFile().lastmodified();
        long defaultLastModified = defaultFile().lastmodified();
        Thread.sleep(5);
        new CSPHeaderDAO(null, dd, xpf);
        assertEquals(
                "csp.xml was unexpectedly updated",
                configLastModified,
                configFile().lastmodified());
        assertEquals(
                "csp_default.xml was unexpectedly updated",
                defaultLastModified,
                defaultFile().lastmodified());
    }

    @Test
    public void testInitializeWithMissingDefaultFile() throws Exception {
        long configLastModified = configFile().lastmodified();
        Files.delete(defaultFile().file().toPath());
        Thread.sleep(5);
        new CSPHeaderDAO(null, dd, xpf);
        assertEquals(
                "csp.xml was unexpectedly updated",
                configLastModified,
                configFile().lastmodified());
        assertEquals(
                "csp_default.xml was not re-created",
                Resource.Type.RESOURCE,
                defaultFile().getType());
    }

    @Test
    public void testInitializeUpdateOutdatedFiles() throws Exception {
        Files.write(configFile().file().toPath(), "<config></config>".getBytes());
        Files.write(defaultFile().file().toPath(), "<config></config>".getBytes());
        long configLastModified = configFile().lastmodified();
        long defaultLastModified = defaultFile().lastmodified();
        Thread.sleep(5);
        new CSPHeaderDAO(null, dd, xpf);
        assertNotEquals(
                "csp.xml was not updated", configLastModified, configFile().lastmodified());
        assertNotEquals(
                "csp_default.xml was not updated",
                defaultLastModified,
                defaultFile().lastmodified());
    }

    @Test
    public void testInitRequestNoProxyPolicy() throws Exception {
        assertNull(dao.init(null));
    }

    @Test
    public void testInitRequestProxyPropertySet() throws Exception {
        System.setProperty(Requests.PROXY_PARAM, "http://foo");
        CSPHeaderDAO.setProxyPolicy("default-src: ${proxy.base.url}");
        assertNull(dao.init(null));
    }

    @Test
    public void testInitRequestNoLocalSettings() throws Exception {
        CSPHeaderDAO.setProxyPolicy("default-src: ${proxy.base.url}");
        assertNull(dao.init(null));
    }

    @Test
    public void testInitRequestNoLocalProxyBaseUrl() throws Exception {
        CSPHeaderDAO.setProxyPolicy("default-src: ${proxy.base.url}");
        try {
            settings.setWorkspace(new WorkspaceInfoImpl());
            assertNull(dao.init(null));
        } finally {
            settings.setWorkspace(null);
        }
    }

    @Test
    public void testInitRequestConfigException() throws Exception {
        CSPHeaderDAO.setProxyPolicy("default-src: ${proxy.base.url}");
        Request request = new Request();
        // erase the contents of csp.xml to cause an xstream exception
        Path file1 = dd.getSecurity(CSPHeaderDAO.CONFIG_FILE_NAME).file().toPath();
        try {
            Files.write(file1, new byte[0]);
            settings.setWorkspace(new WorkspaceInfoImpl());
            settings.setProxyBaseUrl("http://foo");
            assertSame(request, dao.init(request));
        } finally {
            settings.setWorkspace(null);
            settings.setProxyBaseUrl(null);
            Path file2 =
                    dd.getSecurity(CSPHeaderDAO.DEFAULT_CONFIG_FILE_NAME).file().toPath();
            Files.copy(file2, file1, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    public void testInitRequestWithSameLocalProxyBaseUrl() throws Exception {
        String expected = "default-src: http://foo;";
        CSPHeaderDAO.setProxyPolicy("default-src: ${proxy.base.url}");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader(HttpHeaders.HOST, "localhost");
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        when(httpResponse.getHeader(CONTENT_SECURITY_POLICY)).thenReturn(expected);
        Request request = new Request();
        request.setHttpRequest(httpRequest);
        request.setHttpResponse(httpResponse);
        try {
            settings.setWorkspace(new WorkspaceInfoImpl());
            settings.setProxyBaseUrl("http://foo");
            assertSame(request, dao.init(request));
            verify(httpResponse, never()).setHeader(any(), any());
        } finally {
            settings.setWorkspace(null);
            settings.setProxyBaseUrl(null);
        }
    }

    @Test
    public void testInitRequestWithDifferentLocalProxyBaseUrl() throws Exception {
        CSPHeaderDAO.setProxyPolicy("default-src: ${proxy.base.url}");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader(HttpHeaders.HOST, "localhost");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        httpResponse.setHeader(CONTENT_SECURITY_POLICY, "default-src: http://bar;");
        Request request = new Request();
        request.setHttpRequest(httpRequest);
        request.setHttpResponse(httpResponse);
        try {
            settings.setWorkspace(new WorkspaceInfoImpl());
            settings.setProxyBaseUrl("http://foo");
            assertSame(request, dao.init(request));
            assertEquals("default-src: http://foo;", httpResponse.getHeader(CONTENT_SECURITY_POLICY));
        } finally {
            settings.setWorkspace(null);
            settings.setProxyBaseUrl(null);
        }
    }

    @Test
    public void testGetPropertyValueInvalidKey() throws Exception {
        assertEquals("", CSPHeaderDAO.getPropertyValue(null, null, "java.version"));
    }

    @Test
    public void testGetPropertyValueInvalidPropertyValue() throws Exception {
        CSPConfiguration config = new CSPConfiguration();
        System.setProperty(CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES, "~!@#$");
        assertEquals("", CSPHeaderDAO.getPropertyValue(null, config, CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES));
    }

    @Test
    public void testGetPropertyValueInvalidFieldValue() throws Exception {
        CSPConfiguration config = new CSPConfiguration();
        config.setRemoteResources("~!@#$");
        assertEquals("", CSPHeaderDAO.getPropertyValue(null, config, CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES));
    }

    @Test
    public void testGetPropertyValueMissingValue() throws Exception {
        CSPConfiguration config = new CSPConfiguration();
        assertEquals("", CSPHeaderDAO.getPropertyValue(null, config, CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES));
    }

    @Test
    public void testGetPropertyValueValidPropertyValue() throws Exception {
        String expected = "http://geoserver.org";
        CSPConfiguration config = new CSPConfiguration();
        System.setProperty(CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES, expected);
        assertEquals(expected, CSPHeaderDAO.getPropertyValue(null, config, CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES));
    }

    @Test
    public void testGetPropertyValueValidFieldValue() throws Exception {
        String expected = "http://geoserver.org";
        CSPConfiguration config = new CSPConfiguration();
        config.setRemoteResources(expected);
        assertEquals(expected, CSPHeaderDAO.getPropertyValue(null, config, CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES));
    }

    @Test
    public void testMatchesProxyBaseWrongProtocol() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        assertFalse(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo")));
    }

    @Test
    public void testMatchesProxyBaseWrongHost() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.addHeader(HttpHeaders.HOST, "bar");
        assertFalse(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo")));
    }

    @Test
    public void testMatchesProxyBaseWrongPort() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.addHeader(HttpHeaders.HOST, "foo:8080");
        assertFalse(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo")));
    }

    @Test
    public void testMatchesProxyBaseNoForwardedHeaderDefaultPort() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.addHeader(HttpHeaders.HOST, "foo");
        assertTrue(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo")));
    }

    @Test
    public void testMatchesProxyBaseWithXForwardedHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.X_FORWARDED_PROTO, "https");
        request.addHeader(HttpHeaders.X_FORWARDED_HOST, "foo");
        request.addHeader(HttpHeaders.X_FORWARDED_PORT, "443");
        assertTrue(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo")));
    }

    @Test
    public void testMatchesProxyBaseWithXForwardedPort() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.X_FORWARDED_PROTO, "https");
        request.addHeader(HttpHeaders.X_FORWARDED_HOST, "foo:8443");
        request.addHeader(HttpHeaders.X_FORWARDED_PORT, "443");
        assertTrue(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo")));
    }

    @Test
    public void testMatchesProxyBaseWithForwardedHeaderDefaultPort() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.FORWARDED, "for=127.0.0.1;proto=https;host=foo");
        assertTrue(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo")));
    }

    @Test
    public void testMatchesProxyBaseWithForwardedHeaderWithPort() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.FORWARDED, "for=127.0.0.1;proto=https;host=foo:8443");
        assertTrue(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo:8443")));
    }

    @Test
    public void testMatchesProxyBaseWithMissingForwardedHeaderParts() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.FORWARDED, "for=127.0.0.1");
        assertFalse(CSPHeaderDAO.matchesProxyBase(request, new URL("https://foo")));
    }

    private static Resource configFile() {
        return dd.getSecurity(CSPHeaderDAO.CONFIG_FILE_NAME);
    }

    private static Resource defaultFile() {
        return dd.getSecurity(CSPHeaderDAO.DEFAULT_CONFIG_FILE_NAME);
    }
}
