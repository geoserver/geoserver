/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link ResolvingProxyResolver}. */
public class ResolvingProxyResolverTest {

    private Catalog catalog;
    private WorkspaceInfo workspace;
    private NamespaceInfo namespace;
    private DataStoreInfo store;
    private FeatureTypeInfo resource;
    private StyleInfo style;
    private LayerInfo layer;
    private LayerGroupInfo layerGroup;

    @Before
    public void setUp() {
        // Create mock catalog and catalog objects
        catalog = mock(Catalog.class);

        // Mock workspace
        workspace = mock(WorkspaceInfo.class);
        when(workspace.getId()).thenReturn("ws-id");
        when(workspace.getName()).thenReturn("ws-name");

        // Mock namespace
        namespace = mock(NamespaceInfo.class);
        when(namespace.getId()).thenReturn("ns-id");
        when(namespace.getPrefix()).thenReturn("ns-prefix");
        when(namespace.getURI()).thenReturn("ns-uri");

        // Mock store
        store = mock(DataStoreInfo.class);
        when(store.getId()).thenReturn("store-id");
        when(store.getName()).thenReturn("store-name");
        when(store.getWorkspace()).thenReturn(workspace);

        // Mock resource
        resource = mock(FeatureTypeInfo.class);
        when(resource.getId()).thenReturn("resource-id");
        when(resource.getName()).thenReturn("resource-name");
        when(resource.getStore()).thenReturn(store);
        when(resource.getNamespace()).thenReturn(namespace);
        if (resource instanceof ResourceInfoImpl impl) {
            impl.setCatalog(catalog);
        }

        // Mock style
        style = mock(StyleInfo.class);
        when(style.getId()).thenReturn("style-id");
        when(style.getName()).thenReturn("style-name");
        when(style.getWorkspace()).thenReturn(workspace);
        if (style instanceof StyleInfoImpl impl) {
            impl.setCatalog(catalog);
        }

        // Mock layer
        layer = mock(LayerInfo.class);
        when(layer.getId()).thenReturn("layer-id");
        when(layer.getName()).thenReturn("layer-name");
        when(layer.getResource()).thenReturn(resource);
        when(layer.getDefaultStyle()).thenReturn(style);
        LinkedHashSet<StyleInfo> styles = new LinkedHashSet<>();
        styles.add(style);
        when(layer.getStyles()).thenReturn(styles);

        // Mock layer group
        layerGroup = mock(LayerGroupInfo.class);
        when(layerGroup.getId()).thenReturn("layergroup-id");
        when(layerGroup.getName()).thenReturn("layergroup-name");
        when(layerGroup.getWorkspace()).thenReturn(workspace);
        when(layerGroup.getRootLayer()).thenReturn(layer);
        when(layerGroup.getRootLayerStyle()).thenReturn(style);
        List<PublishedInfo> layers = Arrays.asList(layer);
        List<StyleInfo> styles2 = Arrays.asList(style);
        when(layerGroup.getLayers()).thenReturn(layers);
        when(layerGroup.getStyles()).thenReturn(styles2);
        LayerGroupStyle groupStyle = mock(LayerGroupStyle.class);
        when(groupStyle.getLayers()).thenReturn(layers);
        when(groupStyle.getStyles()).thenReturn(styles2);
        when(layerGroup.getLayerGroupStyles()).thenReturn(Arrays.asList(groupStyle));

        // Set up catalog
        when(catalog.getWorkspace(workspace.getId())).thenReturn(workspace);
        when(catalog.getNamespace(namespace.getId())).thenReturn(namespace);
        when(catalog.getStore(store.getId(), StoreInfo.class)).thenReturn(store);
        when(catalog.getStore(store.getId(), DataStoreInfo.class)).thenReturn(store);
        when(catalog.getDataStore(store.getId())).thenReturn(store);
        when(catalog.getResource(resource.getId(), ResourceInfo.class)).thenReturn(resource);
        when(catalog.getResource(resource.getId(), FeatureTypeInfo.class)).thenReturn(resource);
        when(catalog.getFeatureType(resource.getId())).thenReturn(resource);
        when(catalog.getLayer(layer.getId())).thenReturn(layer);
        when(catalog.getStyle(style.getId())).thenReturn(style);
        when(catalog.getLayerGroup(layerGroup.getId())).thenReturn(layerGroup);
    }

    @Test
    public void testResolveWorkspace() {
        ResolvingProxyResolver.resolve(workspace, catalog);
        verifyNoMoreInteractions(catalog);
    }

