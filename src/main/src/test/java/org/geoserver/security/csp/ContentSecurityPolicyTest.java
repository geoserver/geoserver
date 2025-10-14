/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.filters.SecurityHeadersFilter;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.util.Requests;

public class ContentSecurityPolicyTest {

    private static final CSPConfiguration DEFAULT_CONFIG = CSPDefaultConfiguration.newInstance();

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
        GeoServerExtensionsHelper.singleton("cspHeaderDAO", dao);
        GeoServerExtensionsHelper.singleton("proxyfier", new ProxifyingURLMangler(geoServer), URLMangler.class);
    }

    @AfterClass
    public static void clearExtensions() {
        GeoServerExtensionsHelper.clear();
    }

    @Before
    public void resetDAO() throws Exception {
        dao.setConfig(DEFAULT_CONFIG);
        dao.reset();
    }

    @Before
    @After
    public void resetProperties() {
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY);
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_POLICY);
        System.clearProperty(CSPUtils.GEOSERVER_CSP_FALLBACK);
        System.clearProperty(CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES);
        System.clearProperty(CSPUtils.GEOSERVER_CSP_FORM_ACTION);
        System.clearProperty(CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS);
        System.clearProperty(Requests.PROXY_PARAM);
        System.clearProperty("GEOSERVER_DISABLE_STATIC_WEB_FILES");
        System.clearProperty("GEOSERVER_STATIC_WEB_FILES_SCRIPT");
    }

    @Test
    public void testDisabledConfig() throws Exception {
        // no header if config disabled
        CSPConfiguration config = dao.getConfig();
        config.setEnabled(false);
        dao.setConfig(config);
        assertHeader(null, "GET", null, null, null);
    }

    @Test
    public void testDisabledPolicies() throws Exception {
        // no header if all policies disabled
        CSPConfiguration config = dao.getConfig();
        config.getPolicies().forEach(p -> p.setEnabled(false));
        dao.setConfig(config);
        assertHeader(null, "GET", null, null, null);
    }

    @Test
    public void testDisabledRules() throws Exception {
        // no header if all rules disabled
        CSPConfiguration config = dao.getConfig();
        config.getPolicies().forEach(p -> p.getRules().forEach(r -> r.setEnabled(false)));
        dao.setConfig(config);
        assertHeader(null, "GET", null, null, null);
    }

    @Test
    public void testDefaultFallback() throws Exception {
        // erase the contents of csp.xml to cause an xstream exception
        Path file = dd.getSecurity(CSPHeaderDAO.CONFIG_FILE_NAME).file().toPath();
        try {
            Files.write(file, new byte[0]);
            assertHeader(CSPUtils.DEFAULT_FALLBACK, "GET", null, null, null);
        } finally {
            Files.delete(file);
        }
    }

    @Test
    public void testCustomFallback() throws Exception {
        String expected = "frame-ancestors 'none';";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FALLBACK, expected);
        // erase the contents of csp.xml to cause an xstream exception
        Path file = dd.getSecurity(CSPHeaderDAO.CONFIG_FILE_NAME).file().toPath();
        try {
            Files.write(file, new byte[0]);
            assertHeader(expected, "GET", null, null, null);
        } finally {
            Files.delete(file);
        }
    }

    @Test
    public void testGET() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testHEAD() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "HEAD", null, null, null);
    }

    @Test
    public void testPOST() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "POST", null, null, null);
    }

    @Test
    public void testPUT() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "PUT", null, null, null);
    }

    @Test
    public void testDELETE() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "DELETE", null, null, null);
    }

    @Test
    public void testInjectProxyBaseURLNotSet() throws Exception {
        // no proxy base URL is set
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setInjectProxyBase(true);
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testInjectProxyBaseURLWithoutPort() throws Exception {
        // no proxy base URL is set
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self' http://foo; "
                + "connect-src 'self' http://foo; font-src 'self' http://foo; img-src 'self' http://foo data:; "
                + "style-src 'self' http://foo 'unsafe-inline'; script-src 'self' http://foo; "
                + "form-action 'self' http://foo; frame-ancestors 'self';";
        System.setProperty(Requests.PROXY_PARAM, "http://foo");
        CSPConfiguration config = dao.getConfig();
        config.setInjectProxyBase(true);
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testInjectProxyBaseURLWithPort() throws Exception {
        // no proxy base URL is set
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self' http://foo:8080; "
                + "connect-src 'self' http://foo:8080; font-src 'self' http://foo:8080; "
                + "img-src 'self' http://foo:8080 data:; style-src 'self' http://foo:8080 'unsafe-inline'; "
                + "script-src 'self' http://foo:8080; form-action 'self' http://foo:8080; frame-ancestors 'self';";
        System.setProperty(Requests.PROXY_PARAM, "http://foo:8080");
        CSPConfiguration config = dao.getConfig();
        config.setInjectProxyBase(true);
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testInjectProxyBaseURLRequestToProxy() throws Exception {
        // no proxy base URL is set
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        System.setProperty(Requests.PROXY_PARAM, "http://localhost");
        CSPConfiguration config = dao.getConfig();
        config.setInjectProxyBase(true);
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testStaticWebFileDisabled() throws Exception {
        System.setProperty("GEOSERVER_DISABLE_STATIC_WEB_FILES", "true");
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "GET", "/www/index.html", null, null);
    }

    @Test
    public void testStaticWebFileScriptSelf() throws Exception {
        System.setProperty("GEOSERVER_STATIC_WEB_FILES_SCRIPT", "SELF");
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "GET", "/www/index.html", null, null);
    }

    @Test
    public void testStaticWebFileNoRemoteResources() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                + "script-src 'self' 'unsafe-inline' 'unsafe-eval'; form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "GET", "/www/index.html", null, null);
    }

    @Test
    public void testStaticWebFileInvalidRemoteResources() throws Exception {
        System.setProperty(CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES, "~!@#$");
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                + "script-src 'self' 'unsafe-inline' 'unsafe-eval'; form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "GET", "/www/index.html", null, null);
    }

    @Test
    public void testStaticWebFileRemoteResourcesProperty() throws Exception {
        System.setProperty(CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES, "http://foo");
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self' http://foo; img-src 'self' http://foo data:; "
                + "style-src 'self' http://foo 'unsafe-inline'; "
                + "script-src 'self' http://foo 'unsafe-inline' 'unsafe-eval'; form-action 'self'; "
                + "frame-ancestors 'self';";
        assertHeader(expected, "GET", "/www/index.html", null, null);
    }

    @Test
    public void testStaticWebFileRemoteResourcesField() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self' http://bar; img-src 'self' http://bar data:; "
                + "style-src 'self' http://bar 'unsafe-inline'; "
                + "script-src 'self' http://bar 'unsafe-inline' 'unsafe-eval'; form-action 'self'; "
                + "frame-ancestors 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setRemoteResources("http://bar");
        dao.setConfig(config);
        assertHeader(expected, "GET", "/www/index.html", null, null);
    }

    @Test
    public void testStaticWebFileRemoteResourcesPropertyAndField() throws Exception {
        System.setProperty(CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES, "http://foo");
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self' http://foo; img-src 'self' http://foo data:; "
                + "style-src 'self' http://foo 'unsafe-inline'; "
                + "script-src 'self' http://foo 'unsafe-inline' 'unsafe-eval'; form-action 'self'; "
                + "frame-ancestors 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setRemoteResources("http://bar");
        dao.setConfig(config);
        assertHeader(expected, "GET", "/www/index.html", null, null);
    }

    @Test
    public void testIndexPage() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                + "script-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'self';";
        assertHeader(expected, "GET", "/index.html", null, null);
    }

    @Test
    public void testFormActionPropertyValid() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self' https:; frame-ancestors 'self';";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FORM_ACTION, "'self' https:");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFormActionPropertyMissingSelf() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self' https:; frame-ancestors 'self';";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FORM_ACTION, "https:");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFormActionPropertyInvalid() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "frame-ancestors 'self';";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FORM_ACTION, "~!@#$%^&*()_+");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFormActionPropertyHIDE() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "frame-ancestors 'self';";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FORM_ACTION, "HIDE");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFormActionFieldValid() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self' http:; frame-ancestors 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setFormAction("'self' http:");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFormActionFieldMissingSelf() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self' http:; frame-ancestors 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setFormAction("http:");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFormActionFieldInvalid() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "frame-ancestors 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setFormAction("~!@#$%^&*()_+");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFormActionFieldHIDE() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "frame-ancestors 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setFormAction("HIDE");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFormActionPropertyAndField() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self' https:; frame-ancestors 'self';";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FORM_ACTION, "https:");
        CSPConfiguration config = dao.getConfig();
        config.setFormAction("http:");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsSAMEORIGIN() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self';";
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_POLICY, "SAMEORIGIN");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsDENY() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'none';";
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_POLICY, "DENY");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsALLOWFROM() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self';";
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_POLICY, "ALLOW-FROM http://foo");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsXFrameDisabled() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self';";
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY, "false");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsPropertyValid() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self' https:;";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS, "'self' https:");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsPropertyMissingSelf() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self' https:;";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS, "https:");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsPropertyInvalid() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self';";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS, "~!@#$%^&*()_+");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsPropertyHIDE() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self';";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS, "HIDE");
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsFieldValid() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self' http:;";
        CSPConfiguration config = dao.getConfig();
        config.setFrameAncestors("'self' http:");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsFieldMissingSelf() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self' http:;";
        CSPConfiguration config = dao.getConfig();
        config.setFrameAncestors("http:");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsFieldInvalid() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setFrameAncestors("~!@#$%^&*()_+");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsFieldHIDE() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self';";
        CSPConfiguration config = dao.getConfig();
        config.setFrameAncestors("HIDE");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    @Test
    public void testFrameAncestorsPropertyAndField() throws Exception {
        String expected = "base-uri 'self'; default-src 'none'; child-src 'self'; connect-src 'self'; "
                + "font-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; "
                + "form-action 'self'; frame-ancestors 'self' https:;";
        System.setProperty(CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS, "https:");
        CSPConfiguration config = dao.getConfig();
        config.setFrameAncestors("http:");
        dao.setConfig(config);
        assertHeader(expected, "GET", null, null, null);
    }

    private static void assertHeader(
            String expected, String method, String pathInfo, String queryString, Map<String, ?> parameters)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "");
        request.setPathInfo(pathInfo);
        request.setQueryString(queryString);
        request.setParameters(parameters != null ? parameters : Collections.emptyMap());
        request.setProtocol("http");
        request.addHeader(HttpHeaders.HOST, "localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SecurityHeadersFilter().doFilter(request, response, new MockFilterChain());
        String actual = response.getHeader(HttpHeaders.CONTENT_SECURITY_POLICY);
        if (expected == null) {
            assertNull(actual);
        } else {
            assertEquals(expected, actual);
        }
    }
}
