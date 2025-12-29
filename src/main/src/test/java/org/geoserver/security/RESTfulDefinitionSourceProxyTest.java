/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;

/** Unit tests for {@link RESTfulDefinitionSourceProxy}. */
public class RESTfulDefinitionSourceProxyTest {

    private RESTfulDefinitionSourceProxy proxy;
    private FilterInvocationSecurityMetadataSource delegate1;
    private FilterInvocationSecurityMetadataSource delegate2;
    private HttpServletRequest request;

    @Before
    public void setUp() {
        delegate1 = mock(FilterInvocationSecurityMetadataSource.class);
        delegate2 = mock(FilterInvocationSecurityMetadataSource.class);
        request = mock(HttpServletRequest.class);

        // Create proxy with two delegates
        proxy = new RESTfulDefinitionSourceProxy(Arrays.asList(delegate1, delegate2));
    }

    @Test
    public void testSupportsSingleDelegate() {
        // When at least one delegate supports the class, proxy should support it
        when(delegate1.supports(HttpServletRequest.class)).thenReturn(true);
        when(delegate2.supports(HttpServletRequest.class)).thenReturn(false);

        assertTrue(proxy.supports(HttpServletRequest.class));

        // When no delegate supports the class, proxy should not support it
        when(delegate1.supports(String.class)).thenReturn(false);
        when(delegate2.supports(String.class)).thenReturn(false);

        assertFalse(proxy.supports(String.class));
    }

    @Test
    public void testSupportsMultipleDelegates() {
        // When both delegates support the class, proxy should support it
        when(delegate1.supports(HttpServletRequest.class)).thenReturn(true);
        when(delegate2.supports(HttpServletRequest.class)).thenReturn(true);

        assertTrue(proxy.supports(HttpServletRequest.class));
    }

    @Test
    public void testGetAttributesSingleDelegate() {
        // Create proxy with a single delegate
        proxy = new RESTfulDefinitionSourceProxy(Collections.singletonList(delegate1));

        // Setup delegate to return some attributes
        ConfigAttribute attr1 = new SecurityConfig("ROLE_ADMIN");
        ConfigAttribute attr2 = new SecurityConfig("ROLE_USER");
        when(delegate1.getAttributes(request)).thenReturn(Arrays.asList(attr1, attr2));

        // Proxy should return the same attributes
        Collection<ConfigAttribute> attributes = proxy.getAttributes(request);
        assertEquals(2, attributes.size());
        assertTrue(attributes.contains(attr1));
        assertTrue(attributes.contains(attr2));

        // Verify delegate was called
        verify(delegate1, times(1)).getAttributes(request);
    }

    @Test
    public void testGetAttributesMultipleDelegates() {
        // Setup delegates to return different attributes
        ConfigAttribute attr1 = new SecurityConfig("ROLE_ADMIN");
        ConfigAttribute attr2 = new SecurityConfig("ROLE_USER");
        ConfigAttribute attr3 = new SecurityConfig("ROLE_WORKSPACE_ADMIN");

        when(delegate1.getAttributes(request)).thenReturn(Arrays.asList(attr1, attr2));
        when(delegate2.getAttributes(request)).thenReturn(Collections.singletonList(attr3));

        // Proxy should merge attributes from both delegates
        Collection<ConfigAttribute> attributes = proxy.getAttributes(request);
        assertEquals(3, attributes.size());
        assertTrue(attributes.contains(attr1));
        assertTrue(attributes.contains(attr2));
        assertTrue(attributes.contains(attr3));

        // Verify both delegates were called
        verify(delegate1, times(1)).getAttributes(request);
        verify(delegate2, times(1)).getAttributes(request);
    }

    @Test
    public void testGetAttributesNullResponse() {
        // Setup one delegate to return null (this can happen in real implementations)
        when(delegate1.getAttributes(request)).thenReturn(null);
        ConfigAttribute attr = new SecurityConfig("ROLE_USER");
        when(delegate2.getAttributes(request)).thenReturn(Collections.singletonList(attr));

        // Proxy should handle null response and still return attributes from other delegate
        Collection<ConfigAttribute> attributes = proxy.getAttributes(request);
        assertEquals(1, attributes.size());
        assertTrue(attributes.contains(attr));
    }

    @Test
    public void testGetAllConfigAttributes() {
        // Setup delegates to return different attributes
        ConfigAttribute attr1 = new SecurityConfig("ROLE_ADMIN");
        ConfigAttribute attr2 = new SecurityConfig("ROLE_USER");
        ConfigAttribute attr3 = new SecurityConfig("ROLE_WORKSPACE_ADMIN");

        when(delegate1.getAllConfigAttributes()).thenReturn(Arrays.asList(attr1, attr2));
        when(delegate2.getAllConfigAttributes()).thenReturn(Collections.singletonList(attr3));

        // Proxy should merge all config attributes from both delegates
        Collection<ConfigAttribute> attributes = proxy.getAllConfigAttributes();
        assertEquals(3, attributes.size());
        assertTrue(attributes.contains(attr1));
        assertTrue(attributes.contains(attr2));
        assertTrue(attributes.contains(attr3));
    }

    @Test
    public void testGetAllConfigAttributesWithSingleDelegate() {
        // Create proxy with a single delegate
        proxy = new RESTfulDefinitionSourceProxy(Collections.singletonList(delegate1));

        // Setup delegate to return some attributes
        ConfigAttribute attr1 = new SecurityConfig("ROLE_ADMIN");
        ConfigAttribute attr2 = new SecurityConfig("ROLE_USER");
        when(delegate1.getAllConfigAttributes()).thenReturn(Arrays.asList(attr1, attr2));

        // Proxy should return the same attributes
        Collection<ConfigAttribute> attributes = proxy.getAllConfigAttributes();
        assertEquals(2, attributes.size());
        assertTrue(attributes.contains(attr1));
        assertTrue(attributes.contains(attr2));
    }
}
