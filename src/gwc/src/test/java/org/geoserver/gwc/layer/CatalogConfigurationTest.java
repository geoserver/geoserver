/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;
import static org.geoserver.gwc.GWCTestHelpers.mockGroup;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.ows.LocalWorkspace;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the GWCCatalogListener
 *
 * @author groldan
 */
public class CatalogConfigurationTest {

    private Catalog catalog;

    private TileLayerCatalog tileLayerCatalog;

    private GridSetBroker gridSetBroker;

    private CatalogConfiguration config;

    private LayerInfo layer1, layer2, layerWithNoTileLayer;
    private LayerGroupInfo group1, group2, groupWithNoTileLayer;
    private GeoServerTileLayerInfo layerInfo1, layerInfo2, groupInfo1, groupInfo2;

    private GWC mockMediator;

    private GWCConfig defaults;

    @Before
    public void setUp() throws Exception {
        defaults = GWCConfig.getOldDefaults();
        defaults.getDefaultVectorCacheFormats().clear();
        defaults.getDefaultVectorCacheFormats().add("image/png8");
        defaults.getDefaultOtherCacheFormats().clear();
        defaults.getDefaultOtherCacheFormats().add("image/jpeg");
        defaults.setCacheLayersByDefault(false);
        defaults.setCacheNonDefaultStyles(true);

        layer1 = mockLayer("layer1", "test", new String[] {}, PublishedType.RASTER);
        layer2 = mockLayer("layer2", "test", new String[] {}, PublishedType.RASTER);
        layerWithNoTileLayer =
                mockLayer("layerWithNoTileLayer", new String[] {}, PublishedType.RASTER);

        group1 = mockGroup("group1", layer1, layer2);
        group2 = mockGroup("group2", layer2, layer1);
        groupWithNoTileLayer =
                mockGroup("groupWithNoTileLayer", layerWithNoTileLayer, layer1, layer2);

        // set tile layer settings to layer1, layer2, group1, and group2
        layerInfo1 = TileLayerInfoUtil.loadOrCreate(layer1, defaults);
        layerInfo1.setMetaTilingX(1);
        layerInfo1.setMetaTilingY(1);

        layerInfo2 = TileLayerInfoUtil.loadOrCreate(layer2, defaults);
        layerInfo2.setMetaTilingX(2);
        layerInfo2.setMetaTilingY(2);

        groupInfo1 = TileLayerInfoUtil.loadOrCreate(group1, defaults);
        groupInfo1.setMetaTilingX(3);
        groupInfo1.setMetaTilingY(3);

        groupInfo2 = TileLayerInfoUtil.loadOrCreate(group2, defaults);
        groupInfo2.setMetaTilingX(4);
        groupInfo2.setMetaTilingY(4);

        catalog = mock(Catalog.class);
        // catalog returns the three layers, two with tile layer and one without
        when(catalog.getLayers())
                .thenReturn(ImmutableList.of(layer1, layerWithNoTileLayer, layer2));
        when(catalog.getLayer(layer1.getId())).thenReturn(layer1);
        when(catalog.getLayer(layer2.getId())).thenReturn(layer2);
        when(catalog.getLayerGroup(group1.getId())).thenReturn(group1);
        when(catalog.getLayerGroup(group2.getId())).thenReturn(group2);

        // catalog returns the three layer groups, two with tile layer and one without
        when(catalog.getLayerGroups())
                .thenReturn(ImmutableList.of(group1, groupWithNoTileLayer, group2));
        when(catalog.getLayerByName(eq(tileLayerName(layer1)))).thenReturn(layer1);
        when(catalog.getLayerByName(eq(tileLayerName(layer2)))).thenReturn(layer2);
        when(catalog.getLayerByName(eq(tileLayerName(layerWithNoTileLayer))))
                .thenReturn(layerWithNoTileLayer);

        when(catalog.getLayerGroupByName(eq(tileLayerName(group1)))).thenReturn(group1);
        when(catalog.getLayerGroupByName(eq(tileLayerName(group2)))).thenReturn(group2);
        when(catalog.getLayerGroupByName(eq(tileLayerName(groupWithNoTileLayer))))
                .thenReturn(groupWithNoTileLayer);

        gridSetBroker =
                new GridSetBroker(Collections.singletonList(new DefaultGridsets(true, true)));

        Set<String> layerNames =
                ImmutableSet.of(
                        tileLayerName(layer1),
                        tileLayerName(layer2),
                        tileLayerName(group1),
                        tileLayerName(group2));

        tileLayerCatalog = mock(TileLayerCatalog.class);
        when(tileLayerCatalog.getLayerIds())
                .thenReturn(
                        ImmutableSet.of(
                                layer1.getId(), layer2.getId(), group1.getId(), group2.getId()));
        when(tileLayerCatalog.getLayerNames()).thenReturn(layerNames);

        when(tileLayerCatalog.getLayerById(layer1.getId())).thenReturn(layerInfo1);
        when(tileLayerCatalog.getLayerById(layer2.getId())).thenReturn(layerInfo2);
        when(tileLayerCatalog.getLayerById(group1.getId())).thenReturn(groupInfo1);
        when(tileLayerCatalog.getLayerById(group2.getId())).thenReturn(groupInfo2);

        when(tileLayerCatalog.exists(layer1.getId())).thenReturn(true);
        when(tileLayerCatalog.exists(layer2.getId())).thenReturn(true);
        when(tileLayerCatalog.exists(group1.getId())).thenReturn(true);
        when(tileLayerCatalog.exists(group2.getId())).thenReturn(true);

        when(tileLayerCatalog.getLayerByName(tileLayerName(layer1))).thenReturn(layerInfo1);
        when(tileLayerCatalog.getLayerByName(tileLayerName(layer2))).thenReturn(layerInfo2);
        when(tileLayerCatalog.getLayerByName(tileLayerName(group1))).thenReturn(groupInfo1);
        when(tileLayerCatalog.getLayerByName(tileLayerName(group2))).thenReturn(groupInfo2);

        when(tileLayerCatalog.getLayerId(tileLayerName(layer1))).thenReturn(layer1.getId());
        when(tileLayerCatalog.getLayerId(tileLayerName(layer2))).thenReturn(layer2.getId());
        when(tileLayerCatalog.getLayerId(tileLayerName(group1))).thenReturn(group1.getId());
        when(tileLayerCatalog.getLayerId(tileLayerName(group2))).thenReturn(group2.getId());

        config = new CatalogConfiguration(catalog, tileLayerCatalog, gridSetBroker);

        mockMediator = mock(GWC.class);
        GWC.set(mockMediator);
        when(mockMediator.getConfig()).thenReturn(defaults);
        when(mockMediator.getCatalog()).thenReturn(catalog);
    }

