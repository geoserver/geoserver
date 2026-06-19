/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class CachingResourceAccessManagerTest {

    private CachingResourceAccessManager caching;
    private ResourceAccessManager delegate;
    private Authentication user;

    @Before
    public void setUp() {
        delegate = mock(ResourceAccessManager.class);
        caching = new CachingResourceAccessManager();
        caching.setDelegate(delegate);

        user = mock(Authentication.class);
        when(user.getName()).thenReturn("testuser");

        bindRequest();
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    public void testLayerCachesWithinRequest() {
        LayerInfo layer = layerWithId("l1");
        DataAccessLimits limits = mock(DataAccessLimits.class);
        when(delegate.getAccessLimits(user, layer)).thenReturn(limits);

        assertSame(limits, caching.getAccessLimits(user, layer));
        assertSame(limits, caching.getAccessLimits(user, layer));
        verify(delegate, times(1)).getAccessLimits(user, layer);
    }

    @Test
    public void testNullResultCached() {
        LayerInfo layer = layerWithId("l1");
        when(delegate.getAccessLimits(user, layer)).thenReturn(null);

        assertNull(caching.getAccessLimits(user, layer));
        assertNull(caching.getAccessLimits(user, layer));
        verify(delegate, times(1)).getAccessLimits(user, layer);
    }

    @Test
    public void testNoRequestContextDelegatesEveryTime() {
        RequestContextHolder.resetRequestAttributes();
        LayerInfo layer = layerWithId("l1");

        caching.getAccessLimits(user, layer);
        caching.getAccessLimits(user, layer);
        verify(delegate, times(2)).getAccessLimits(user, layer);
    }

    @Test
    public void testDifferentLayersMiss() {
        LayerInfo l1 = layerWithId("l1");
        LayerInfo l2 = layerWithId("l2");

        caching.getAccessLimits(user, l1);
        caching.getAccessLimits(user, l2);
        verify(delegate, times(1)).getAccessLimits(user, l1);
        verify(delegate, times(1)).getAccessLimits(user, l2);
    }

    @Test
    public void testDifferentUsersMiss() {
        Authentication u2 = mock(Authentication.class);
        when(u2.getName()).thenReturn("otheruser");

        LayerInfo layer = layerWithId("l1");
        caching.getAccessLimits(user, layer);
        caching.getAccessLimits(u2, layer);
        verify(delegate, times(1)).getAccessLimits(user, layer);
        verify(delegate, times(1)).getAccessLimits(u2, layer);
    }

    @Test
    public void testContainersDifferentMiss() {
        LayerInfo layer = layerWithId("l1");
        LayerGroupInfo g1 = mock(LayerGroupInfo.class);
        when(g1.getId()).thenReturn("g1");

        caching.getAccessLimits(user, layer, List.of());
        caching.getAccessLimits(user, layer, List.of(g1));
        verify(delegate, times(1)).getAccessLimits(user, layer, List.of());
        verify(delegate, times(1)).getAccessLimits(user, layer, List.of(g1));
    }

    @Test
    public void testContainersSameHit() {
        LayerInfo layer = layerWithId("l1");
        LayerGroupInfo g1 = mock(LayerGroupInfo.class);
        when(g1.getId()).thenReturn("g1");

        caching.getAccessLimits(user, layer, List.of(g1));
        caching.getAccessLimits(user, layer, List.of(g1));
        verify(delegate, times(1)).getAccessLimits(user, layer, List.of(g1));
    }

    @Test
    public void testNullIdSkipsCaching() {
        LayerInfo layer = mock(LayerInfo.class);
        when(layer.getId()).thenReturn(null);

        caching.getAccessLimits(user, layer);
        caching.getAccessLimits(user, layer);
        verify(delegate, times(2)).getAccessLimits(user, layer);
    }

    @Test
    public void testWorkspaceCachesWithinRequest() {
        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        when(ws.getId()).thenReturn("ws1");
        WorkspaceAccessLimits limits = mock(WorkspaceAccessLimits.class);
        when(delegate.getAccessLimits(user, ws)).thenReturn(limits);

        assertSame(limits, caching.getAccessLimits(user, ws));
        assertSame(limits, caching.getAccessLimits(user, ws));
        verify(delegate, times(1)).getAccessLimits(user, ws);
    }

    @Test
    public void testResourceCachesWithinRequest() {
        ResourceInfo res = mock(ResourceInfo.class);
        when(res.getId()).thenReturn("r1");

        caching.getAccessLimits(user, res);
        caching.getAccessLimits(user, res);
        verify(delegate, times(1)).getAccessLimits(user, res);
    }

    @Test
    public void testStyleCachesWithinRequest() {
        StyleInfo style = mock(StyleInfo.class);
        when(style.getId()).thenReturn("s1");

        caching.getAccessLimits(user, style);
        caching.getAccessLimits(user, style);
        verify(delegate, times(1)).getAccessLimits(user, style);
    }

    @Test
    public void testLayerGroupCachesWithinRequest() {
        LayerGroupInfo lg = mock(LayerGroupInfo.class);
        when(lg.getId()).thenReturn("lg1");

        caching.getAccessLimits(user, lg);
        caching.getAccessLimits(user, lg);
        verify(delegate, times(1)).getAccessLimits(user, lg);
    }

    @Test
    public void testSecurityFilterCachesWithinRequest() {
        caching.getSecurityFilter(user, ResourceInfo.class);
        caching.getSecurityFilter(user, ResourceInfo.class);
        verify(delegate, times(1)).getSecurityFilter(user, ResourceInfo.class);
    }

    @Test
    public void testIsWorkspaceAdminCachesWithinRequest() {
        Catalog catalog = mock(Catalog.class);
        when(delegate.isWorkspaceAdmin(user, catalog)).thenReturn(true);

        caching.isWorkspaceAdmin(user, catalog);
        caching.isWorkspaceAdmin(user, catalog);
        verify(delegate, times(1)).isWorkspaceAdmin(user, catalog);
    }

    @Test
    public void testCrossRequestIsolation() {
        LayerInfo layer = layerWithId("l1");
        DataAccessLimits limits = mock(DataAccessLimits.class);
        when(delegate.getAccessLimits(user, layer)).thenReturn(limits);

        caching.getAccessLimits(user, layer);

        // simulate second request
        RequestContextHolder.resetRequestAttributes();
        bindRequest();

        caching.getAccessLimits(user, layer);
        verify(delegate, times(2)).getAccessLimits(user, layer);
    }

    @Test
    public void testStaleRequestAttributesClearedAfterException() {
        // simulate post-request background thread: attrs bound but request already completed
        RequestAttributes stale = mock(RequestAttributes.class);
        when(stale.getAttribute(anyString(), anyInt())).thenThrow(new IllegalStateException("request not active"));
        RequestContextHolder.setRequestAttributes(stale);

        LayerInfo layer = layerWithId("l1");

        // first call: hits the exception, clears the stale binding, delegates
        caching.getAccessLimits(user, layer);
        verify(delegate, times(1)).getAccessLimits(user, layer);
        assertNull("stale binding must be cleared", RequestContextHolder.getRequestAttributes());

        // subsequent calls: no request context, delegate directly, no further exceptions
        caching.getAccessLimits(user, layer);
        verify(delegate, times(2)).getAccessLimits(user, layer);
    }

    private LayerInfo layerWithId(String id) {
        LayerInfo layer = mock(LayerInfo.class);
        when(layer.getId()).thenReturn(id);
        return layer;
    }
}
