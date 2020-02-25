/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static org.geoserver.gwc.GWCTestHelpers.mockGroup;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.LegacyTileLayerInfoLoader;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.gwc.wmts.WMTSInfoImpl;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GWCInitializerTest {

    private GWCInitializer initializer;

    private GWCConfigPersister configPersister;

    private GeoServer geoServer;

    private Catalog rawCatalog;

    private TileLayerCatalog tileLayerCatalog;

    private GeoServerFacade geoServerFacade;

    private WMTSInfo wmtsInfo = new WMTSInfoImpl();

    @Before
    public void setUp() throws Exception {

        configPersister = mock(GWCConfigPersister.class);
        GWCConfig config = GWCConfig.getOldDefaults();
        config.setWMTSEnabled(false);
        when(configPersister.getConfig()).thenReturn(config);

        rawCatalog = mock(Catalog.class);
        tileLayerCatalog = mock(TileLayerCatalog.class);
        initializer = new GWCInitializer(configPersister, rawCatalog, tileLayerCatalog);

        wmtsInfo.setEnabled(true);
        geoServerFacade = mock(GeoServerFacade.class);
        when(geoServerFacade.getService(WMTSInfo.class)).thenReturn(wmtsInfo);

        geoServer = mock(GeoServer.class);
        when(geoServer.getFacade()).thenReturn(geoServerFacade);
    }

    @Test
    public void testInitializeLayersToOldDefaults() throws Exception {
        // no gwc-gs.xml exists
        when(configPersister.findConfigFile()).thenReturn(null);
        // ignore the upgrade of the direct wms integration flag on this test
        when(geoServer.getService(eq(WMSInfo.class))).thenReturn(null);

        // let the catalog have something to initialize
        LayerInfo layer = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        LayerGroupInfo group = mockGroup("testGroup", layer);
        when(rawCatalog.getLayers()).thenReturn(Lists.newArrayList(layer));
        when(rawCatalog.getLayerGroups()).thenReturn(Lists.newArrayList(group));

        // run layer initialization
        initializer.initialize(geoServer);

        // make sure default tile layers were created
        GWCConfig oldDefaults = GWCConfig.getOldDefaults();
        GeoServerTileLayerInfo tileLayer = TileLayerInfoUtil.loadOrCreate(layer, oldDefaults);
        GeoServerTileLayerInfo tileLayerGroup = TileLayerInfoUtil.loadOrCreate(group, oldDefaults);

        verify(tileLayerCatalog, times(1)).save(eq(tileLayer));
        verify(tileLayerCatalog, times(1)).save(eq(tileLayerGroup));
    }

    @Test
    public void testUpgradeDirectWMSIntegrationFlag() throws Exception {
        // no gwc-gs.xml exists, so that initialization runs
        when(configPersister.findConfigFile()).thenReturn(null);

        // no catalog layers for this test
        List<LayerInfo> layers = ImmutableList.of();
        List<LayerGroupInfo> groups = ImmutableList.of();
        when(rawCatalog.getLayers()).thenReturn(layers);
        when(rawCatalog.getLayerGroups()).thenReturn(groups);

        WMSInfoImpl wmsInfo = new WMSInfoImpl();
        // initialize wmsInfo with a value for the old direct wms integration flag
        wmsInfo.getMetadata().put(GWCInitializer.WMS_INTEGRATION_ENABLED_KEY, Boolean.TRUE);

        // make sure WMSInfo exists
        when(geoServer.getService(eq(WMSInfo.class))).thenReturn(wmsInfo);

        ArgumentCaptor<GWCConfig> captor = ArgumentCaptor.forClass(GWCConfig.class);
        // run layer initialization
        initializer.initialize(geoServer);

        verify(configPersister, times(3)).save(captor.capture());
        assertTrue(captor.getAllValues().get(0).isDirectWMSIntegrationEnabled());

        assertFalse(wmsInfo.getMetadata().containsKey(GWCInitializer.WMS_INTEGRATION_ENABLED_KEY));
        verify(geoServer).save(same(wmsInfo));
    }

    @Test
    public void testUpgradeFromTileLayerInfosToTileLayerCatalog() throws Exception {
        // do have gwc-gs.xml, so it doesn't go through the createDefaultTileLayerInfos path
        Resource fakeConfig = Files.asResource(new File("target", "gwc-gs.xml"));
        when(configPersister.findConfigFile()).thenReturn(fakeConfig);

        GWCConfig defaults = GWCConfig.getOldDefaults();
        defaults.setCacheLayersByDefault(true);
        when(configPersister.getConfig()).thenReturn(defaults);

        // let the catalog have something to initialize
        LayerInfo layer = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        LayerGroupInfo group = mockGroup("testGroup", layer);
        when(rawCatalog.getLayers()).thenReturn(Lists.newArrayList(layer));
        when(rawCatalog.getLayerGroups()).thenReturn(Lists.newArrayList(group));

        GeoServerTileLayerInfoImpl layerInfo = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        GeoServerTileLayerInfoImpl groupInfo = TileLayerInfoUtil.loadOrCreate(group, defaults);

        LegacyTileLayerInfoLoader.save(layerInfo, layer.getMetadata());
        LegacyTileLayerInfoLoader.save(groupInfo, group.getMetadata());

        // run layer initialization
        initializer.initialize(geoServer);

        verify(tileLayerCatalog, times(1)).save(eq(layerInfo));
        assertFalse(LegacyTileLayerInfoLoader.hasTileLayerDef(layer.getMetadata()));
        verify(rawCatalog, times(1)).save(eq(layer));

        verify(tileLayerCatalog, times(1)).save(eq(groupInfo));
        assertFalse(LegacyTileLayerInfoLoader.hasTileLayerDef(group.getMetadata()));
        verify(rawCatalog, times(1)).save(eq(group));
    }

    @Test
    public void testUpgradeWithWmtsEnablingInfo() throws Exception {
        // force configuration initialisation
        when(configPersister.findConfigFile()).thenReturn(null);
        assertTrue(wmtsInfo.isEnabled());
        // run layer initialization
        initializer.initialize(geoServer);
        // checking that the configuration was saved
        verify(geoServer).save(same(wmtsInfo));
        verify(configPersister, times(2)).save(configPersister.getConfig());
        // checking that the service info have been updated with gwc configuration value
        assertFalse(wmtsInfo.isEnabled());
    }
}
