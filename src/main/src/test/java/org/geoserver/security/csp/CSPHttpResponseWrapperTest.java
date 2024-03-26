/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CSPHttpResponseWrapperTest {

    @Mock private HttpServletResponse response;

    @InjectMocks private CSPHttpResponseWrapper wrapper;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        this.closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        this.closeable.close();
    }

    @Test
    public void testNotCSP() throws Exception {
        this.wrapper.setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        verify(this.response).setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        verify(this.response, never()).getHeader(HttpHeaders.CONTENT_TYPE);
    }

    @Test
    public void testCSPWithoutMerge() throws Exception {
        String csp1 = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        String csp2 = "base-uri 'none'; default-src 'none';, frame-ancestors 'none';";
        String csp3 = csp2;
        when(this.response.getHeader(HttpHeaders.CONTENT_SECURITY_POLICY)).thenReturn(csp1);
        this.wrapper.setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, csp2);
        verify(this.response).setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, csp3);
    }

    @Test
    public void testCSPWithMerge() throws Exception {
        String csp1 = "base-uri 'self'; default-src 'self';, frame-ancestors 'self';";
        String csp2 = "base-uri 'none'; form-action 'none'; default-src 'none';";
        String csp3 =
                "base-uri 'none'; form-action 'none'; default-src 'none';, frame-ancestors 'self';";
        when(this.response.getHeader(HttpHeaders.CONTENT_SECURITY_POLICY)).thenReturn(csp1);
        this.wrapper.setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, csp2);
        verify(this.response).setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, csp3);
    }
}
