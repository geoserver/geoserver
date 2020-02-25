/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static com.google.common.collect.Iterators.forEnumeration;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.union;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.geoserver.gwc.GWC.tileLayerName;
import static org.geoserver.gwc.GWCTestHelpers.mockGroup;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.geoserver.gwc.layer.TileLayerInfoUtil.updateAcceptAllFloatParameterFilter;
import static org.geoserver.gwc.layer.TileLayerInfoUtil.updateStringParameterFilter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.layer.CatalogLayerEventListener;
import org.geoserver.gwc.layer.CatalogStyleChangeListener;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredLayerInfo;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.kvp.PaletteManager;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geowebcache.GeoWebCacheEnvironment;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.ConfigurationPersistenceException;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.config.FileBlobStoreInfo;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.config.XMLGridSet;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.mime.MimeType;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.SeedRequest;
import org.geowebcache.seed.TileBreeder;
import org.geowebcache.seed.TruncateAllRequest;
import org.geowebcache.service.Service;
import org.geowebcache.storage.BlobStoreAggregator;
import org.geowebcache.storage.CompositeBlobStore;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.springframework.context.ApplicationContext;

/** Unit test suite for the {@link GWC} mediator. */
/** @author groldan */
public class GWCTest {

    private GWC mediator;

    private GWCConfig defaults;

    private GWCConfigPersister gwcConfigPersister;

    private XMLConfiguration xmlConfig;

    private StorageBroker storageBroker;

    private GridSetBroker gridSetBroker;

    private TileLayerConfiguration config;

    private TileLayerDispatcher tld;

    private TileBreeder tileBreeder;

    private QuotaStore quotaStore;

    private DiskQuotaMonitor diskQuotaMonitor;

    private ConfigurableQuotaStoreProvider diskQuotaStoreProvider;

    private Dispatcher owsDispatcher;

    private Catalog catalog;

    LayerInfo layer;

    LayerGroupInfo layerGroup;

    GeoServerTileLayerInfo tileLayerInfo;

    GeoServerTileLayerInfo tileLayerGroupInfo;

    GeoServerTileLayer tileLayer;

    GeoServerTileLayer tileLayerGroup;

    private DefaultStorageFinder storageFinder;

    private JDBCConfigurationStorage jdbcStorage;

    static Resource tmpDir() throws IOException {
        Resource root = Files.asResource(new File(System.getProperty("java.io.tmpdir", ".")));
        Resource directory = Resources.createRandom("tmp", "", root);

        do {
            FileUtils.forceDelete(directory.dir());
        } while (Resources.exists(directory));

        FileUtils.forceMkdir(directory.dir());

        return Files.asResource(directory.dir());
    }

    @Rule public ExpectedException expected = ExpectedException.none();

    private BlobStoreAggregator blobStoreAggregator;

    @Before
    @SuppressWarnings("deprecation")
    public void setUp() throws Exception {

        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
        System.setProperty("TEST_ENV_PROPERTY", "H2");

        catalog = mock(Catalog.class);
        layer = mockLayer("testLayer", new String[] {"style1", "style2"}, PublishedType.RASTER);
        layerGroup = mockGroup("testGroup", layer);
        mockCatalog();

        defaults = GWCConfig.getOldDefaults();

        gwcConfigPersister = mock(GWCConfigPersister.class);
        when(gwcConfigPersister.getConfig()).thenReturn(defaults);

        storageBroker = mock(StorageBroker.class);

        tileLayerInfo = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        tileLayerGroupInfo = TileLayerInfoUtil.loadOrCreate(layerGroup, defaults);

        config = mock(TileLayerConfiguration.class);
        tileBreeder = mock(TileBreeder.class);
        quotaStore = mock(QuotaStore.class);
        diskQuotaMonitor = mock(DiskQuotaMonitor.class);
        when(diskQuotaMonitor.getQuotaStore()).thenReturn(quotaStore);
        owsDispatcher = mock(Dispatcher.class);
        diskQuotaStoreProvider = mock(ConfigurableQuotaStoreProvider.class);
        when(diskQuotaMonitor.getQuotaStoreProvider()).thenReturn(diskQuotaStoreProvider);

        storageFinder = mock(DefaultStorageFinder.class);
        jdbcStorage = createMock(JDBCConfigurationStorage.class);
        xmlConfig = mock(XMLConfiguration.class);
        blobStoreAggregator = mock(BlobStoreAggregator.class);

        gridSetBroker =
                new GridSetBroker(Arrays.asList(new DefaultGridsets(true, true), xmlConfig));
        tileLayer = new GeoServerTileLayer(layer, gridSetBroker, tileLayerInfo);
        GridSet testGridSet = namedGridsetCopy("TEST", gridSetBroker.getDefaults().worldEpsg4326());

        GridSubset testGridSubset =
                GridSubsetFactory.createGridSubSet(
                        testGridSet,
                        new BoundingBox(-180, 0, 0, 90),
                        0,
                        testGridSet.getNumLevels() - 1);
        when(xmlConfig.getGridSet(eq("TEST"))).thenReturn(Optional.of(testGridSet));
        tileLayer.addGridSubset(testGridSubset);
        tileLayerGroup = new GeoServerTileLayer(layerGroup, gridSetBroker, tileLayerGroupInfo);
        tileLayerGroup.addGridSubset(testGridSubset);
        tld = mock(TileLayerDispatcher.class);
        mockTileLayerDispatcher();

        GeoWebCacheEnvironment genv =
                createMockBuilder(GeoWebCacheEnvironment.class).withConstructor().createMock();

        ApplicationContext appContext = createMock(ApplicationContext.class);

        expect(appContext.getBeanNamesForType(GeoWebCacheEnvironment.class))
                .andReturn(new String[] {"geoWebCacheEnvironment"})
                .anyTimes();
        Map<String, GeoWebCacheEnvironment> genvMap = new HashMap<>();
        genvMap.put("geoWebCacheEnvironment", genv);
        expect(appContext.getBeansOfType(GeoWebCacheEnvironment.class))
                .andReturn(genvMap)
                .anyTimes();
        expect(appContext.getBean("geoWebCacheEnvironment")).andReturn(genv).anyTimes();

        expect(appContext.getBeanNamesForType(XMLConfiguration.class))
                .andReturn(new String[] {"geoWebCacheXMLConfiguration"})
                .anyTimes();
        Map<String, XMLConfiguration> xmlConfMap = new HashMap();
        xmlConfMap.put("geoWebCacheXMLConfiguration", xmlConfig);
        expect(appContext.getBeansOfType(XMLConfiguration.class)).andReturn(xmlConfMap).anyTimes();
        expect(appContext.getBean("geoWebCacheXMLConfiguration")).andReturn(xmlConfig).anyTimes();

        replay(appContext);

        GeoWebCacheExtensions gse = createMockBuilder(GeoWebCacheExtensions.class).createMock();
        gse.setApplicationContext(appContext);

        replay(gse);

        List<GeoWebCacheEnvironment> extensions =
                GeoWebCacheExtensions.extensions(GeoWebCacheEnvironment.class);
        assertNotNull(extensions);
        assertEquals(1, extensions.size());
        assertTrue(extensions.contains(genv));

        JDBCConfiguration jdbcConfiguration = new JDBCConfiguration();
        if (GeoWebCacheEnvironment.ALLOW_ENV_PARAMETRIZATION) {
            jdbcConfiguration.setDialect("${TEST_ENV_PROPERTY}");
        } else {
            jdbcConfiguration.setDialect("H2");
        }
        File jdbcConfigurationFile =
                File.createTempFile("jdbcConfigurationFile", ".tmp", tmpDir().dir());
        jdbcConfiguration.store(jdbcConfiguration, jdbcConfigurationFile);

        JDBCConfiguration loadedConf = jdbcConfiguration.load(jdbcConfigurationFile);
        jdbcStorage.setApplicationContext(appContext);

        expect(jdbcStorage.getJDBCDiskQuotaConfig()).andReturn(loadedConf).anyTimes();

        replay(jdbcStorage);

        mediator =
                new GWC(
                        gwcConfigPersister,
                        storageBroker,
                        tld,
                        gridSetBroker,
                        tileBreeder,
                        diskQuotaMonitor,
                        owsDispatcher,
                        catalog,
                        catalog,
                        storageFinder,
                        jdbcStorage,
                        blobStoreAggregator);
        mediator.setApplicationContext(appContext);

        mediator = spy(mediator);
        GWC.set(mediator);
    }

