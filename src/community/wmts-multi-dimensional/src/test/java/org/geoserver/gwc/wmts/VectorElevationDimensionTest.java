/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.geoserver.gwc.wmts.MultiDimensionalExtension.ALL_DOMAINS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.*;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.gwc.wmts.dimensions.VectorElevationDimension;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * This class contains tests that check that elevation dimensions values are correctly extracted
 * from vector data.
 */
public class VectorElevationDimensionTest extends TestsSupport {

    @Test
    public void testDisabledDimension() throws Exception {
        // enable a elevation dimension
        DimensionInfo dimensionInfo = new DimensionInfoImpl();
        dimensionInfo.setEnabled(true);
        FeatureTypeInfo vectorInfo = getVectorInfo();
        vectorInfo.getMetadata().put(ResourceInfo.ELEVATION, dimensionInfo);
        getCatalog().save(vectorInfo);
        // check that we correctly retrieve the elevation dimension
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(1));
        // disable the elevation dimension
        dimensionInfo.setEnabled(false);
        vectorInfo.getMetadata().put(ResourceInfo.ELEVATION, dimensionInfo);
        getCatalog().save(vectorInfo);
        // no dimensions should be available
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(0));
    }

    @Test
    public void testGetDefaultValue() {
        testDefaultValueStrategy(Strategy.MINIMUM, "1.0");
        testDefaultValueStrategy(Strategy.MAXIMUM, "5.0");
    }

    @Test
    public void testGetDomainsValues() throws Exception {
        testDomainsValuesRepresentation(2, "1.0--5.0");
        testDomainsValuesRepresentation(4, "1.0", "2.0", "3.0", "5.0");
        testDomainsValuesRepresentation(7, "1.0", "2.0", "3.0", "5.0");
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        dimensionInfo.setAttribute("startElevation");
        FeatureTypeInfo rasterInfo = getVectorInfo();
        Dimension dimension = new VectorElevationDimension(wms, getLayerInfo(), dimensionInfo);
        rasterInfo.getMetadata().put(ResourceInfo.ELEVATION, dimensionInfo);
        getCatalog().save(rasterInfo);
        return dimension;
    }

    @Test
    public void testGetHistogram() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "1");
        assertThat(histogram.first, is("1.0/6.0/1.0"));
        assertThat(histogram.second, equalTo(Arrays.asList(1, 1, 1, 0, 1)));
    }

    @Test
    public void testGetHistogramMisaligned() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "0.75");
        assertThat(histogram.first, is("1.0/5.0/0.75"));
        assertThat(histogram.second, equalTo(Arrays.asList(1, 1, 1, 0, 0, 1)));
    }

    /** Helper method that just returns the current layer info. */
    private LayerInfo getLayerInfo() {
        return catalog.getLayerByName(VECTOR_ELEVATION.getLocalPart());
    }

    /** Helper method that just returns the current vector info. */
    private FeatureTypeInfo getVectorInfo() {
        LayerInfo layerInfo = getLayerInfo();
        assertThat(layerInfo.getResource(), instanceOf(FeatureTypeInfo.class));
        return (FeatureTypeInfo) layerInfo.getResource();
    }
}
