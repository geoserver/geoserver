/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Tests for {@link HttpServletRequestSupplier}. */
public class HttpServletRequestSupplierTest {

    @Before
    public void setUp() {
        // Clear any existing request attributes before each test
        RequestContextHolder.resetRequestAttributes();
    }

    @After
    public void tearDown() {
        // Clean up after each test
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testImplementsSupplier() {
        HttpServletRequestSupplier supplier = new HttpServletRequestSupplier();

        assertTrue("Should implement Supplier interface", supplier instanceof Supplier);
    }

    @Test
    public void testGetThrowsExceptionWhenNoRequestContext() {
        HttpServletRequestSupplier supplier = new HttpServletRequestSupplier();

        try {
            supplier.get();
            fail("Expected IllegalStateException when no request context");
        } catch (IllegalStateException e) {
            assertEquals("Failed to obtain ServletRequestAttributes.", e.getMessage());
        }
    }

    @Test
    public void testGetReturnsRequestWhenContextIsSet() {
        // Set up a mock request in the RequestContextHolder
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("GET");
        mockRequest.setRequestURI("/test/path");
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        HttpServletRequestSupplier supplier = new HttpServletRequestSupplier();

        HttpServletRequest result = supplier.get();

        assertNotNull("Should return a request", result);
        assertSame("Should return the same mock request", mockRequest, result);
    }

    @Test
    public void testGetReturnsCorrectRequestProperties() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("POST");
        mockRequest.setRequestURI("/api/resource");
        mockRequest.setParameter("param1", "value1");
        mockRequest.addHeader("Authorization", "Bearer token123");
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        HttpServletRequestSupplier supplier = new HttpServletRequestSupplier();

        HttpServletRequest result = supplier.get();

        assertEquals("POST", result.getMethod());
        assertEquals("/api/resource", result.getRequestURI());
        assertEquals("value1", result.getParameter("param1"));
        assertEquals("Bearer token123", result.getHeader("Authorization"));
    }

    @Test
    public void testMultipleCallsReturnSameRequest() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        HttpServletRequestSupplier supplier = new HttpServletRequestSupplier();

        HttpServletRequest result1 = supplier.get();
        HttpServletRequest result2 = supplier.get();

        assertSame("Multiple calls should return the same request", result1, result2);
    }

    @Test
    public void testDifferentSupplierInstancesReturnSameRequest() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        HttpServletRequestSupplier supplier1 = new HttpServletRequestSupplier();
        HttpServletRequestSupplier supplier2 = new HttpServletRequestSupplier();

        HttpServletRequest result1 = supplier1.get();
        HttpServletRequest result2 = supplier2.get();

        assertSame("Different supplier instances should return the same request", result1, result2);
    }

    @Test
    public void testGetAfterContextReset() {
        // First set up a context
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        HttpServletRequestSupplier supplier = new HttpServletRequestSupplier();

        // Verify it works
        assertNotNull(supplier.get());

        // Reset the context
        RequestContextHolder.resetRequestAttributes();

        // Now it should throw
        try {
            supplier.get();
            fail("Expected IllegalStateException after context reset");
        } catch (IllegalStateException e) {
            assertEquals("Failed to obtain ServletRequestAttributes.", e.getMessage());
        }
    }

    @Test
    public void testGetWithDifferentRequestMethods() {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"};

        for (String method : methods) {
            MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.setMethod(method);
            ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
            RequestContextHolder.setRequestAttributes(attrs);

            HttpServletRequestSupplier supplier = new HttpServletRequestSupplier();
            HttpServletRequest result = supplier.get();

            assertEquals("Should handle " + method + " requests", method, result.getMethod());

            RequestContextHolder.resetRequestAttributes();
        }
    }
}
