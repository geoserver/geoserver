/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;
import static org.geoserver.gwc.GWCTestHelpers.mockGroup;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableSet;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.junit.Before;
import org.junit.Test;

public class LegacyTileLayerInfoLoaderTest {

    private GWCConfig defaults;

    private GeoServerTileLayerInfo defaultVectorInfo;

    @Before
    public void setup() {
        defaults = GWCConfig.getOldDefaults();
        defaultVectorInfo = TileLayerInfoUtil.create(defaults);
        defaultVectorInfo.getMimeFormats().clear();
        defaultVectorInfo.getMimeFormats().addAll(defaults.getDefaultVectorCacheFormats());
    }

    @Test
    public void testLoadLayerInfo() {
        LayerInfoImpl layer = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);

        assertNull(LegacyTileLayerInfoLoader.load(layer));

        TileLayerInfoUtil.checkAutomaticStyles(layer, defaultVectorInfo);

        LegacyTileLayerInfoLoader.save(defaultVectorInfo, layer.getMetadata());

        GeoServerTileLayerInfo info2 = LegacyTileLayerInfoLoader.load(layer);

        defaultVectorInfo.setId(layer.getId());
        defaultVectorInfo.setName(tileLayerName(layer));
        assertEquals(defaultVectorInfo, info2);
    }

    @Test
    public void testLoadLayerInfoExtraStyles() {
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.setAutoCacheStyles(false);
        TileLayerInfoUtil.setCachedStyles(info, "default", ImmutableSet.of("style1"));

        LayerInfoImpl layer =
                mockLayer("testLayer", new String[] {"style1", "style2"}, PublishedType.RASTER);
        TileLayerInfoUtil.checkAutomaticStyles(layer, info);

        assertNull(LegacyTileLayerInfoLoader.load(layer));

        LegacyTileLayerInfoLoader.save(info, layer.getMetadata());

        GeoServerTileLayerInfo actual;
        actual = LegacyTileLayerInfoLoader.load(layer);

        info.setId(layer.getId());
        info.setName(tileLayerName(layer));
        assertEquals(info, actual);

        layer.setDefaultStyle(null);
        TileLayerInfoUtil.setCachedStyles(info, null, ImmutableSet.of("style1"));
        LegacyTileLayerInfoLoader.save(info, layer.getMetadata());
        actual = LegacyTileLayerInfoLoader.load(layer);
        assertEquals(ImmutableSet.of("style1"), actual.cachedStyles());
    }

    @Test
    public void testLoadLayerInfoAutoCacheStyles() {
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.setAutoCacheStyles(true);

        LayerInfoImpl layer =
                mockLayer("testLayer", new String[] {"style1", "style2"}, PublishedType.RASTER);
        assertNull(LegacyTileLayerInfoLoader.load(layer));

        TileLayerInfoUtil.checkAutomaticStyles(layer, defaultVectorInfo);

        LegacyTileLayerInfoLoader.save(info, layer.getMetadata());

        GeoServerTileLayerInfo actual;
        actual = LegacyTileLayerInfoLoader.load(layer);

        TileLayerInfoUtil.setCachedStyles(info, "default", ImmutableSet.of("style1", "style2"));

        info.setId(layer.getId());
        info.setName(tileLayerName(layer));
        assertEquals(info, actual);

        layer.setDefaultStyle(null);
        TileLayerInfoUtil.setCachedStyles(info, null, ImmutableSet.of("style1", "style2"));

        actual = LegacyTileLayerInfoLoader.load(layer);
        assertEquals(ImmutableSet.of("style1", "style2"), actual.cachedStyles());
    }

    @Test
    public void testLoadLayerGroup() {
        LayerGroupInfoImpl lg =
                mockGroup(
                        "tesGroup",
                        mockLayer("L1", new String[] {}, PublishedType.RASTER),
                        mockLayer("L2", new String[] {}, PublishedType.RASTER));

        assertNull(LegacyTileLayerInfoLoader.load(lg));
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.getMimeFormats().clear();
        info.getMimeFormats().addAll(defaults.getDefaultOtherCacheFormats());

        LegacyTileLayerInfoLoader.save(info, lg.getMetadata());

        GeoServerTileLayerInfo actual;
        actual = LegacyTileLayerInfoLoader.load(lg);

        info.setId(lg.getId());
        info.setName(GWC.tileLayerName(lg));
        assertEquals(info, actual);
    }

    @Test
    public void testClear() {
        LayerGroupInfoImpl lg =
                mockGroup(
                        "tesGroup",
                        mockLayer("L1", new String[] {}, PublishedType.RASTER),
                        mockLayer("L2", new String[] {}, PublishedType.RASTER));

        assertNull(LegacyTileLayerInfoLoader.load(lg));
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.getMimeFormats().clear();
        info.getMimeFormats().addAll(defaults.getDefaultOtherCacheFormats());

        LegacyTileLayerInfoLoader.save(info, lg.getMetadata());

        GeoServerTileLayerInfo actual;
        actual = LegacyTileLayerInfoLoader.load(lg);
        assertNotNull(actual);

        LegacyTileLayerInfoLoader.clear(lg.getMetadata());
        assertNull(LegacyTileLayerInfoLoader.load(lg));
    }
}