    @After
    public void tearDown() {
        GWC.set(null);
    }

    @Test
    public void testGoofyMethods() {
        assertEquals("GeoServer Catalog Configuration", config.getIdentifier());
    }

    @Test
    public void testInitialize() {
        config.afterPropertiesSet();
    }

    @Test
    public void testGetTileLayerCount() {
        assertEquals(4, config.getLayerCount());
    }

    @Test
    public void testGetTileLayerNames() {
        Set<String> expected =
                ImmutableSet.of(
                        tileLayerName(layer1),
                        tileLayerName(layer2),
                        tileLayerName(group1),
                        tileLayerName(group2));

        Set<String> actual = config.getLayerNames();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetLayers() {
        Iterable<TileLayer> layers = config.getLayers();
        testGetLayers(layers);
    }

    @Test
    public void testDeprecatedGetTileLayers() {
        @SuppressWarnings("deprecation")
        Iterable<TileLayer> layers = config.getLayers();
        testGetLayers(layers);
    }

    private void testGetLayers(Iterable<TileLayer> layers) {
        assertEquals(3, catalog.getLayers().size());
        assertEquals(3, catalog.getLayerGroups().size());
        assertEquals(4, Iterables.size(layers));

        Set<GeoServerTileLayerInfo> expected =
                ImmutableSet.of(layerInfo1, layerInfo2, groupInfo1, groupInfo2);
        Set<GeoServerTileLayerInfo> actual = new HashSet<GeoServerTileLayerInfo>();

        for (TileLayer layer : layers) {
            actual.add(((GeoServerTileLayer) layer).getInfo());
        }
        assertEquals(4, actual.size());
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTileLayer() {
        String layerName = tileLayerName(layerWithNoTileLayer);
        assertFalse(config.getLayer(layerName).isPresent());
        assertFalse(config.getLayer(tileLayerName(groupWithNoTileLayer)).isPresent());

        assertTrue(config.getLayer(tileLayerName(layer1)).isPresent());
        assertTrue(config.getLayer(tileLayerName(layer2)).isPresent());
        assertTrue(config.getLayer(tileLayerName(group1)).isPresent());
        assertTrue(config.getLayer(tileLayerName(group2)).isPresent());

        assertFalse(config.getLayer("anythingElse").isPresent());
    }

    @Test
    public void testModifyLayer() {
        try {
            config.modifyLayer(null);
            fail("expected precondition exception");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("is null"));
        }
        try {
            config.modifyLayer(mock(TileLayer.class));
            fail("expected precondition exception");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Can't save TileLayer of type"));
        }

        GeoServerTileLayer tileLayer1 =
                (GeoServerTileLayer) config.getLayer(tileLayerName(layer1)).get();
        GeoServerTileLayer tileLayer2 =
                (GeoServerTileLayer) config.getLayer(tileLayerName(group1)).get();

        testModifyLayer(tileLayer1);
        testModifyLayer(tileLayer2);
    }

    private void testModifyLayer(GeoServerTileLayer orig) {

        GeoServerTileLayerInfo newState = TileLayerInfoUtil.create(defaults);
        newState.setId(orig.getInfo().getId());
        newState.setName(orig.getInfo().getName());
        assertFalse(orig.equals(newState));

        final GeoServerTileLayer modified =
                new GeoServerTileLayer(orig.getPublishedInfo(), gridSetBroker, newState);

        assertEquals(
                orig.getInfo(),
                ((GeoServerTileLayer) config.getLayer(orig.getName()).get()).getInfo());

        // Update mocks
        when(tileLayerCatalog.save(modified.getInfo())).thenReturn(orig.getInfo());

        config.modifyLayer(modified);

        when(tileLayerCatalog.getLayerById(modified.getId())).thenReturn(modified.getInfo());

        assertEquals(
                newState, ((GeoServerTileLayer) config.getLayer(orig.getName()).get()).getInfo());

        final String origName = orig.getName();
        modified.getInfo().setName("changed");

        when(tileLayerCatalog.save(modified.getInfo())).thenReturn(orig.getInfo());

        config.modifyLayer(modified);

        // Update mocks
        when(tileLayerCatalog.getLayerById(orig.getId())).thenReturn(null);
        when(tileLayerCatalog.getLayerId(orig.getName())).thenReturn(null);
        when(tileLayerCatalog.getLayerNames()).thenReturn(Collections.emptySet());

        assertFalse(config.getLayer(origName).isPresent());
        assertFalse(config.getLayerNames().contains(origName));
    }

    @Test
    public void testRemoveLayer() {
        try {
            config.removeLayer(null);
            fail("expected precondition violation exception");
        } catch (RuntimeException e) {
            assertTrue(true);
        }
        try {
            config.removeLayer(GWC.tileLayerName(layerWithNoTileLayer));
            fail("expected precondition violation exception");
        } catch (RuntimeException e) {
            assertTrue(true);
        }
        try {
            config.removeLayer(GWC.tileLayerName(groupWithNoTileLayer));
            fail("expected precondition violation exception");
        } catch (RuntimeException e) {
            assertTrue(true);
        }

        String layerName;

        layerName = tileLayerName(layer1);
        assertNotNull(config.getLayer(layerName));

        final int initialCount = config.getLayerCount();

        config.removeLayer(layerName);

        // Update mocks
        when(tileLayerCatalog.getLayerByName(layerName)).thenReturn(null);
        when(tileLayerCatalog.getLayerId(layerName)).thenReturn(null);
        when(tileLayerCatalog.getLayerNames())
                .thenReturn(
                        ImmutableSet.of(
                                tileLayerName(layer2),
                                tileLayerName(group1),
                                tileLayerName(group2)));
        when(tileLayerCatalog.getLayerIds())
                .thenReturn(ImmutableSet.of(layer2.getId(), group1.getId(), group2.getId()));

        assertFalse(config.getLayer(layerName).isPresent());
        assertFalse(config.getLayerNames().contains(layerName));
        assertEquals(initialCount - 1, config.getLayerCount());

        layerName = GWC.tileLayerName(group1);
        assertNotNull(config.getLayer(layerName));
        config.removeLayer(layerName);

        // Update mocks
        when(tileLayerCatalog.getLayerByName(layerName)).thenReturn(null);
        when(tileLayerCatalog.getLayerId(layerName)).thenReturn(null);
        when(tileLayerCatalog.getLayerNames())
                .thenReturn(ImmutableSet.of(tileLayerName(layer2), tileLayerName(group2)));
        when(tileLayerCatalog.getLayerIds())
                .thenReturn(ImmutableSet.of(layer2.getId(), group2.getId()));

        assertFalse(config.getLayer(layerName).isPresent());
        assertEquals(initialCount - 2, config.getLayerCount());
    }

    @Test
    public void testSaveRename() {

        GeoServerTileLayerInfo originalState = layerInfo1;

        GeoServerTileLayerInfo forceState1 = TileLayerInfoUtil.loadOrCreate(layer1, defaults);
        when(tileLayerCatalog.save(same(forceState1))).thenReturn(originalState);
        forceState1.setName("newName");

        config.modifyLayer(new GeoServerTileLayer(layer1, gridSetBroker, forceState1));

        verify(mockMediator, never()).layerRemoved(anyString());
        verify(tileLayerCatalog, times(1)).save(same(forceState1));
        verify(mockMediator, times(1)).layerRenamed(eq(layerInfo1.getName()), eq("newName"));
    }

    @Test
    public void testSave() {
        // delete layer
        when(tileLayerCatalog.delete(eq(layerInfo2.getId()))).thenReturn(layerInfo2);
        config.removeLayer(layerInfo2.getName());

        // failing delete
        when(tileLayerCatalog.delete(eq(groupInfo1.getId()))).thenReturn(groupInfo1);
        config.removeLayer(groupInfo1.getName());
        doThrow(new IllegalArgumentException("failedDelete"))
                .when(tileLayerCatalog)
                .delete(eq(group1.getId()));

        // modify two layers, one will fail
        GeoServerTileLayerInfo forceState1 = TileLayerInfoUtil.loadOrCreate(layer1, defaults);
        forceState1.setName("newName");

        GeoServerTileLayerInfo forceState2 = TileLayerInfoUtil.loadOrCreate(group2, defaults);

        when(tileLayerCatalog.save(same(forceState1))).thenReturn(layerInfo1);
        config.modifyLayer(new GeoServerTileLayer(layer1, gridSetBroker, forceState1));
        config.modifyLayer(new GeoServerTileLayer(group2, gridSetBroker, forceState2));
        // make this last one fail
        doThrow(new IllegalArgumentException("failedSave"))
                .when(tileLayerCatalog)
                .save(eq(forceState2));

        GeoServerTileLayerInfo addedState1 =
                TileLayerInfoUtil.loadOrCreate(layerWithNoTileLayer, defaults);
        config.addLayer(new GeoServerTileLayer(layerWithNoTileLayer, gridSetBroker, addedState1));
        doThrow(new IllegalArgumentException("callback exception"))
                .when(mockMediator)
                .layerAdded(eq(addedState1.getName()));

        GeoServerTileLayerInfo addedState2 =
                TileLayerInfoUtil.loadOrCreate(groupWithNoTileLayer, defaults);
        config.addLayer(new GeoServerTileLayer(groupWithNoTileLayer, gridSetBroker, addedState2));

        verify(tileLayerCatalog, times(1)).delete(eq(group1.getId()));
        verify(tileLayerCatalog, times(1)).delete(eq(layer2.getId()));
        verify(tileLayerCatalog, times(1)).save(same(forceState1));
        verify(tileLayerCatalog, times(1)).save(same(forceState2));
        verify(tileLayerCatalog, times(1)).save(same(addedState1));
        verify(tileLayerCatalog, times(1)).save(same(addedState2));

        verify(mockMediator, times(1)).layerRemoved(eq(layerInfo2.getName()));
        verify(mockMediator, times(1)).layerRenamed(eq(layerInfo1.getName()), eq("newName"));
        verify(mockMediator, times(1)).layerAdded(eq(addedState1.getName()));
        verify(mockMediator, times(1)).layerAdded(eq(addedState2.getName()));
    }

    @Test
    public void testCanSave() {
        // Create mock layer not transient and ensure that the Layer cannot be saved
        GeoServerTileLayer l = mock(GeoServerTileLayer.class);
        when(l.isTransientLayer()).thenReturn(true);
        assertFalse(config.canSave(l));
    }

    @Test
    public void testNoGeometry() throws Exception {
        org.opengis.feature.type.FeatureType featureTypeWithNoGeometry =
                mock(org.opengis.feature.type.FeatureType.class);
        when(featureTypeWithNoGeometry.getGeometryDescriptor()).thenReturn(null);
        org.geoserver.catalog.FeatureTypeInfo resourceWithNoGeometry =
                mock(org.geoserver.catalog.FeatureTypeInfo.class);
        when(resourceWithNoGeometry.getFeatureType()).thenReturn(featureTypeWithNoGeometry);
        LayerInfo layerWithNoGeometry =
                mockLayer("layerWithNoGeometry", new String[] {}, PublishedType.VECTOR);
        layerWithNoGeometry.setResource(resourceWithNoGeometry);
        GeoServerTileLayer tl = mock(GeoServerTileLayer.class);
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("layerWithNoGeometry");
        info.setName("layerWithNoGeometry");
        when(tl.getId()).thenReturn("layerWithNoGeometry");
        when(tl.isTransientLayer()).thenReturn(false);
        when(tl.getInfo()).thenReturn(info);
        when(tl.getPublishedInfo()).thenReturn(layerWithNoGeometry);
        when(catalog.getLayer(layerWithNoGeometry.getId())).thenReturn(layerWithNoGeometry);
        when(catalog.getLayerByName(eq(tileLayerName(layerWithNoGeometry))))
                .thenReturn(layerWithNoGeometry);

        config.addLayer(tl);

        verify(this.tileLayerCatalog, never()).save(info);
    }

    @Test
    public void testConfigurationDeadlock() throws Exception {
        // to make it reproducible with some reliability on my machine
        // 100000 loops need to be attempted. With the fix it works, but runs for
        // a minute and a half, so not suitable for actual builds.
        // For in-build tests I've thus settled down for 1000 loops instead
        final int LOOPS = 1000;
        ExecutorService service = Executors.newFixedThreadPool(8);
        Runnable reloader =
                new Runnable() {

                    @Override
                    public void run() {
                        config.setGridSetBroker(gridSetBroker);
                        config.afterPropertiesSet();
                    }
                };
        Runnable tileLayerFetcher =
                new Runnable() {

                    @Override
                    public void run() {
                        config.getLayer(layer1.getName());
                        config.getLayer(layer2.getName());
                        config.getLayer(group1.getName());
                        config.getLayer(group2.getName());
                    }
                };
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < LOOPS; i++) {
                futures.add(service.submit(reloader));
                futures.add(service.submit(tileLayerFetcher));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            service.shutdown();
        }
    }