    @Test
    public void testResolveNamespace() {
        ResolvingProxyResolver.resolve(namespace, catalog);
        verifyNoMoreInteractions(catalog);
    }

    @Test
    public void testResolveStore() {
        WorkspaceInfo proxy = ResolvingProxy.create(workspace.getId(), WorkspaceInfo.class);

        store = new DataStoreInfoImpl();
        store.setWorkspace(proxy);

        ResolvingProxyResolver.resolve(store, catalog);
        verify(catalog).getWorkspace(workspace.getId());
        assertSame(workspace, store.getWorkspace());
        assertSame(catalog, store.getCatalog());
    }

    @Test
    public void testResolveStoreMissingWorkspacePreservesProxy() {
        WorkspaceInfo proxy = ResolvingProxy.create("missing", WorkspaceInfo.class);
        when(catalog.getWorkspace("missing")).thenReturn(null);

        store = new DataStoreInfoImpl();
        store.setWorkspace(proxy);
        assertSame(proxy, store.getWorkspace());

        ResolvingProxyResolver.resolve(store, catalog);
        verify(catalog).getWorkspace("missing");
        assertSame(proxy, store.getWorkspace());
    }

    @Test
    public void testResolveResource() {
        StoreInfo proxyStore = ResolvingProxy.create(store.getId(), DataStoreInfo.class);
        NamespaceInfo proxyNamespace = ResolvingProxy.create(namespace.getId(), NamespaceInfo.class);

        ResourceInfo resourceWithProxies = new FeatureTypeInfoImpl();
        resourceWithProxies.setStore(proxyStore);
        resourceWithProxies.setNamespace(proxyNamespace);

        ResolvingProxyResolver.resolve(resourceWithProxies, catalog);

        verify(catalog).getDataStore(store.getId());
        verify(catalog).getNamespace(namespace.getId());
        assertSame(store, resourceWithProxies.getStore());
        assertSame(namespace, resourceWithProxies.getNamespace());
    }

    @Test
    public void testResolveResourceMissingRefs() {
        StoreInfo proxyStore = ResolvingProxy.create("missing-store", DataStoreInfo.class);
        NamespaceInfo proxyNamespace = ResolvingProxy.create("missing-ns", NamespaceInfo.class);

        ResourceInfo resourceWithProxies = new FeatureTypeInfoImpl();
        resourceWithProxies.setStore(proxyStore);
        resourceWithProxies.setNamespace(proxyNamespace);

        ResolvingProxyResolver.resolve(resourceWithProxies, catalog);

        verify(catalog).getDataStore("missing-store");
        verify(catalog).getNamespace("missing-ns");
        assertSame(proxyStore, resourceWithProxies.getStore());
        assertSame(proxyNamespace, resourceWithProxies.getNamespace());
    }

    @Test
    public void testResolveLayer() {
        // Create a proxied resource and style
        FeatureTypeInfo proxyResource = ResolvingProxy.create("resource-id", FeatureTypeInfo.class);
        StyleInfo proxyStyle = ResolvingProxy.create("style-id", StyleInfo.class);

        // Set up layer with proxies
        LayerInfo layerWithProxies = new LayerInfoImpl();
        layerWithProxies.setResource(proxyResource);
        layerWithProxies.setDefaultStyle(proxyStyle);

        layerWithProxies.getStyles().add(proxyStyle);

        // Resolve layer
        ResolvingProxyResolver.resolve(layerWithProxies, catalog);

        // Verify that proxies are resolved
        assertSame(resource, layerWithProxies.getResource());
        assertSame(style, layerWithProxies.getDefaultStyle());
        assertEquals(Set.of(style), layerWithProxies.getStyles());
        verify(catalog).getFeatureType("resource-id");
        verify(catalog, times(2)).getStyle("style-id");
    }

    @Test
    public void testResolveLayerPreservesMissingResourceAndDefaultStyle() {
        // Create a proxied resource and style
        FeatureTypeInfo proxyResource = ResolvingProxy.create("missing-resource-id", FeatureTypeInfo.class);
        StyleInfo proxyStyle = ResolvingProxy.create("missing-style-id", StyleInfo.class);

        // Set up layer with proxies
        LayerInfo layerWithProxies = new LayerInfoImpl();
        layerWithProxies.setResource(proxyResource);
        layerWithProxies.setDefaultStyle(proxyStyle);

        // Resolve layer
        ResolvingProxyResolver.resolve(layerWithProxies, catalog);

        // Verify that proxies are resolved
        assertSame(proxyResource, layerWithProxies.getResource());
        assertSame(proxyStyle, layerWithProxies.getDefaultStyle());
    }

