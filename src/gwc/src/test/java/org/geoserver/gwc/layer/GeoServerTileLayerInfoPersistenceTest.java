/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.geoserver.gwc.layer.TileLayerInfoUtil.loadOrCreate;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;

import com.thoughtworks.xstream.XStream;

public class GeoServerTileLayerInfoPersistenceTest {

    private GeoServerTileLayerInfo info;

    private GWCConfig defaults;

    private GeoServerTileLayerInfo defaultVectorInfo;

    @Before
    public void setUp() throws Exception {
        info = new GeoServerTileLayerInfoImpl();
        defaults = GWCConfig.getOldDefaults();
        defaultVectorInfo = TileLayerInfoUtil.create(defaults);
        defaultVectorInfo.getMimeFormats().clear();
        defaultVectorInfo.getMimeFormats().addAll(defaults.getDefaultVectorCacheFormats());
    }

    private void testMarshaling(GeoServerTileLayerInfo info) {

        XStream xstream = XMLConfiguration.getConfiguredXStream(new XStream(), (WebApplicationContext) null);
        xstream = new GWCGeoServerConfigurationProvider().getConfiguredXStream(xstream);

        String marshalled = xstream.toXML(info);
        GeoServerTileLayerInfo unmarshalled = (GeoServerTileLayerInfo) xstream
                .fromXML(new StringReader(marshalled));

        assertNotNull(unmarshalled);

        assertEquals("enabled", info.isEnabled(), unmarshalled.isEnabled());
        assertEquals("autoCacheStyles", info.isAutoCacheStyles(), unmarshalled.isAutoCacheStyles());

        assertEquals("gutter", info.getGutter(), unmarshalled.getGutter());
        assertEquals("metaTilingX", info.getMetaTilingX(), unmarshalled.getMetaTilingX());
        assertEquals("metaTilingY", info.getMetaTilingY(), unmarshalled.getMetaTilingY());
        assertEquals("cachedStyles", info.cachedStyles(), unmarshalled.cachedStyles());
        assertEquals("gridSubsets", info.getGridSubsets(), unmarshalled.getGridSubsets());
        assertEquals("mimeFormats", info.getMimeFormats(), unmarshalled.getMimeFormats());
        assertCollection("parameterFilters", info.getParameterFilters(),
                unmarshalled.getParameterFilters());

        assertEquals("info", info, unmarshalled);
    }

    private void assertCollection(String message, Collection<?> c1, Collection<?> c2) {
        if (c1 == null && c2 != null) {
            assertEquals(message, 0, c2.size());
        } else if (c1 != null && c2 == null) {
            assertEquals(message, 0, c1.size());
        } else {
            assertEquals(message, c1, c2);
        }
    }

    @Test
    public void testMarshallingDefaults() {
        GWCConfig oldDefaults = GWCConfig.getOldDefaults();
        LayerInfo layerInfo = mockLayer("testLayer", new String[]{}, PublishedType.RASTER);
        info = loadOrCreate(layerInfo, oldDefaults);
        testMarshaling(info);
    }

    @Test
    public void testMarshallingGridSubsets() {
        List<XMLGridSubset> subsets = new ArrayList<XMLGridSubset>();
        XMLGridSubset subset;
        subset = new XMLGridSubset();
        subset.setGridSetName("EPSG:4326");
        subset.setZoomStart(1);
        subset.setZoomStop(10);
        subset.setExtent(new BoundingBox(0, 0, 180, 90));
        subsets.add(subset);

        subset = new XMLGridSubset();
        subset.setGridSetName("EPSG:900913");
        subsets.add(subset);

        subset = new XMLGridSubset();
        subset.setGridSetName("GlobalCRS84Scale");
        subset.setZoomStart(4);
        subset.setExtent(new BoundingBox(-100, -40, 100, 40));
        subsets.add(subset);

        info.getGridSubsets().add(subsets.get(0));
        testMarshaling(info);

        info.getGridSubsets().clear();
        info.getGridSubsets().add(subsets.get(1));
        testMarshaling(info);

        info.getGridSubsets().clear();
        info.getGridSubsets().add(subsets.get(2));
        testMarshaling(info);

        info.getGridSubsets().addAll(subsets);
        testMarshaling(info);
    }

    @Test
    public void testMarshallingParameterFilters() throws Exception {
        StringParameterFilter strParam = new StringParameterFilter();
        strParam.setKey("TIME");
        strParam.setDefaultValue("now");
        List<String> strValues = new ArrayList(strParam.getValues());
        strValues.addAll(Arrays.asList("today", "yesterday", "tomorrow"));
        strParam.setValues(strValues);

        RegexParameterFilter regExParam = new RegexParameterFilter();
        regExParam.setKey("CQL_FILTER");
        regExParam.setDefaultValue("INCLUDE");
        regExParam.setRegex(".*");

        FloatParameterFilter floatParam = new FloatParameterFilter();
        floatParam.setKey("ENV");
        floatParam.setThreshold(Float.valueOf(1E-4F));
        List<Float> floatValues = new ArrayList(floatParam.getValues());
        floatValues.addAll(Arrays.asList(1f, 1.5f, 2f, 2.5f));
        floatParam.setValues(floatValues);

        info.getParameterFilters().clear();
        testMarshaling(info);

        info.getParameterFilters().clear();
        info.getParameterFilters().add(strParam);
        testMarshaling(info);

        info.getParameterFilters().clear();
        info.getParameterFilters().add(regExParam);
        testMarshaling(info);

        info.getParameterFilters().clear();
        info.getParameterFilters().add(floatParam);
        testMarshaling(info);

        info.getParameterFilters().clear();
        info.getParameterFilters().add(strParam);
        info.getParameterFilters().add(regExParam);
        info.getParameterFilters().add(floatParam);
        testMarshaling(info);

        StringParameterFilter strParam2 = new StringParameterFilter();
        strParam2.setKey("ELEVATION");
        strParam2.setDefaultValue("1");
        List<String> strValues2 = new ArrayList(strParam2.getValues());
        strValues2.addAll(Arrays.asList("1", "2", "3"));
        strParam2.setValues(strValues2);
        info.getParameterFilters().add(strParam2);
        testMarshaling(info);
    }

}