    @Test
    public void getLayerByIdWithLocalWorkspace() {
        try {
            // create test workspaces
            WorkspaceInfo testWorkspace = new WorkspaceInfoImpl();
            testWorkspace.setName("test");
            WorkspaceInfo otherWorkspace = new WorkspaceInfoImpl();
            otherWorkspace.setName("other");
            // setting the local workspace equal to layers workspace
            LocalWorkspace.set(testWorkspace);
            assertThat(config.getLayer(layer1.prefixedName()).orElse(null), notNullValue());
            assertThat(config.getLayer(layer2.prefixedName()).orElse(null), notNullValue());
            // setting the local workspace different from layers workspaces
            LocalWorkspace.set(otherWorkspace);
            assertThat(config.getLayer(layer1.prefixedName()).orElse(null), nullValue());
            assertThat(config.getLayer(layer2.prefixedName()).orElse(null), nullValue());
        } finally {
            // cleaning
            LocalWorkspace.set(null);
        }
    }

    @Test
    public void getLayersWithLocalWorkspace() {
        try {
            // create test workspaces
            WorkspaceInfo testWorkspace = new WorkspaceInfoImpl();
            testWorkspace.setName("test");
            WorkspaceInfo otherWorkspace = new WorkspaceInfoImpl();
            otherWorkspace.setName("other");
            // setting the local workspace equal to layers workspace
            LocalWorkspace.set(testWorkspace);
            assertThat(Iterators.size(config.getLayers().iterator()), is(2));
            // setting the local workspace different from layers workspaces
            LocalWorkspace.set(otherWorkspace);
            assertThat(Iterators.size(config.getLayers().iterator()), is(0));
        } finally {
            // cleaning
            LocalWorkspace.set(null);
        }
    }
}
