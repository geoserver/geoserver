/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.csp.CSPConfiguration;
import org.geoserver.security.csp.CSPHeaderDAO;
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

/** Simple test to make sure the SecurityHeadersFilter works and is configurable. */
public class SecurityHeadersFilterTest {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    private static CSPHeaderDAO dao;

    @BeforeClass
    public static void initDAO() throws IOException {
        GeoServerDataDirectory dd = new GeoServerDataDirectory(folder.getRoot());
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        dao = new CSPHeaderDAO(null, dd, xpf);
        GeoServerExtensionsHelper.singleton("cspHeaderDAO", dao);
    }

    @AfterClass
    public static void clearExtensions() {
        GeoServerExtensionsHelper.clear();
    }

    @Before
    @After
    public void resetProperties() {
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY);
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_HSTS_POLICY);
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_HSTS_SHOULD_SET_POLICY);
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_POLICY);
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY);
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_XXSS_PROTECTION_POLICY);
        System.clearProperty(SecurityHeadersFilter.GEOSERVER_XXSS_PROTECTION_SHOULD_SET_POLICY);
    }

    @Test
    public void testFilterCSPDisabled() throws Exception {
        setCSPConfig(false, true);
        assertNull(getHeader("Content-Security-Policy"));
        assertNull(getHeader("Content-Security-Policy-Report-Only"));
    }

    @Test
    public void testFilterCSPBlocking() throws Exception {
        setCSPConfig(true, false);
        String expected = "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                + "style-src 'self' 'unsafe-inline'; script-src 'self';, "
                + "frame-ancestors 'self';";
        assertEquals(expected, getHeader("Content-Security-Policy"));
        assertNull(getHeader("Content-Security-Policy-Report-Only"));
    }

    @Test
    public void testFilterCSPReporting() throws Exception {
        setCSPConfig(true, true);
        String expected = "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                + "style-src 'self' 'unsafe-inline'; script-src 'self';, "
                + "frame-ancestors 'self';";
        assertNull(getHeader("Content-Security-Policy"));
        assertEquals(expected, getHeader("Content-Security-Policy-Report-Only"));
    }

    private static void setCSPConfig(boolean enabled, boolean reportOnly) throws Exception {
        CSPConfiguration config = dao.getConfig();
        config.setEnabled(enabled);
        config.setReportOnly(reportOnly);
        dao.setConfig(config);
    }

    @Test
    public void testFilterDefaultFrameOptions() throws Exception {
        assertEquals("SAMEORIGIN", getHeader("X-Frame-Options"));
    }

    @Test
    public void testFilterWithoutFrameOptions() throws Exception {
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY, "false");
        assertNull(getHeader("X-Frame-Options"));
    }

    @Test
    public void testFilterCustomFrameOptions() throws Exception {
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XFRAME_POLICY, "DENY");
        assertEquals("DENY", getHeader("X-Frame-Options"));
    }

    @Test
    public void testFilterDefaultContentTypeOptions() throws Exception {
        assertEquals("nosniff", getHeader("X-Content-Type-Options"));
    }

    @Test
    public void testFilterWithoutContentTypeOptions() throws Exception {
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY, "false");
        assertNull(getHeader("X-Content-Type-Options"));
    }

    @Test
    public void testFilterDefaultXssProtection() throws Exception {
        assertNull(getHeader("X-XSS-Protection"));
    }

    @Test
    public void testFilterWithXssProtection() throws Exception {
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XXSS_PROTECTION_SHOULD_SET_POLICY, "true");
        assertEquals("0", getHeader("X-XSS-Protection"));
    }

    @Test
    public void testFilterCustomXssProtection() throws Exception {
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XXSS_PROTECTION_SHOULD_SET_POLICY, "true");
        System.setProperty(SecurityHeadersFilter.GEOSERVER_XXSS_PROTECTION_POLICY, "1; mode=block");
        assertEquals("1; mode=block", getHeader("X-XSS-Protection"));
    }

    @Test
    public void testFilterHttpHstsDefault() throws Exception {
        assertNull(getHeader("Strict-Transport-Security"));
    }

    @Test
    public void testFilterHttpHstsEnabled() throws Exception {
        System.setProperty(SecurityHeadersFilter.GEOSERVER_HSTS_SHOULD_SET_POLICY, "true");
        assertNull(getHeader("Strict-Transport-Security"));
    }

    @Test
    public void testFilterHttpsHstsDefault() throws Exception {
        assertNull(getHeader(true, "Strict-Transport-Security"));
    }

    @Test
    public void testFilterHttpsHstsEnabled() throws Exception {
        System.setProperty(SecurityHeadersFilter.GEOSERVER_HSTS_SHOULD_SET_POLICY, "true");
        assertEquals("max-age=31536000 ; includeSubDomains", getHeader(true, "Strict-Transport-Security"));
    }

    @Test
    public void testFilterHttpsHstsCustom() throws Exception {
        System.setProperty(SecurityHeadersFilter.GEOSERVER_HSTS_SHOULD_SET_POLICY, "true");
        System.setProperty(SecurityHeadersFilter.GEOSERVER_HSTS_POLICY, "max-age=985500");
        assertEquals("max-age=985500", getHeader(true, "Strict-Transport-Security"));
    }

    private static String getHeader(String name) throws Exception {
        return getHeader(false, name);
    }

    private static String getHeader(boolean secure, String name) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
        request.setScheme(secure ? "https" : "http");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        MockFilterChain mockChain = new MockFilterChain();

        filter.doFilter(request, response, mockChain);

        return response.getHeader(name);
    }
}
