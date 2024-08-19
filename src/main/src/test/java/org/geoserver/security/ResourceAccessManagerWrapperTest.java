/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;

public class ResourceAccessManagerWrapperTest {

    private ResourceAccessManagerWrapper wrapper;
    private ResourceAccessManager delegate;
    private Authentication user;

    @Before
    public void setUp() {
        user = mock(Authentication.class);
        delegate = mock(ResourceAccessManager.class);
        wrapper = new ResourceAccessManagerWrapper() {};
        wrapper.setDelegate(delegate);
    }

    @Test
    public void getAccessLimitsWorkspaceInfo() {
        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        wrapper.getAccessLimits(user, ws);
        verify(delegate, times(1)).getAccessLimits(user, ws);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void getAccessLimitsLayerInfo() {
        LayerInfo layer = mock(LayerInfo.class);
        wrapper.getAccessLimits(user, layer);
        verify(delegate, times(1)).getAccessLimits(user, layer);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void getAccessLimitsLayerInfoContainers() {
        LayerInfo layer = mock(LayerInfo.class);
        List<LayerGroupInfo> containers = List.of();
        wrapper.getAccessLimits(user, layer, containers);
        verify(delegate, times(1)).getAccessLimits(user, layer, containers);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void getAccessLimitsResourceInfo() {
        ResourceInfo res = mock(ResourceInfo.class);
        wrapper.getAccessLimits(user, res);
        verify(delegate, times(1)).getAccessLimits(user, res);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void getAccessLimitsStyleInfo() {
        StyleInfo style = mock(StyleInfo.class);
        wrapper.getAccessLimits(user, style);
        verify(delegate, times(1)).getAccessLimits(user, style);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void getAccessLimitsLayerGroupInfo() {
        LayerGroupInfo lg = mock(LayerGroupInfo.class);
        wrapper.getAccessLimits(user, lg);
        verify(delegate, times(1)).getAccessLimits(user, lg);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void getAccessLimitsLayerGroupInfoContainers() {
        LayerGroupInfo lg = mock(LayerGroupInfo.class);
        List<LayerGroupInfo> containers = List.of();
        wrapper.getAccessLimits(user, lg, containers);
        verify(delegate, times(1)).getAccessLimits(user, lg, containers);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void getSecurityFilter() {
        wrapper.getSecurityFilter(user, ResourceInfo.class);
        verify(delegate, times(1)).getSecurityFilter(user, ResourceInfo.class);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void isWorkspaceAdmin() {
        Catalog catalog = mock(Catalog.class);
        wrapper.isWorkspaceAdmin(user, catalog);
        verify(delegate, times(1)).isWorkspaceAdmin(user, catalog);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void unwrap() {
        assertSame(delegate, wrapper.unwrap());
        wrapper.setDelegate(null);
        assertNull(wrapper.unwrap());
        wrapper.setDelegate(delegate);
        assertSame(delegate, wrapper.unwrap());
    }
}
