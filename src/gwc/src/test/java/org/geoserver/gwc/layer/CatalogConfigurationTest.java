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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        gridSetBroker = new GridSetBroker(true, true);

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
        assertNull(config.getServiceInformation());
        assertTrue(config.isRuntimeStatsEnabled());
    }

    @Test
    public void testInitialize() {
        assertEquals(4, config.initialize(gridSetBroker));
    }

    @Test
    public void testGetTileLayerCount() {
        assertEquals(4, config.getTileLayerCount());
    }

    @Test
    public void testGetTileLayerNames() {
        Set<String> expected =
                ImmutableSet.of(
                        tileLayerName(layer1),
                        tileLayerName(layer2),
                        tileLayerName(group1),
                        tileLayerName(group2));

        Set<String> actual = config.getTileLayerNames();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetLayers() {
        Iterable<GeoServerTileLayer> layers = config.getLayers();
        testGetLayers(layers);
    }

    @Test
    public void testDeprecatedGetTileLayers() {
        @SuppressWarnings("deprecation")
        List<GeoServerTileLayer> layers = config.getTileLayers();
        testGetLayers(layers);
    }

    private void testGetLayers(Iterable<GeoServerTileLayer> layers) {
        assertEquals(3, catalog.getLayers().size());
        assertEquals(3, catalog.getLayerGroups().size());
        assertEquals(4, Iterables.size(layers));

        Set<GeoServerTileLayerInfo> expected =
                ImmutableSet.of(layerInfo1, layerInfo2, groupInfo1, groupInfo2);
        Set<GeoServerTileLayerInfo> actual = new HashSet<GeoServerTileLayerInfo>();

        for (GeoServerTileLayer layer : layers) {
            actual.add(layer.getInfo());
        }
        assertEquals(4, actual.size());
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTileLayer() {
        String layerName = tileLayerName(layerWithNoTileLayer);
        assertNull(config.getTileLayer(layerName));
        assertNull(config.getTileLayer(tileLayerName(groupWithNoTileLayer)));

        assertNotNull(config.getTileLayer(tileLayerName(layer1)));
        assertNotNull(config.getTileLayer(tileLayerName(layer2)));
        assertNotNull(config.getTileLayer(tileLayerName(group1)));
        assertNotNull(config.getTileLayer(tileLayerName(group2)));

        assertNull(config.getTileLayer("anythingElse"));
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

        GeoServerTileLayer tileLayer1 = config.getTileLayer(tileLayerName(layer1));
        GeoServerTileLayer tileLayer2 = config.getTileLayer(tileLayerName(group1));

        testModifyLayer(tileLayer1);
        testModifyLayer(tileLayer2);
    }

    private void testModifyLayer(GeoServerTileLayer orig) {

        GeoServerTileLayerInfo newState = TileLayerInfoUtil.create(defaults);
        newState.setId(orig.getInfo().getId());
        newState.setName(orig.getInfo().getName());
        assertFalse(orig.equals(newState));

        final GeoServerTileLayer modified;
        if (orig.getLayerInfo() != null) {
            modified = new GeoServerTileLayer(orig.getLayerInfo(), gridSetBroker, newState);
        } else {
            modified = new GeoServerTileLayer(orig.getLayerGroupInfo(), gridSetBroker, newState);
        }

        assertEquals(orig.getInfo(), config.getTileLayer(orig.getName()).getInfo());

        config.modifyLayer(modified);

        assertEquals(newState, config.getTileLayer(orig.getName()).getInfo());

        final String origName = orig.getName();
        modified.getInfo().setName("changed");

        config.modifyLayer(modified);

        assertNull(config.getTileLayer(origName));
        assertFalse(config.getTileLayerNames().contains(origName));
    }

    @Test
    public void testRemoveLayer() {
        try {
            config.removeLayer(null);
            fail("expected precondition violation exception");
        } catch (RuntimeException e) {
            assertTrue(true);
        }

        assertFalse(config.removeLayer(GWC.tileLayerName(layerWithNoTileLayer)));
        assertFalse(config.removeLayer(GWC.tileLayerName(groupWithNoTileLayer)));

        String layerName;

        layerName = tileLayerName(layer1);
        assertNotNull(config.getTileLayer(layerName));

        final int initialCount = config.getTileLayerCount();

        assertTrue(config.removeLayer(layerName));

        assertNull(config.getTileLayer(layerName));
        assertFalse(config.getTileLayerNames().contains(layerName));
        assertEquals(initialCount - 1, config.getTileLayerCount());

        layerName = GWC.tileLayerName(group1);
        assertNotNull(config.getTileLayer(layerName));
        assertTrue(config.removeLayer(layerName));
        assertNull(config.getTileLayer(layerName));
        assertEquals(initialCount - 2, config.getTileLayerCount());
    }

    @Test
    public void testSaveRename() {

        GeoServerTileLayerInfo originalState = layerInfo1;

        GeoServerTileLayerInfo forceState1 = TileLayerInfoUtil.loadOrCreate(layer1, defaults);
        when(tileLayerCatalog.save(same(forceState1))).thenReturn(originalState);
        forceState1.setName("newName");

        config.modifyLayer(new GeoServerTileLayer(layer1, gridSetBroker, forceState1));

        verify(mockMediator, never()).layerRemoved(anyString());
        verify(mockMediator, never()).layerRenamed(anyString(), anyString());

        config.save();

        verify(tileLayerCatalog, times(1)).save(same(forceState1));

        // and gwc has been instructed on the changes
        verify(mockMediator, times(1)).layerRenamed(eq(layerInfo1.getName()), eq("newName"));
    }

    @Test
    public void testSave() {
        // add a pending delete
        when(tileLayerCatalog.delete(eq(layerInfo2.getId()))).thenReturn(layerInfo2);
        assertTrue(config.removeLayer(layerInfo2.getName()));

        // and a failed one at save
        when(tileLayerCatalog.delete(eq(groupInfo1.getId()))).thenReturn(groupInfo1);
        assertTrue(config.removeLayer(groupInfo1.getName()));
        doThrow(new IllegalArgumentException("failedDelete"))
                .when(tileLayerCatalog)
                .delete(eq(group1.getId()));

        // add two pending modifications
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

        verify(mockMediator, times(1)).layerRemoved(layerInfo2.getName());
        verify(mockMediator, never()).layerRenamed(anyString(), anyString());

        GeoServerTileLayerInfo addedState1 =
                TileLayerInfoUtil.loadOrCreate(layerWithNoTileLayer, defaults);
        config.addLayer(new GeoServerTileLayer(layerWithNoTileLayer, gridSetBroker, addedState1));
        doThrow(new IllegalArgumentException("callback exception"))
                .when(mockMediator)
                .layerAdded(eq(addedState1.getName()));

        GeoServerTileLayerInfo addedState2 =
                TileLayerInfoUtil.loadOrCreate(groupWithNoTileLayer, defaults);
        config.addLayer(new GeoServerTileLayer(groupWithNoTileLayer, gridSetBroker, addedState2));

        config.save();

        verify(tileLayerCatalog, times(1)).delete(eq(group1.getId()));
        verify(tileLayerCatalog, times(1)).delete(eq(layer2.getId()));
        verify(tileLayerCatalog, times(1)).save(same(forceState1));
        verify(tileLayerCatalog, times(1)).save(same(forceState2));
        verify(tileLayerCatalog, times(1)).save(same(addedState1));
        verify(tileLayerCatalog, times(1)).save(same(addedState2));

        // and gwc has been instructed on the changes
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
        when(tl.getLayerInfo()).thenReturn(layerWithNoGeometry);
        when(catalog.getLayer(layerWithNoGeometry.getId())).thenReturn(layerWithNoGeometry);
        when(catalog.getLayerByName(eq(tileLayerName(layerWithNoGeometry))))
                .thenReturn(layerWithNoGeometry);

        config.addLayer(tl);
        config.save();

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
                        config.initialize(gridSetBroker);
                    }
                };
        Runnable tileLayerFetcher =
                new Runnable() {

                    @Override
                    public void run() {
                        config.getTileLayer(layer1.getName());
                        config.getTileLayer(layer2.getName());
                        config.getTileLayer(group1.getName());
                        config.getTileLayer(group2.getName());
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
            assertThat(config.getTileLayerById(layer1.getId()), notNullValue());
            assertThat(config.getTileLayerById(layer2.getId()), notNullValue());
            // setting the local workspace different from layers workspaces
            LocalWorkspace.set(otherWorkspace);
            assertThat(config.getTileLayerById(layer1.getId()), nullValue());
            assertThat(config.getTileLayerById(layer2.getId()), nullValue());
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
