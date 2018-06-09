/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.geoserver.gwc.layer.TileLayerInfoUtil.loadOrCreate;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.beans.Introspector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GeoServerTileLayerInfoSerializableTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder();

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

    <T> Matcher<T> sameProperty(T expected, String property) throws Exception {
        return sameProperty(expected, property, Matchers::is);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    <T> Matcher<T> sameProperty(T expected, String property, Function<?, Matcher<?>> valueMatcher)
            throws Exception {
        Object value =
                Arrays.stream(
                                Introspector.getBeanInfo(expected.getClass())
                                        .getPropertyDescriptors())
                        .filter(p -> p.getName().equals(property))
                        .findAny()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "bean expected lacks the property " + property))
                        .getReadMethod()
                        .invoke(expected);
        return hasProperty(property, (Matcher<?>) ((Function) valueMatcher).apply(value));
    }

    private GeoServerTileLayerInfo testMarshaling(GeoServerTileLayerInfo info) throws Exception {

        File f = temp.newFile();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
            out.writeObject(info);
        }
        GeoServerTileLayerInfo unmarshalled;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f)); ) {
            unmarshalled = (GeoServerTileLayerInfo) in.readObject();
        }

        assertThat(unmarshalled, notNullValue());

        assertThat(unmarshalled, sameProperty(info, "enabled"));
        assertThat(unmarshalled, sameProperty(info, "autoCacheStyles"));

        assertThat(unmarshalled, sameProperty(info, "gutter"));
        assertThat(unmarshalled, sameProperty(info, "metaTilingX"));
        assertThat(unmarshalled, sameProperty(info, "metaTilingY"));
        assertThat(unmarshalled, sameProperty(info, "gridSubsets"));
        assertThat(unmarshalled, sameProperty(info, "mimeFormats"));
        assertThat(unmarshalled, sameProperty(info, "parameterFilters"));
        assertThat(unmarshalled, equalTo(info));

        assertThat("cachedStyles", unmarshalled.cachedStyles(), equalTo(info.cachedStyles()));

        return unmarshalled;
    }

    @Test
    public void testMarshallingDefaults() throws Exception {
        GWCConfig oldDefaults = GWCConfig.getOldDefaults();
        LayerInfo layerInfo = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        info = loadOrCreate(layerInfo, oldDefaults);
        testMarshaling(info);
    }

    @Test
    public void testMarshallingBlobStoreId() throws Exception {
        GWCConfig oldDefaults = GWCConfig.getOldDefaults();
        LayerInfo layerInfo = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        info = loadOrCreate(layerInfo, oldDefaults);
        info.setBlobStoreId("myBlobStore");
        GeoServerTileLayerInfo unmarshalled = testMarshaling(info);
        assertThat(unmarshalled, hasProperty("blobStoreId", is("myBlobStore")));
    }

    @Test
    public void testMarshallingGridSubsets() throws Exception {
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
        List<String> strValues = new ArrayList<>(strParam.getValues());
        strValues.addAll(Arrays.asList("today", "yesterday", "tomorrow"));
        strParam.setValues(strValues);

        RegexParameterFilter regExParam = new RegexParameterFilter();
        regExParam.setKey("CQL_FILTER");
        regExParam.setDefaultValue("INCLUDE");
        regExParam.setRegex(".*");

        FloatParameterFilter floatParam = new FloatParameterFilter();
        floatParam.setKey("ENV");
        floatParam.setThreshold(Float.valueOf(1E-4F));
        List<Float> floatValues = new ArrayList<>(floatParam.getValues());
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
        List<String> strValues2 = new ArrayList<>(strParam2.getValues());
        strValues2.addAll(Arrays.asList("1", "2", "3"));
        strParam2.setValues(strValues2);
        info.getParameterFilters().add(strParam2);
        testMarshaling(info);
    }
}