    @After
    public void tearDown() {
        System.clearProperty("ALLOW_ENV_PARAMETRIZATION");
        System.clearProperty("TEST_ENV_PROPERTY");
        GWC.set(null);
    }

    private void mockCatalog() {
        when(catalog.getLayers()).thenReturn(Arrays.asList(layer));
        when(catalog.getLayerGroups()).thenReturn(Arrays.asList(layerGroup));
        when(catalog.getLayer(eq(layer.getId()))).thenReturn(layer);
        when(catalog.getLayerGroup(layerGroup.getId())).thenReturn(layerGroup);
        when(catalog.getLayerByName(eq(layer.getResource().prefixedName()))).thenReturn(layer);
        when(catalog.getLayerGroupByName(tileLayerName(layerGroup))).thenReturn(layerGroup);
    }

    private void mockTileLayerDispatcher() throws Exception {
        when(tld.getConfiguration(same(tileLayer))).thenReturn(config);
        when(tld.getConfiguration(same(tileLayerGroup))).thenReturn(config);
        when(tld.getConfiguration(eq(tileLayer.getName()))).thenReturn(config);
        when(tld.getConfiguration(eq(tileLayerGroup.getName()))).thenReturn(config);

        when(tld.getTileLayer(eq(tileLayer.getName()))).thenReturn(tileLayer);
        when(tld.getTileLayer(eq(tileLayerGroup.getName()))).thenReturn(tileLayerGroup);

        when(tld.getLayerNames())
                .thenReturn(ImmutableSet.of(tileLayer.getName(), tileLayerGroup.getName()));
        Iterable<TileLayer> tileLayers =
                ImmutableList.of((TileLayer) tileLayer, (TileLayer) tileLayerGroup);
        when(tld.getLayerList()).thenReturn(tileLayers);

        when(tld.layerExists(eq(tileLayer.getName()))).thenReturn(true);
        when(tld.layerExists(eq(tileLayerGroup.getName()))).thenReturn(true);
    }