    @Test
    public void testResolveLayerNullifiesMissingStyles() {
        // Create a proxied resource and style
        StyleInfo proxyStyle = ResolvingProxy.create("missing-style-id", StyleInfo.class);

        // Set up layer with proxies
        LayerInfo layerWithProxies = new LayerInfoImpl();
        layerWithProxies.getStyles().add(proxyStyle);

        // Resolve layer
        ResolvingProxyResolver.resolve(layerWithProxies, catalog);

        Set<StyleInfo> expected = new HashSet<>();
        expected.add(null);
        assertEquals(expected, layerWithProxies.getStyles());
    }

    @Test
    public void testResolveLayerWithNullProxies() {
        // Set up layer with null proxies
        LayerInfo layerWithNullProxies = new LayerInfoImpl();
        layerWithNullProxies.setResource(null);
        layerWithNullProxies.setDefaultStyle(null);
        layerWithNullProxies.getStyles().add(null);

        // Resolve layer (should not throw exceptions)
        ResolvingProxyResolver.resolve(layerWithNullProxies, catalog);
        assertNull(layerWithNullProxies.getResource());
        assertNull(layerWithNullProxies.getDefaultStyle());
        Set<StyleInfo> expected = new HashSet<>();
        expected.add(null);
        assertEquals(expected, layerWithNullProxies.getStyles());
    }

    @Test
    public void testResolveLayerGroup() {
        // Create proxied objects
        WorkspaceInfo proxyWorkspace = ResolvingProxy.create("ws-id", WorkspaceInfo.class);
        LayerInfo proxyLayer = ResolvingProxy.create("layer-id", LayerInfo.class);
        StyleInfo proxyStyle = ResolvingProxy.create("style-id", StyleInfo.class);

        // Create layer group with proxies
        LayerGroupInfoImpl layerGroupWithProxies = new LayerGroupInfoImpl();
        layerGroupWithProxies.setWorkspace(proxyWorkspace);
        layerGroupWithProxies.setRootLayer(proxyLayer);
        layerGroupWithProxies.setRootLayerStyle(proxyStyle);

        layerGroupWithProxies.getLayers().add(proxyLayer);
        layerGroupWithProxies.getStyles().add(proxyStyle);

        LayerGroupStyleImpl groupStyle = new LayerGroupStyleImpl();
        groupStyle.getLayers().add(proxyLayer);
        groupStyle.getStyles().add(proxyStyle);
        layerGroupWithProxies.getLayerGroupStyles().add(groupStyle);

        // Resolve layer group
        ResolvingProxyResolver.resolve(layerGroupWithProxies, catalog);

        assertSame(workspace, layerGroupWithProxies.getWorkspace());
        assertSame(layer, layerGroupWithProxies.getRootLayer());
        assertSame(style, layerGroupWithProxies.getRootLayerStyle());

        assertEquals(List.of(layer), layerGroupWithProxies.getLayers());
        assertEquals(List.of(style), layerGroupWithProxies.getStyles());

        LayerGroupStyle lgs = layerGroupWithProxies.getLayerGroupStyles().get(0);
        assertEquals(List.of(layer), lgs.getLayers());
        assertEquals(List.of(style), lgs.getStyles());
    }

    @Test
    public void testResolveLayerGroupPreservesMissingWorkspace() {
        WorkspaceInfo proxyWorkspace = ResolvingProxy.create("missing-ws-id", WorkspaceInfo.class);

        LayerGroupInfoImpl layerGroupWithProxies = new LayerGroupInfoImpl();
        layerGroupWithProxies.setWorkspace(proxyWorkspace);

        ResolvingProxyResolver.resolve(layerGroupWithProxies, catalog);

        assertSame(proxyWorkspace, layerGroupWithProxies.getWorkspace());
    }

