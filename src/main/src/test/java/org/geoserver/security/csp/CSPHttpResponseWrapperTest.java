/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY_REPORT_ONLY;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CSPHttpResponseWrapperTest {

    @Mock
    private HttpServletResponse response;

    private CSPHttpResponseWrapper wrapper;

    private CSPConfiguration config;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        this.closeable = MockitoAnnotations.openMocks(this);
        this.config = new CSPConfiguration();
        this.config.setReportOnly(false);
        this.wrapper = new CSPHttpResponseWrapper(this.response, this.config);
    }

    @After
    public void tearDown() throws Exception {
        this.closeable.close();
    }

    @Test
    public void testNotCSP() throws Exception {
        this.wrapper.setHeader(CONTENT_TYPE, "text/plain");
        verify(this.response).setHeader(CONTENT_TYPE, "text/plain");
        verify(this.response, never()).getHeader(CONTENT_TYPE);
    }

    @Test
    public void testCSPDisabledWithOverride() throws Exception {
        // if config is disabled, don't do anything special and just set the header
        this.config.setEnabled(false);
        this.config.setAllowOverride(true);
        String csp = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY, csp);
        verify(this.response, never()).getHeader(CONTENT_SECURITY_POLICY);
        verify(this.response, never()).getHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY, csp);
    }

    @Test
    public void testCSPDisabledWithoutOverride() throws Exception {
        // if config is disabled, don't do anything special and just set the header
        this.config.setEnabled(false);
        String csp = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY, csp);
        verify(this.response, never()).getHeader(CONTENT_SECURITY_POLICY);
        verify(this.response, never()).getHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY);
        verify(this.response, never()).setHeader(CONTENT_SECURITY_POLICY, csp);
    }

    @Test
    public void testCSPReportDisabledWithOverride() throws Exception {
        // if config is disabled, don't do anything special and just set the header
        this.config.setEnabled(false);
        this.config.setAllowOverride(true);
        String csp = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp);
        verify(this.response, never()).getHeader(CONTENT_SECURITY_POLICY);
        verify(this.response, never()).getHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp);
    }

    @Test
    public void testCSPReportDisabledWithoutOverride() throws Exception {
        // if config is disabled, don't do anything special and just set the header
        this.config.setEnabled(false);
        String csp = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp);
        verify(this.response, never()).getHeader(CONTENT_SECURITY_POLICY);
        verify(this.response, never()).getHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY);
        verify(this.response, never()).setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp);
    }

    @Test
    public void testCSPNotSet() throws Exception {
        String csp = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY, csp);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY, csp);
    }

    @Test
    public void testCSPReportNotSet() throws Exception {
        this.config.setReportOnly(true);
        String csp = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp);
    }

    @Test
    public void testCSPWithoutMerge() throws Exception {
        String csp1 = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        String csp2 = "base-uri 'none'; default-src 'none';, frame-ancestors 'none';";
        String csp3 = csp2;
        when(this.response.getHeader(CONTENT_SECURITY_POLICY)).thenReturn(csp1);
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY, csp2);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY, csp3);
    }

    @Test
    public void testCSPReportWithoutMerge() throws Exception {
        this.config.setReportOnly(true);
        String csp1 = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        String csp2 = "base-uri 'none'; default-src 'none';, frame-ancestors 'none';";
        String csp3 = csp2;
        when(this.response.getHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY)).thenReturn(csp1);
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp2);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp3);
    }

    @Test
    public void testCSPWithMerge() throws Exception {
        String csp1 = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        String csp2 = "base-uri 'none'; form-action 'none'; default-src 'none';";
        String csp3 = "base-uri 'none'; form-action 'none'; default-src 'none';, frame-ancestors 'self';";
        when(this.response.getHeader(CONTENT_SECURITY_POLICY)).thenReturn(csp1);
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY, csp2);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY, csp3);
    }

    @Test
    public void testCSPReportWithMerge() throws Exception {
        this.config.setReportOnly(true);
        String csp1 = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        String csp2 = "base-uri 'none'; form-action 'none'; default-src 'none';";
        String csp3 = "base-uri 'none'; form-action 'none'; default-src 'none';, frame-ancestors 'self';";
        when(this.response.getHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY)).thenReturn(csp1);
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp2);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp3);
    }

    @Test
    public void testCSPOverride() throws Exception {
        this.config.setAllowOverride(true);
        String csp1 = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        String csp2 = "base-uri 'none'; form-action 'none'; default-src 'none';";
        when(this.response.getHeader(CONTENT_SECURITY_POLICY)).thenReturn(csp1);
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY, csp2);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY, csp2);
    }

    @Test
    public void testCSPReportOverride() throws Exception {
        this.config.setAllowOverride(true);
        this.config.setReportOnly(true);
        String csp1 = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        String csp2 = "base-uri 'none'; form-action 'none'; default-src 'none';";
        when(this.response.getHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY)).thenReturn(csp1);
        this.wrapper.setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp2);
        verify(this.response).setHeader(CONTENT_SECURITY_POLICY_REPORT_ONLY, csp2);
    }
}