    @Test
    public void testAddTileLayer() throws Exception {

        doThrow(new IllegalArgumentException("fake")).when(tld).addLayer(same(tileLayer));
        doNothing().when(tld).addLayer(same(tileLayerGroup));

        try {
            mediator.add(tileLayer);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        mediator.add(tileLayerGroup);
    }

    @Test
    public void testModifyTileLayer() throws Exception {
        try {
            mediator.save(null);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        doThrow(new IllegalArgumentException()).when(tld).modify(same(tileLayer));
        try {
            mediator.save(tileLayer);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        doNothing().when(tld).modify(same(tileLayerGroup));
        mediator.save(tileLayerGroup);
        verify(tld, times(1)).modify(same(tileLayerGroup));

        doNothing().when(tld).modify(same(tileLayer));
        doThrow(new ConfigurationPersistenceException(new IOException()))
                .when(config)
                .modifyLayer(tileLayer);
        try {
            mediator.save(tileLayer);
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testRemoveTileLayers() throws Exception {
        try {
            mediator.removeTileLayers(null);
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        doNothing().when(tld).removeLayer(eq(tileLayer.getName()));
        doNothing().when(tld).removeLayer(eq(tileLayerGroup.getName()));

        List<String> layerNames = Arrays.asList(tileLayer.getName(), tileLayerGroup.getName());
        mediator.removeTileLayers(layerNames);
        verify(tld, times(1)).removeLayer(eq(tileLayer.getName()));
        verify(tld, times(1)).removeLayer(eq(tileLayerGroup.getName()));
    }

    @Test
    public void testAddGridset() throws Exception {
        try {
            mediator.addGridSet(null);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        GridSet gset = mock(GridSet.class);
        GridSet gset2 = mock(GridSet.class);
        doThrow(new IOException("fake")).when(tld).addGridSet(same(gset));
        try {
            mediator.addGridSet(gset);
            fail();
        } catch (IOException e) {
            assertEquals("fake", e.getMessage());
        }
        mediator.addGridSet(gset2);
        verify(tld, times(1)).addGridSet(same(gset2));
    }

    @Test
    public void testModifyGridsetPreconditions() throws Exception {
        GridSet oldGridset = gridSetBroker.get("EPSG:4326");
        try {
            mediator.modifyGridSet(null, oldGridset);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        try {
            mediator.modifyGridSet("oldname", null);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        when(xmlConfig.getGridSet(eq("wrongOldName"))).thenReturn(Optional.empty());
        try {
            mediator.modifyGridSet("wrongOldName", oldGridset);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
    }

    @Test
    public void testModifyGridsetNoNeedToTruncate() throws Exception {
        final String oldName = "TEST";
        final String newName = "TEST_CHANGED";

        final GridSet oldGridset = gridSetBroker.get(oldName);
        final GridSet newGridset;
        newGridset = namedGridsetCopy(newName, oldGridset);

        assertNotNull(tileLayer.getGridSubset(oldName));
        assertNotNull(tileLayerGroup.getGridSubset(oldName));

        when(xmlConfig.getGridSet(eq(newName))).thenReturn(Optional.empty());
        when(xmlConfig.canSave(eq(newGridset))).thenReturn(true);

        when(tld.getConfiguration(same(tileLayer))).thenReturn(config);
        when(tld.getConfiguration(same(tileLayerGroup))).thenReturn(config);
        mediator.modifyGridSet(oldName, newGridset);

        when(xmlConfig.getGridSet(eq(oldName))).thenReturn(Optional.empty());
        when(xmlConfig.getGridSet(eq(newName))).thenReturn(Optional.of(newGridset));

        assertNull(tileLayer.getGridSubset(oldName));
        assertNull(tileLayerGroup.getGridSubset(oldName));
        assertNotNull(tileLayer.getGridSubset(newName));
        assertNotNull(tileLayerGroup.getGridSubset(newName));

        verify(xmlConfig, times(1)).removeGridSet(eq(oldName));
        verify(xmlConfig, times(1)).addGridSet(eq(newGridset));

        assertNull(gridSetBroker.get(oldName));
        assertEquals(newGridset, gridSetBroker.get(newName));
    }

    protected GridSet namedGridsetCopy(final String newName, final GridSet oldGridset) {
        final GridSet newGridset;
        {
            XMLGridSet xmlGridSet = new XMLGridSet(oldGridset);
            xmlGridSet.setName(newName);
            newGridset = xmlGridSet.makeGridSet();
        }
        return newGridset;
    }

    @Test
    public void testModifyGridsetTruncates() throws Exception {
        final String oldName = "TEST";
        final String newName = "TEST_CHANGED";

        final GridSet oldGridset = gridSetBroker.get(oldName);
        final GridSet newGridset;
        {
            XMLGridSet xmlGridSet = new XMLGridSet(oldGridset);
            xmlGridSet.setName(newName);

            // make it so the gridset forces truncation
            xmlGridSet.setAlignTopLeft(!xmlGridSet.getAlignTopLeft());
            newGridset = xmlGridSet.makeGridSet();
        }
        when(xmlConfig.getGridSet(eq(newName))).thenReturn(Optional.empty());
        when(xmlConfig.canSave(eq(newGridset))).thenReturn(true);

        when(tld.getConfiguration(same(tileLayer))).thenReturn(config);
        when(tld.getConfiguration(same(tileLayerGroup))).thenReturn(config);

        mediator.modifyGridSet(oldName, newGridset);

        when(xmlConfig.getGridSet(eq(oldName))).thenReturn(Optional.empty());
        when(xmlConfig.getGridSet(eq(newName))).thenReturn(Optional.of(newGridset));

        verify(storageBroker, times(1)).deleteByGridSetId(eq(tileLayer.getName()), eq(oldName));
        verify(storageBroker, times(1))
                .deleteByGridSetId(eq(tileLayerGroup.getName()), eq(oldName));
    }

    @Test
    public void testRemoveGridsets() throws Exception {
        try {
            mediator.removeGridSets(null);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        {
            final GridSet oldGridset = gridSetBroker.get("TEST");
            final GridSet newGridset = this.namedGridsetCopy("My4326", oldGridset);
            when(xmlConfig.getGridSet(eq("My4326"))).thenReturn(Optional.of(newGridset));
        }

        when(tld.getConfiguration(same(tileLayer))).thenReturn(config);
        when(tld.getConfiguration(same(tileLayerGroup))).thenReturn(config);
        doNothing().when(tld).modify(same(tileLayer));
        doNothing().when(tld).modify(same(tileLayerGroup));

        mediator.removeGridSets(ImmutableSet.of("My4326", "TEST"));

        assertEquals(ImmutableSet.of("EPSG:4326", "EPSG:900913"), tileLayer.getGridSubsets());
        assertEquals(ImmutableSet.of("EPSG:4326", "EPSG:900913"), tileLayerGroup.getGridSubsets());

        verify(storageBroker, times(1)).deleteByGridSetId(eq(tileLayer.getName()), eq("TEST"));
        verify(storageBroker, times(1)).deleteByGridSetId(eq(tileLayerGroup.getName()), eq("TEST"));

        verify(storageBroker, never())
                .deleteByGridSetId(eq(tileLayer.getName()), eq("EPSG:900913"));
        verify(storageBroker, never()).deleteByGridSetId(eq(tileLayer.getName()), eq("EPSG:4326"));
        verify(storageBroker, never()).deleteByGridSetId(eq(tileLayer.getName()), eq("My4326"));
        verify(storageBroker, never())
                .deleteByGridSetId(eq(tileLayerGroup.getName()), eq("EPSG:900913"));
        verify(storageBroker, never())
                .deleteByGridSetId(eq(tileLayerGroup.getName()), eq("EPSG:4326"));
        verify(storageBroker, never())
                .deleteByGridSetId(eq(tileLayerGroup.getName()), eq("My4326"));

        verify(tld, times(1)).modify(same(tileLayer));
        verify(tld, times(1)).modify(same(tileLayerGroup));
    }

    @Test
    public void testRemoveAllLayerGridsetsDisablesLayer() throws Exception {

        when(tld.getConfiguration(same(tileLayer))).thenReturn(config);
        when(tld.getConfiguration(same(tileLayerGroup))).thenReturn(config);
        doNothing().when(tld).modify(same(tileLayer));
        doNothing().when(tld).modify(same(tileLayerGroup));

        // sanity check before modification
        assertTrue(tileLayer.getInfo().isEnabled());
        assertTrue(tileLayer.getInfo().isEnabled());

        // Defaults can't be removed from the broker so remove them from the layers first
        tileLayer.removeGridSubset("EPSG:900913");
        tileLayer.removeGridSubset("EPSG:4326");
        tileLayerGroup.removeGridSubset("EPSG:900913");
        tileLayerGroup.removeGridSubset("EPSG:4326");
        mediator.save(tileLayer);
        mediator.save(tileLayerGroup);
        mediator.removeGridSets(ImmutableSet.of("TEST"));

        verify(tld, times(2)).modify(same(tileLayer)); // all other checks are in testRemoveGridsets
        verify(tld, times(2)).modify(same(tileLayerGroup));
        verify(storageBroker, times(1)).deleteByGridSetId(eq(tileLayer.getName()), eq("TEST"));
        verify(storageBroker, times(1)).deleteByGridSetId(eq(tileLayer.getName()), eq("TEST"));

        assertTrue(tileLayer.getGridSubsets().isEmpty());
        assertTrue(tileLayerGroup.getGridSubsets().isEmpty());

        // layers ended up with no gridsubsets, shall have been disabled
        assertFalse(tileLayer.getInfo().isEnabled());
        assertFalse(tileLayerGroup.getInfo().isEnabled());
    }

    @Test
    public void testAutoConfigureLayers() throws Exception {
        {
            GWCConfig insaneDefaults = new GWCConfig();
            insaneDefaults.setMetaTilingX(-1);
            assertFalse(insaneDefaults.isSane());
            try {
                mediator.autoConfigureLayers(Arrays.asList(tileLayer.getName()), insaneDefaults);
            } catch (IllegalArgumentException e) {
                assertTrue(true);
            }
        }

        try {
            mediator.autoConfigureLayers(Arrays.asList(tileLayer.getName()), defaults);
            fail("expected IAE, layer exists");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        LayerInfo layer2 = mockLayer("layer2", new String[] {}, PublishedType.RASTER);
        LayerGroupInfo group2 = mockGroup("group2", layer, layer2);

        when(catalog.getLayerByName(eq(tileLayerName(layer2)))).thenReturn(layer2);
        when(catalog.getLayerGroupByName(eq(tileLayerName(group2)))).thenReturn(group2);

        List<String> layerNames = Arrays.asList(tileLayerName(layer2), tileLayerName(group2));

        doNothing().when(tld).addLayer(any(GeoServerTileLayer.class));
        mediator.autoConfigureLayers(layerNames, defaults);

        GeoServerTileLayerInfo expected1 =
                new GeoServerTileLayer(layer2, defaults, gridSetBroker).getInfo();
        GeoServerTileLayerInfo expected2 =
                new GeoServerTileLayer(group2, defaults, gridSetBroker).getInfo();

        ArgumentCaptor<GeoServerTileLayer> addCaptor =
                ArgumentCaptor.forClass(GeoServerTileLayer.class);

        verify(tld, times(2)).addLayer(addCaptor.capture());

        GeoServerTileLayerInfo actual1 = addCaptor.getAllValues().get(0).getInfo();
        GeoServerTileLayerInfo actual2 = addCaptor.getAllValues().get(1).getInfo();

        assertEquals(expected1, actual1);
        assertEquals(expected2, actual2);
    }

    @Test
    public void testIsInternalGridset() {

        Set<String> embeddedNames = gridSetBroker.getEmbeddedNames();
        for (String name : embeddedNames) {
            assertTrue(mediator.isInternalGridSet(name));
        }

        assertFalse(mediator.isInternalGridSet("somethingelse"));
    }

    @Test
    public void testDeleteCacheByGridSetId() throws Exception {

        when(storageBroker.deleteByGridSetId(eq("layer"), eq("gset1")))
                .thenThrow(new StorageException("fake"));

        try {
            mediator.deleteCacheByGridSetId("layer", "gset1");
            fail();
        } catch (RuntimeException e) {
            assertTrue(true);
        }
        mediator.deleteCacheByGridSetId("layer", "gset2");
        verify(storageBroker, times(1)).deleteByGridSetId(eq("layer"), eq("gset2"));
    }

    @Test
    public void testDestroy() throws Exception {

        mediator.destroy();

        ArgumentCaptor<CatalogListener> captor = ArgumentCaptor.forClass(CatalogListener.class);
        verify(catalog, times(2)).removeListener(captor.capture());
        for (CatalogListener captured : captor.getAllValues()) {
            assertTrue(
                    captured instanceof CatalogLayerEventListener
                            || captured instanceof CatalogStyleChangeListener);
        }
    }

    @Test
    public void testTruncateLayerFully() throws Exception {

        when(tld.getTileLayer(eq(tileLayerGroup.getName())))
                .thenThrow(new GeoWebCacheException("fake"));

        mediator.truncate(tileLayerGroup.getName());
        verify(storageBroker, never()).deleteByGridSetId(anyString(), anyString());

        mediator.truncate(tileLayer.getName());
        verify(storageBroker, times(tileLayer.getGridSubsets().size()))
                .deleteByGridSetId(anyString(), anyString());
    }

    @Test
    public void testTruncateByLayerAndStyle() throws Exception {

        String layerName = tileLayer.getName();
        String styleName = "notACachedStyle";

        mediator.truncateByLayerAndStyle(layerName, styleName);
        verify(tileBreeder, never()).dispatchTasks(any(GWCTask[].class));

        styleName = layer.getDefaultStyle().prefixedName();
        mediator.truncateByLayerAndStyle(layerName, styleName);

        int expected = tileLayer.getGridSubsets().size() * tileLayer.getMimeTypes().size();
        verify(tileBreeder, times(expected)).dispatchTasks(any());
    }

    @Test
    public void testTruncateByBounds() throws Exception {
        String layerName = tileLayer.getName();

        when(tileBreeder.findTileLayer(layerName)).thenReturn(tileLayer);
        final Set<Map<String, String>> cachedParameters =
                tileLayer
                        .getInfo()
                        .cachedStyles()
                        .stream()
                        .map(style -> Collections.singletonMap("STYLES", style))
                        .collect(Collectors.toSet());

        when(storageBroker.getCachedParameters(layerName)).thenReturn(cachedParameters);

        ReferencedEnvelope bounds;
        // bounds outside layer bounds (which are -180,0,0,90)
        bounds = new ReferencedEnvelope(10, 20, 10, 20, DefaultGeographicCRS.WGS84);
        BoundingBox layerBounds = tileLayer.getGridSubset("EPSG:4326").getOriginalExtent();

        assertFalse(bounds.intersects(layerBounds.getMinX(), layerBounds.getMinY()));
        assertFalse(bounds.intersects(layerBounds.getMaxX(), layerBounds.getMaxY()));

        mediator.truncate(layerName, bounds);

        verify(tileBreeder, never()).dispatchTasks(any(GWCTask[].class));

        // bounds intersecting layer bounds
        bounds = new ReferencedEnvelope(-10, -10, 10, 10, DefaultGeographicCRS.WGS84);

        mediator.truncate(layerName, bounds);

        int numGridsets = tileLayer.getGridSubsets().size();
        int numFormats = tileLayer.getMimeTypes().size();
        int numStyles = 1 /* default */ + tileLayer.getInfo().cachedStyles().size();
        final int expected = numGridsets * numFormats * numStyles;
        verify(tileBreeder, times(expected)).seed(eq(layerName), any(SeedRequest.class));

        reset(tileBreeder);
        when(tileBreeder.findTileLayer(layerName)).thenReturn(tileLayer);
        bounds = bounds.transform(CRS.decode("EPSG:900913"), true);
        mediator.truncate(layerName, bounds);
        verify(tileBreeder, times(expected)).seed(eq(layerName), any(SeedRequest.class));

        reset(tileBreeder);
        when(tileBreeder.findTileLayer(layerName)).thenReturn(tileLayer);
        bounds =
                mediator.getAreaOfValidity(
                        CRS.decode("EPSG:2083")); // Terra del Fuego Does not intersect subset
        mediator.truncate(layerName, bounds);
        verify(tileBreeder, times(0)).seed(eq(layerName), any(SeedRequest.class));

        reset(tileBreeder);
        when(tileBreeder.findTileLayer(layerName)).thenReturn(tileLayer);
        bounds = mediator.getAreaOfValidity(CRS.decode("EPSG:26986")); // Massachussets
        mediator.truncate(layerName, bounds);
        verify(tileBreeder, times(expected)).seed(eq(layerName), any(SeedRequest.class));
    }

    @Test
    public void testTruncateByBoundsWithDimension() throws Exception {
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(tileLayerInfo, "TIME", true);
        Collection<String> cachedTimes = Arrays.asList("time1", "time2");
        String layerName = tileLayer.getName();

        when(tileBreeder.findTileLayer(layerName)).thenReturn(tileLayer);
        final Set<Map<String, String>> cachedParameters =
                tileLayer
                        .getInfo()
                        .cachedStyles()
                        .stream()
                        .flatMap(
                                style ->
                                        cachedTimes
                                                .stream()
                                                .map(
                                                        time -> {
                                                            Map<String, String> map = new HashMap();
                                                            map.put("STYLE", style);
                                                            map.put("TIME", time);
                                                            return map;
                                                        }))
                        .collect(Collectors.toSet());

        when(storageBroker.getCachedParameters(layerName)).thenReturn(cachedParameters);

        ReferencedEnvelope bounds;
        // bounds outside layer bounds (which are -180,0,0,90)
        bounds = new ReferencedEnvelope(10, 20, 10, 20, DefaultGeographicCRS.WGS84);
        BoundingBox layerBounds =
                tileLayer.getGridSubset("EPSG:4326").getGridSet().getOriginalExtent();

        assertFalse(bounds.intersects(layerBounds.getMinX(), layerBounds.getMinY()));
        assertFalse(bounds.intersects(layerBounds.getMaxX(), layerBounds.getMaxY()));

        mediator.truncate(layerName, bounds);

        verify(tileBreeder, never()).dispatchTasks(any(GWCTask[].class));

        // bounds intersecting layer bounds
        bounds = new ReferencedEnvelope(-10, -10, 10, 10, DefaultGeographicCRS.WGS84);

        mediator.truncate(layerName, bounds);

        int numGridsets = tileLayer.getGridSubsets().size();
        int numFormats = tileLayer.getMimeTypes().size();
        int numStyles = tileLayer.getInfo().cachedStyles().size();
        int numTimes = cachedTimes.size();
        int numParameters = numStyles * numTimes + 1;
        final int expected = numGridsets * numFormats * numParameters;
        verify(tileBreeder, times(expected)).seed(eq(layerName), any(SeedRequest.class));
    }

    @Test
    public void testTruncateAll() throws Exception {
        String layerName = tileLayer.getName();
        when(tileBreeder.findTileLayer(layerName)).thenReturn(tileLayer);

        ArrayList<TileLayer> mockList = new ArrayList<TileLayer>();
        mockList.add(tileLayer);
        when(tileBreeder.getLayers()).thenReturn(mockList);
        for (String grid : tileLayer.getGridSubsets())
            when(storageBroker.deleteByGridSetId(layerName, grid)).thenReturn(true);

        TruncateAllRequest truncateAll = mediator.truncateAll();

        verify(tileBreeder, times(1)).getLayers();
        assertTrue(truncateAll.getTrucatedLayersList().contains(layerName));
    }

    @Test
    public void testLayerRemoved() throws Exception {
        mediator.layerRemoved("someLayer");
        verify(storageBroker, times(1)).delete(eq("someLayer"));

        doThrow(new StorageException("fake")).when(storageBroker).delete(eq("anotherLayer"));
        try {
            mediator.layerRemoved("anotherLayer");
            fail("Expected RTE");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof StorageException);
        }
    }

    @Test
    public void testLayerAdded() throws Exception {

        when(diskQuotaMonitor.isEnabled()).thenReturn(false);
        mediator.layerAdded("someLayer");
        verify(quotaStore, never()).createLayer(anyString());

        when(diskQuotaMonitor.isEnabled()).thenReturn(true);

        mediator.layerAdded("someLayer");
        verify(quotaStore, times(1)).createLayer(eq("someLayer"));

        doThrow(new InterruptedException("fake")).when(quotaStore).createLayer(eq("someLayer"));
        try {
            mediator.layerAdded("someLayer");
            fail("Expected RTE");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof InterruptedException);
        }
    }

    @Test
    public void testLayerRenamed() throws Exception {

        mediator.layerRenamed("old", "new");
        verify(storageBroker, times(1)).rename(eq("old"), eq("new"));

        doThrow(new StorageException("target directory already exists"))
                .when(storageBroker)
                .rename(eq("old"), eq("new"));
        try {
            mediator.layerRenamed("old", "new");
            fail("Expected RTE");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof StorageException);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testReload() throws Exception {
        mediator.reload();
        verify(tld, times(1)).reInit();
        verify(diskQuotaStoreProvider, times(1)).reloadQuotaStore();
        RuntimeException expected = new RuntimeException("expected");
        doThrow(expected).when(tld).reInit();
        try {
            mediator.reload();
            fail("Expected RTE");
        } catch (RuntimeException e) {
            assertSame(expected, e);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testReloadAndLayerRemovedExternally() throws Exception {

        final String removedLayer = tileLayer.getName();
        final String remainingLayer = tileLayerGroup.getName();

        final Set<String> layerNames = Sets.newHashSet(removedLayer, remainingLayer);

        when(tld.getLayerNames()).thenReturn(layerNames);
        doAnswer(
                        new Answer<Void>() {

                            @Override
                            public Void answer(InvocationOnMock invocation) throws Throwable {
                                layerNames.remove(removedLayer);
                                return null;
                            }
                        })
                .when(tld)
                .reInit();

        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);

        doReturn(true).when(mediator).layerRemoved(argCaptor.capture());

        mediator.reload();

        verify(tld, times(1)).reInit();
        assertEquals(1, argCaptor.getAllValues().size());
        assertEquals(removedLayer, argCaptor.getValue());
    }

    @Test
    public void testIsServiceEnabled() {
        Service service = mock(Service.class);

        when(service.getPathName()).thenReturn("wms");
        defaults.setWMSCEnabled(true);
        assertTrue(mediator.isServiceEnabled(service));
        defaults.setWMSCEnabled(false);
        assertFalse(mediator.isServiceEnabled(service));

        when(service.getPathName()).thenReturn("tms");
        defaults.setTMSEnabled(true);
        assertTrue(mediator.isServiceEnabled(service));
        defaults.setTMSEnabled(false);
        assertFalse(mediator.isServiceEnabled(service));

        when(service.getPathName()).thenReturn("somethingElse");
        assertTrue(mediator.isServiceEnabled(service));
    }

    @Test
    public void testDispatchGetMapDoesntMatchTileCache() throws Exception {
        GetMapRequest request = new GetMapRequest();

        @SuppressWarnings("unchecked")
        Map<String, String> rawKvp = new CaseInsensitiveMap(new HashMap<String, String>());
        request.setRawKvp(rawKvp);

        rawKvp.put("layers", "more,than,one,layer");
        assertDispatchMismatch(request, "more than one layer requested");

        rawKvp.put("layers", "SomeNonCachedLayer");
        when(tld.getTileLayer(eq("SomeNonCachedLayer")))
                .thenThrow(new GeoWebCacheException("layer not found"));
        assertDispatchMismatch(request, "not a tile layer");

        rawKvp.put("layers", tileLayer.getName());

        request.setFormat("badFormat");
        assertDispatchMismatch(request, "not a GWC supported format");

        request.setFormat("image/gif");
        assertDispatchMismatch(request, "no tile cache for requested format");
        request.setFormat(tileLayer.getMimeTypes().get(0).getMimeType());

        request.setSRS("EPSG:4326");
        request.setBbox(new Envelope(10, 10, 20, 20));
        assertDispatchMismatch(request, "request does not align to grid");

        request.setSRS("EPSG:23036");
        assertDispatchMismatch(request, "no cache exists for requested CRS");

        request.setSRS("badCRS");
        assertDispatchMismatch(request, "exception occurred");
        request.setSRS("EPSG:4326");

        request.setWidth(128);
        request.setHeight(256);
        assertDispatchMismatch(request, "request does not align to grid");

        request.setWidth(256);
        request.setHeight(128);
        assertDispatchMismatch(request, "request does not align to grid");

        request.setSRS("EPSG:4326");
        request.setWidth(256);
        request.setHeight(256);
        assertDispatchMismatch(request, "request does not align to grid");
    }

    @Test
    public void testDispatchGetMapNonMatchingParameterFilter() throws Exception {
        GetMapRequest request = new GetMapRequest();

        @SuppressWarnings("unchecked")
        Map<String, String> rawKvp = new CaseInsensitiveMap(new HashMap<String, String>());
        request.setRawKvp(rawKvp);

        rawKvp.put("layers", tileLayer.getName());
        tileLayer.setEnabled(false);
        assertDispatchMismatch(request, "tile layer disabled");

        tileLayer.setEnabled(true);
        assertTrue(layer.enabled());

        request.setRemoteOwsURL(new URL("http://example.com"));
        assertDispatchMismatch(request, "remote OWS");
        request.setRemoteOwsURL(null);

        request.setRemoteOwsType("WFS");
        assertDispatchMismatch(request, "remote OWS");
        request.setRemoteOwsType(null);

        request.setEnv(ImmutableMap.of("envVar", "envValue"));
        assertDispatchMismatch(request, "no parameter filter exists for ENV");
        request.setEnv(null);

        request.setFormatOptions(ImmutableMap.of("optKey", "optVal"));
        assertDispatchMismatch(request, "no parameter filter exists for FORMAT_OPTIONS");
        request.setFormatOptions(null);

        request.setAngle(45);
        assertDispatchMismatch(request, "no parameter filter exists for ANGLE");
        request.setAngle(0);

        rawKvp.put("BGCOLOR", "0xAA0000");
        assertDispatchMismatch(request, "no parameter filter exists for BGCOLOR");
        rawKvp.remove("BGCOLOR");

        request.setBuffer(10);
        assertDispatchMismatch(request, "no parameter filter exists for BUFFER");
        request.setBuffer(0);

        request.setCQLFilter(Arrays.asList(CQL.toFilter("ATT = 1")));
        assertDispatchMismatch(request, "no parameter filter exists for CQL_FILTER");
        request.setCQLFilter(null);

        request.setElevation(10D);
        assertDispatchMismatch(request, "no parameter filter exists for ELEVATION");
        request.setElevation(Collections.emptyList());

        request.setFeatureId(Arrays.asList(new FeatureIdImpl("someid")));
        assertDispatchMismatch(request, "no parameter filter exists for FEATUREID");
        request.setFeatureId(null);

        request.setFilter(Arrays.asList(CQL.toFilter("ATT = 1")));
        assertDispatchMismatch(request, "no parameter filter exists for FILTER");
        request.setFilter(null);

        request.setPalette(PaletteManager.getPalette("SAFE"));
        assertDispatchMismatch(request, "no parameter filter exists for PALETTE");
        request.setPalette(null);

        request.setStartIndex(10);
        assertDispatchMismatch(request, "no parameter filter exists for STARTINDEX");
        request.setStartIndex(null);

        request.setMaxFeatures(1);
        assertDispatchMismatch(request, "no parameter filter exists for MAXFEATURES");
        request.setMaxFeatures(null);

        request.setTime(Arrays.asList((Object) 1, (Object) 2));
        assertDispatchMismatch(request, "no parameter filter exists for TIME");
        request.setTime(Collections.emptyList());

        List<Map<String, String>> viewParams =
                ImmutableList.of((Map<String, String>) ImmutableMap.of("paramKey", "paramVal"));
        request.setViewParams(viewParams);
        assertDispatchMismatch(request, "no parameter filter exists for VIEWPARAMS");
        request.setViewParams(null);

        request.setFeatureVersion("@version");
        assertDispatchMismatch(request, "no parameter filter exists for FEATUREVERSION");
        request.setFeatureVersion(null);
    }

    /** See GEOS-5003 */
    @Test
    public void testNullsInDimensionAndTimeParameters() throws Exception {
        TileLayerInfoUtil.updateAcceptAllFloatParameterFilter(tileLayerInfo, "ELEVATION", true);
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(tileLayerInfo, "TIME", true);
        tileLayer = new GeoServerTileLayer(layer, gridSetBroker, tileLayerInfo);

        GetMapRequest request = new GetMapRequest();
        @SuppressWarnings("unchecked")
        Map<String, String> rawKvp = new CaseInsensitiveMap(new HashMap<String, String>());
        request.setRawKvp(rawKvp);

        StringBuilder target = new StringBuilder();

        boolean cachingPossible;

        request.setElevation(Arrays.asList((Object) null));
        cachingPossible = mediator.isCachingPossible(tileLayer, request, target);
        assertTrue(cachingPossible);
        assertEquals(0, target.length());
        request.setElevation(Collections.emptyList());

        request.setTime(Arrays.asList((Object) null));
        cachingPossible = mediator.isCachingPossible(tileLayer, request, target);
        assertTrue(cachingPossible);
        assertEquals(0, target.length());
    }

    /**
     * Since GeoServer sets a new FILTER parameter equal to an input CQL_FILTER parameter (if
     * present) for each WMS requests (using direct WMS integration), this may result in a caching
     * error. This test ensures that no error is thrown and caching is allowed.
     */
    @Test
    public void testCQLFILTERParameters() throws Exception {
        // Define a CQL_FILTER
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(tileLayerInfo, "CQL_FILTER", true);
        tileLayer = new GeoServerTileLayer(layer, gridSetBroker, tileLayerInfo);
        // Create the new GetMapRequest
        GetMapRequest request = new GetMapRequest();
        @SuppressWarnings("unchecked")
        Map<String, String> rawKvp = new CaseInsensitiveMap(new HashMap<String, String>());
        rawKvp.put("CQL_FILTER", "include");
        request.setRawKvp(rawKvp);
        StringBuilder target = new StringBuilder();

        // Setting CQL FILTER
        List<Filter> cqlFilters = Arrays.asList(CQL.toFilter("include"));
        request.setCQLFilter(cqlFilters);
        // Checking if caching is possible
        assertTrue(mediator.isCachingPossible(tileLayer, request, target));
        // Ensure No error is logged
        assertEquals(0, target.length());

        // Setting FILTER parameter equal to CQL FILTER (GeoServer does it internally)
        request.setFilter(cqlFilters);
        // Checking if caching is possible
        assertTrue(mediator.isCachingPossible(tileLayer, request, target));
        // Ensure No error is logged
        assertEquals(0, target.length());

        // Ensure that if another filter is set an error is thrown
        List filters = new ArrayList(cqlFilters);
        filters.add(Filter.INCLUDE);
        request.setFilter(filters);

        // Ensuring caching is not possible
        assertFalse(mediator.isCachingPossible(tileLayer, request, target));
        // Ensure No error is logged
        assertFalse(0 == target.length());
    }

    private void assertDispatchMismatch(GetMapRequest request, String expectedReason) {

        StringBuilder target = new StringBuilder();
        assertNull(mediator.dispatch(request, target));
        assertTrue(
                "mismatch reason '" + target + "' does not contain '" + expectedReason + "'",
                target.toString().contains(expectedReason));
    }

    @Test
    public void testDispatchGetMapMultipleCrsMatchingGridSubsets() throws Exception {

        testMultipleCrsMatchingGridSubsets("EPSG:4326", "EPSG:4326", new long[] {1, 1, 1});
        testMultipleCrsMatchingGridSubsets("EPSG:4326", "EPSG:4326", new long[] {10, 10, 10});

        testMultipleCrsMatchingGridSubsets("EPSG:4326", "GlobalCRS84Scale", new long[] {1, 1, 1});
        testMultipleCrsMatchingGridSubsets(
                "EPSG:4326", "GlobalCRS84Scale", new long[] {10, 10, 10});

        testMultipleCrsMatchingGridSubsets("EPSG:4326", "GlobalCRS84Scale", new long[] {1, 1, 1});
        testMultipleCrsMatchingGridSubsets(
                "EPSG:4326", "GlobalCRS84Scale", new long[] {10, 10, 10});
    }

    private void testMultipleCrsMatchingGridSubsets(
            final String srs, final String expectedGridset, long[] tileIndex) throws Exception {
        GetMapRequest request = new GetMapRequest();

        @SuppressWarnings("unchecked")
        Map<String, String> rawKvp = new CaseInsensitiveMap(new HashMap<String, String>());
        request.setRawKvp(rawKvp);
        request.setFormat("image/png");

        request.setSRS(srs);

        request.setWidth(256);
        request.setHeight(256);
        rawKvp.put("layers", "mockLayer");
        List<String> gridSetNames =
                Arrays.asList("GlobalCRS84Pixel", "GlobalCRS84Scale", "EPSG:4326");
        tileLayer = mockTileLayer("mockLayer", gridSetNames);

        // make the request match a tile in the expected gridset
        BoundingBox bounds;
        bounds = tileLayer.getGridSubset(expectedGridset).boundsFromIndex(tileIndex);

        Envelope reqBbox =
                new Envelope(
                        bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY());
        request.setBbox(reqBbox);

        ArgumentCaptor<ConveyorTile> captor = ArgumentCaptor.forClass(ConveyorTile.class);
        StringBuilder errors = new StringBuilder();

        mediator.dispatch(request, errors);

        assertTrue(errors.toString(), errors.length() == 0);

        verify(tileLayer, times(1)).getTile(captor.capture());

        ConveyorTile tileRequest = captor.getValue();

        assertEquals(expectedGridset, tileRequest.getGridSetId());
        assertEquals("image/png", tileRequest.getMimeType().getMimeType());
        assertTrue(
                "Expected "
                        + Arrays.toString(tileIndex)
                        + " got "
                        + Arrays.toString(tileRequest.getTileIndex()),
                Arrays.equals(tileIndex, tileRequest.getTileIndex()));
    }

    private GeoServerTileLayer mockTileLayer(String layerName, List<String> gridSetNames)
            throws Exception {

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tld.layerExists(eq(layerName))).thenReturn(true);
        when(tld.getTileLayer(eq(layerName))).thenReturn(tileLayer);
        when(tileLayer.getName()).thenReturn(layerName);
        when(tileLayer.isEnabled()).thenReturn(true);

        final MimeType mimeType1 = MimeType.createFromFormat("image/png");
        final MimeType mimeType2 = MimeType.createFromFormat("image/jpeg");
        when(tileLayer.getMimeTypes()).thenReturn(ImmutableList.of(mimeType1, mimeType2));

        Map<String, GridSubset> subsets = Maps.newHashMap();
        Multimap<SRS, GridSubset> bySrs = LinkedHashMultimap.create();

        GridSetBroker broker = gridSetBroker;

        for (String gsetName : gridSetNames) {
            GridSet gridSet = broker.get(gsetName);
            XMLGridSubset xmlGridSubset = new XMLGridSubset();
            String gridSetName = gridSet.getName();
            xmlGridSubset.setGridSetName(gridSetName);
            GridSubset gridSubSet = xmlGridSubset.getGridSubSet(broker);
            subsets.put(gsetName, gridSubSet);
            bySrs.put(gridSet.getSrs(), gridSubSet);

            when(tileLayer.getGridSubset(eq(gsetName))).thenReturn(gridSubSet);
        }
        for (SRS srs : bySrs.keySet()) {
            List<GridSubset> list = ImmutableList.copyOf(bySrs.get(srs));
            when(tileLayer.getGridSubsetsForSRS(eq(srs))).thenReturn(list);
        }
        when(tileLayer.getGridSubsets()).thenReturn(subsets.keySet());

        // sanity check
        for (String gsetName : gridSetNames) {
            assertTrue(tileLayer.getGridSubsets().contains(gsetName));
            assertNotNull(tileLayer.getGridSubset(gsetName));
        }

        return tileLayer;
    }

    @Test
    public void testDispatchGetMapWithMatchingParameterFilters() throws Exception {
        GetMapRequest request = new GetMapRequest();

        @SuppressWarnings("unchecked")
        Map<String, String> rawKvp = new CaseInsensitiveMap(new HashMap<String, String>());
        request.setRawKvp(rawKvp);
        request.setFormat("image/png");
        request.setSRS("EPSG:900913");
        request.setWidth(256);
        request.setHeight(256);
        rawKvp.put("layers", tileLayer.getName());

        // tileLayer = mockTileLayer("mockLayer", ImmutableList.of("EPSG:900913", "EPSG:4326"));

        // make the request match a tile in the expected gridset
        BoundingBox bounds;
        bounds = tileLayer.getGridSubset("EPSG:900913").boundsFromIndex(new long[] {0, 0, 1});

        Envelope reqBbox =
                new Envelope(
                        bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY());
        request.setBbox(reqBbox);

        assertTrue(tileLayer.getInfo().cachedStyles().size() > 0);

        for (String style : tileLayer.getInfo().cachedStyles()) {

            if (style != null && style.equals("default")) {
                // if no styles are provided WMTS assumes a default style named default
                continue;
            }

            String rawKvpParamName = "styles";
            String rawKvpParamValue = style;

            testParameterFilter(request, rawKvp, rawKvpParamName, rawKvpParamValue);
        }

        request.setEnv(ImmutableMap.of("envKey", "envValue"));
        updateStringParameterFilter(
                tileLayerInfo, "ENV", true, "def:devVal", "envKey:envValue", "envKey2:envValue2");
        testParameterFilter(request, rawKvp, "env", "envKey:envValue");

        updateAcceptAllFloatParameterFilter(tileLayerInfo, "ANGLE", true);
        request.setAngle(60);
        testParameterFilter(request, rawKvp, "angle", "60.0");

        request.setAngle(61.1);
        testParameterFilter(request, rawKvp, "angle", "61.1");
    }

    private void testParameterFilter(
            GetMapRequest request,
            Map<String, String> rawKvp,
            String rawKvpParamName,
            String rawKvpParamValue) {

        // set up raw kvp
        rawKvp.put(rawKvpParamName, rawKvpParamValue);

        StringBuilder errors = new StringBuilder();
        ConveyorTile tileRequest = mediator.prepareRequest(tileLayer, request, errors);
        assertTrue(errors.toString(), errors.length() == 0);

        Map<String, String> fullParameters = tileRequest.getFilteringParameters();
        assertEquals(
                fullParameters.toString(),
                rawKvpParamValue,
                fullParameters.get(rawKvpParamName.toUpperCase()));
    }

    @Test
    public void testGetDefaultAdvertisedCachedFormats() {
        // from src/main/resources/org/geoserver/gwc/advertised_formats.properties
        Set<String> defaultFormats =
                ImmutableSet.of(
                        "image/png",
                        "image/png8",
                        "image/jpeg",
                        "image/gif",
                        "image/vnd.jpeg-png",
                        "image/vnd.jpeg-png8");

        SetView<String> formatsWithUtfGrid =
                union(defaultFormats, Collections.singleton("application/json;type=utfgrid"));
        assertEquals(formatsWithUtfGrid, GWC.getAdvertisedCachedFormats(PublishedType.VECTOR));
        assertEquals(formatsWithUtfGrid, GWC.getAdvertisedCachedFormats(PublishedType.REMOTE));

        assertEquals(defaultFormats, GWC.getAdvertisedCachedFormats(PublishedType.RASTER));
        assertEquals(defaultFormats, GWC.getAdvertisedCachedFormats(PublishedType.WMS));

        assertEquals(formatsWithUtfGrid, GWC.getAdvertisedCachedFormats(PublishedType.GROUP));
    }

    @Test
    public void testGetPluggabledAdvertisedCachedFormats() throws IOException {
        List<URL> urls;
        try {
            // load the default and test resources separately so they are named differently and we
            // don't get the ones for testing listed in the UI when running from eclipse
            String defaultResource = "org/geoserver/gwc/advertised_formats.properties";
            String testResource = "org/geoserver/gwc/advertised_formats_unittesting.properties";
            ClassLoader classLoader = GWC.class.getClassLoader();
            urls = newArrayList(forEnumeration(classLoader.getResources(defaultResource)));
            urls.addAll(newArrayList(forEnumeration(classLoader.getResources(testResource))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // from src/main/resources/org/geoserver/gwc/advertised_formats.properties
        Set<String> defaultFormats =
                ImmutableSet.of(
                        "image/png",
                        "image/png8",
                        "image/jpeg",
                        "image/gif",
                        "image/vnd.jpeg-png",
                        "image/vnd.jpeg-png8");

        // see src/test/resources/org/geoserver/gwc/advertised_formats.properties
        Set<String> expectedVector =
                union(
                        defaultFormats,
                        ImmutableSet.of(
                                "test/vector1", "test/vector2", "application/json;type=utfgrid"));
        Set<String> expectedRaster =
                union(defaultFormats, ImmutableSet.of("test/raster1", "test/raster2;type=test"));
        Set<String> expectedGroup =
                union(
                        defaultFormats,
                        ImmutableSet.of(
                                "test/group1", "test/group2", "application/json;type=utfgrid"));

        assertEquals(expectedVector, GWC.getAdvertisedCachedFormats(PublishedType.VECTOR, urls));
        assertEquals(expectedVector, GWC.getAdvertisedCachedFormats(PublishedType.REMOTE, urls));

        assertEquals(expectedRaster, GWC.getAdvertisedCachedFormats(PublishedType.RASTER, urls));
        assertEquals(expectedRaster, GWC.getAdvertisedCachedFormats(PublishedType.WMS, urls));

        assertEquals(expectedGroup, GWC.getAdvertisedCachedFormats(PublishedType.GROUP, urls));
    }

    @Test
    public void testSetBlobStoresNull() throws ConfigurationException {
        expected.expect(NullPointerException.class);
        expected.expectMessage("stores is null");
        mediator.setBlobStores(null);
    }

    @Test
    public void testSetBlobStoresSavesConfig() throws Exception {
        when(xmlConfig.getBlobStores()).thenReturn(ImmutableList.<BlobStoreInfo>of());
        CompositeBlobStore composite = mock(CompositeBlobStore.class);
        doReturn(composite).when(mediator).getCompositeBlobStore();

        List<BlobStoreInfo> configList =
                Lists.newArrayList(mock(BlobStoreInfo.class), mock(BlobStoreInfo.class));
        when(configList.get(0).getName()).thenReturn("store0");
        when(configList.get(1).getName()).thenReturn("store1");
        when(blobStoreAggregator.getBlobStores()).thenReturn(configList);
        when(blobStoreAggregator.getBlobStoreNames()).thenReturn(Arrays.asList("store0", "store1"));

        BlobStoreInfo config = new FileBlobStoreInfo("TestBlobStore");
        List<BlobStoreInfo> newStores = ImmutableList.<BlobStoreInfo>of(config);
        mediator.setBlobStores(newStores);

        verify(blobStoreAggregator, times(1)).removeBlobStore(eq(configList.get(0).getName()));
        verify(blobStoreAggregator, times(1)).removeBlobStore(eq(configList.get(1).getName()));
        verify(blobStoreAggregator, times(1)).addBlobStore(eq(config));
    }

    @Test
    public void testSetBlobStoresRestoresRuntimeStoresOnSaveFailure() throws Exception {
        when(blobStoreAggregator.getBlobStores()).thenReturn(ImmutableList.<BlobStoreInfo>of());
        CompositeBlobStore composite = mock(CompositeBlobStore.class);
        doReturn(composite).when(mediator).getCompositeBlobStore();

        BlobStoreInfo config = new FileBlobStoreInfo("TestStore");

        doThrow(new ConfigurationPersistenceException(new IOException("expected")))
                .when(blobStoreAggregator)
                .addBlobStore(config);

        List<BlobStoreInfo> oldStores =
                Lists.newArrayList(mock(BlobStoreInfo.class), mock(BlobStoreInfo.class));

        when(blobStoreAggregator.getBlobStores()).thenReturn(oldStores);

        List<BlobStoreInfo> newStores = ImmutableList.<BlobStoreInfo>of(config);
        try {
            mediator.setBlobStores(newStores);
            fail("Expected ConfigurationException");
        } catch (ConfigurationException e) {
            assertTrue(e.getMessage().contains("Error saving config"));
        }
    }

    @Test
    public void testGeoServerEnvParametrization() throws Exception {
        if (GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
            assertTrue("H2".equals(jdbcStorage.getJDBCDiskQuotaConfig().clone(true).getDialect()));
        }
    }

    private void mockCachedSecureLayer(
            Envelope filterBox,
            BoundingBox bounds,
            final String layerName,
            final String gridName,
            final long zoom,
            final long col,
            final long row)
            throws GeoWebCacheException, NoSuchAuthorityCodeException, FactoryException {
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        GridSubset subset = mock(GridSubset.class);
        SRS srs = mock(SRS.class);
        SecuredLayerInfo layer = mock(SecuredLayerInfo.class);
        FeatureTypeInfo featureType = mock(FeatureTypeInfo.class);
        CoordinateReferenceSystem crs = mock(CoordinateReferenceSystem.class);
        CoordinateSystem cs = mock(CoordinateSystem.class);
        WrapperPolicy policy = mock(WrapperPolicy.class);
        CoverageAccessLimits limits = mock(CoverageAccessLimits.class);
        MultiPolygon filter = mock(MultiPolygon.class);

        when(tld.getTileLayer(eq(layerName))).thenReturn(tileLayer);
        when(tileLayer.getGridSubset(eq(gridName))).thenReturn(subset);
        when(subset.boundsFromIndex(eq(new long[] {col, row, zoom}))).thenReturn(bounds);
        when(subset.getSRS()).thenReturn(srs);
        doReturn(crs).when(mediator).getCRSForGridset(eq(subset));
        when(tileLayer.getPublishedInfo()).thenReturn(layer);
        when(layer.getResource()).thenReturn(featureType);
        when(featureType.getCRS()).thenReturn(crs);
        when(crs.getCoordinateSystem()).thenReturn(cs);
        when(cs.getDimension()).thenReturn(2);
        when(catalog.getLayerByName(layerName)).thenReturn(layer);
        when(layer.getWrapperPolicy()).thenReturn(policy);
        when(policy.getLimits()).thenReturn(limits);
        when(limits.getRasterFilter()).thenReturn(filter);
        when(filter.getEnvelopeInternal()).thenReturn(filterBox);
    }
}
