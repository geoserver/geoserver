/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;
import static org.geoserver.gwc.GWCTestHelpers.mockGroup;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import junit.framework.TestCase;

import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;

import com.google.common.collect.ImmutableSet;

/**
 * Unit test suite for {@link TileLayerInfoUtil}
 * 
 */
public class TileLayerInfoUtilTest extends TestCase {

    private GWCConfig defaults;

    private GeoServerTileLayerInfo defaultVectorInfo;

    @Override
    protected void setUp() throws Exception {
        defaults = GWCConfig.getOldDefaults();
        defaultVectorInfo = TileLayerInfoUtil.create(defaults);
        defaultVectorInfo.getMimeFormats().clear();
        defaultVectorInfo.getMimeFormats().addAll(defaults.getDefaultVectorCacheFormats());
    }

    public void testCreateLayerInfo() {
        LayerInfoImpl layer = mockLayer("testLayer");
        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        defaultVectorInfo.setId(layer.getId());
        defaultVectorInfo.setName(tileLayerName(layer));
        assertNotNull(info);
        assertEquals(defaultVectorInfo, info);
    }

    public void testCreateLayerGroupInfo() {
        LayerGroupInfoImpl group = mockGroup("testGroup", mockLayer("testLayer"));

        defaults.getDefaultOtherCacheFormats().clear();
        defaults.getDefaultOtherCacheFormats().add("image/png8");
        defaults.getDefaultOtherCacheFormats().add("image/jpeg");

        GeoServerTileLayerInfo expected = TileLayerInfoUtil.create(defaults);
        expected.setId(group.getId());
        expected.setName(GWC.tileLayerName(group));

        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(group, defaults);
        assertNotNull(info);
        assertEquals(expected, info);
    }

    public void testCreateLayerInfoAutoCacheStyles() {
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.setAutoCacheStyles(true);

        defaults.setCacheNonDefaultStyles(true);

        LayerInfoImpl layer = mockLayer("testLayer", "style1", "style2");

        GeoServerTileLayerInfo actual;
        actual = TileLayerInfoUtil.loadOrCreate(layer, defaults);

        TileLayerInfoUtil.setCachedStyles(info, "default", ImmutableSet.of("style1", "style2"));
        assertEquals(info.cachedStyles(), actual.cachedStyles());

        layer.setDefaultStyle(null);
        TileLayerInfoUtil.setCachedStyles(info, "", ImmutableSet.of("style1", "style2"));

        actual = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        assertEquals(ImmutableSet.of("style1", "style2"), actual.cachedStyles());
    }

    public void testCreateLayerGroup() {
        LayerGroupInfoImpl lg = mockGroup("tesGroup", mockLayer("L1"), mockLayer("L2"));

        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.setId(lg.getId());
        info.setName(GWC.tileLayerName(lg));
        info.getMimeFormats().clear();
        info.getMimeFormats().addAll(defaults.getDefaultOtherCacheFormats());

        GeoServerTileLayerInfo actual;
        actual = TileLayerInfoUtil.loadOrCreate(lg, defaults);

        assertEquals(info, actual);
    }

    public void testUpdateAcceptAllRegExParameterFilter() {
        GeoServerTileLayerInfo info = defaultVectorInfo;

        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(info, "ENV", true);

        ParameterFilter filter = TileLayerInfoUtil.findParameterFilter("ENV",
                info.getParameterFilters());
        assertTrue(filter instanceof RegexParameterFilter);
        assertEquals(".*", ((RegexParameterFilter) filter).getRegex());

        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(info, "ENV", true);
        ParameterFilter filter2 = TileLayerInfoUtil.findParameterFilter("ENV",
                info.getParameterFilters());
        assertNotSame(filter, filter2);
        assertEquals(filter, filter2);

        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(info, "ENV", false);
        assertNull(TileLayerInfoUtil.findParameterFilter("ENV", info.getParameterFilters()));
    }

    public void testUpdateAcceptAllFloatParameterFilter() {
        GeoServerTileLayerInfo info = defaultVectorInfo;

        TileLayerInfoUtil.updateAcceptAllFloatParameterFilter(info, "ELEVATION", true);

        ParameterFilter filter = TileLayerInfoUtil.findParameterFilter("ELEVATION",
                info.getParameterFilters());
        assertTrue(filter instanceof FloatParameterFilter);
        assertEquals(0, ((FloatParameterFilter) filter).getValues().size());

        TileLayerInfoUtil.updateAcceptAllFloatParameterFilter(info, "ELEVATION", true);
        ParameterFilter filter2 = TileLayerInfoUtil.findParameterFilter("ELEVATION",
                info.getParameterFilters());
        assertNotSame(filter, filter2);
        assertEquals(filter, filter2);

        TileLayerInfoUtil.updateAcceptAllFloatParameterFilter(info, "ELEVATION", false);
        assertNull(TileLayerInfoUtil.findParameterFilter("ELEVATION", info.getParameterFilters()));
    }

}