    @Test
    public void testResolveLayerGroupStylesSpecialCase() {
        // special case we might have a StyleInfo representing
        // only the name of a LayerGroupStyle thus not present in Catalog.
        // We take the ref and create a new object
        // without searching in catalog.

        LayerGroupInfo proxyLayerGroup = ResolvingProxy.create("missing-layergroup-id", LayerGroupInfo.class);
        StyleInfo proxyStyle = ResolvingProxy.create("another-style-id", StyleInfo.class);

        LayerGroupInfoImpl layerGroupWithProxies = new LayerGroupInfoImpl();

        layerGroupWithProxies.getLayers().add(proxyLayerGroup);
        layerGroupWithProxies.getStyles().add(proxyStyle);

        ResolvingProxyResolver.resolve(layerGroupWithProxies, catalog);

        StyleInfoImpl expected = new StyleInfoImpl();
        expected.setName("another-style-id");

        assertEquals(List.of(expected), layerGroupWithProxies.getStyles());
    }

    @Test
    public void testResolveLayerGroupStylesNull() {
        LayerGroupInfo proxyLayerGroup = ResolvingProxy.create("missing-layergroup-id", LayerGroupInfo.class);

        LayerGroupInfoImpl layerGroupWithProxies = new LayerGroupInfoImpl();

        layerGroupWithProxies.getLayers().add(proxyLayerGroup);
        layerGroupWithProxies.getStyles().add(null);

        ResolvingProxyResolver.resolve(layerGroupWithProxies, catalog);

        List<StyleInfo> expected = new ArrayList<>();
        expected.add(null);
        assertEquals(expected, layerGroupWithProxies.getStyles());
    }

    @Test
    public void testResolveStyle() {
        // Create proxy workspace
        WorkspaceInfo proxyWorkspace = ResolvingProxy.create("ws-id", WorkspaceInfo.class);

        when(catalog.getWorkspace("missing")).thenReturn(null);

        // Create style with proxy workspace
        StyleInfoImpl styleWithProxy = new StyleInfoImpl();
        styleWithProxy.setWorkspace(proxyWorkspace);

        // Resolve style
        ResolvingProxyResolver.resolve(styleWithProxy, catalog);

        // Verify that workspace is resolved
        verify(catalog).getWorkspace("ws-id");
        assertSame(workspace, styleWithProxy.getWorkspace());
        assertSame(catalog, styleWithProxy.getCatalog());
    }

    @Test
    public void testResolveStyleKeepsMissingProxy() {
        // Create proxy workspace
        WorkspaceInfo proxyWorkspace = ResolvingProxy.create("missing", WorkspaceInfo.class);
        when(catalog.getWorkspace("missing")).thenReturn(null);

        // Create style with proxy workspace
        StyleInfoImpl styleWithProxy = new StyleInfoImpl();
        styleWithProxy.setWorkspace(proxyWorkspace);

        // Resolve style
        ResolvingProxyResolver.resolve(styleWithProxy, catalog);

        // Verify that workspace is resolved
        verify(catalog).getWorkspace("missing");
        assertSame(proxyWorkspace, styleWithProxy.getWorkspace());
        assertSame(catalog, styleWithProxy.getCatalog());
    }

    @Test
    public void testResolveLayerGroupStylesWithSpecialCase() {
        // Test for the special case in resolveLayerGroupStyles where a StyleInfo
        // represents only the name of a LayerGroupStyle

        // Create proxied layer group and style
        LayerGroupInfo proxyLayerGroup = ResolvingProxy.create("layergroup-id", LayerGroupInfo.class);
        StyleInfo proxyStyle = ResolvingProxy.create("layergroup-style-name", StyleInfo.class);

        // Create layer group and style list
        List<PublishedInfo> layers = Arrays.asList(proxyLayerGroup);
        List<StyleInfo> styles = Arrays.asList(proxyStyle);

        // Call the private method through the public resolve method
        LayerGroupInfoImpl testLayerGroup = new LayerGroupInfoImpl();
        testLayerGroup.setLayers(layers);
        testLayerGroup.setStyles(styles);

        ResolvingProxyResolver.resolve(testLayerGroup, catalog);

        // The styles should now contain a newly created style with the reference name
        StyleInfo resolvedStyle = testLayerGroup.getStyles().get(0);
        assertNotNull(resolvedStyle);
        // In a real scenario, this would be the name of the layer group style
        assertEquals("layergroup-style-name", resolvedStyle.getName());
    }

    @Test
    public void testResolveMapInfo() {
        ResolvingProxyResolver.resolve(new MapInfoImpl(), catalog);
        verifyNoMoreInteractions(catalog);
    }

    @Test
    public void testUnknownCatalognfo() {
        ResolvingProxyResolver.resolve(mock(CatalogInfo.class), catalog);
        verifyNoMoreInteractions(catalog);
    }
}
