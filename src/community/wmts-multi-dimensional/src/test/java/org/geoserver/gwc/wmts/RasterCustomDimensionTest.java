/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.geoserver.gwc.wmts.MultiDimensionalExtension.ALL_DOMAINS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.List;
import org.geoserver.catalog.*;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.testreader.CustomFormat;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.gwc.wmts.dimensions.RasterCustomDimension;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * This class contains tests that check that custom dimensions values are correctly extracted from
 * rasters. Custom dimensions are only supported in rasters.
 */
public class RasterCustomDimensionTest extends TestsSupport {

    @Test
    public void testDisabledDimension() throws Exception {
        // enable a custom dimension
        DimensionInfo dimensionInfo = new DimensionInfoImpl();
        dimensionInfo.setEnabled(true);
        CoverageInfo rasterInfo = getCoverageInfo();
        rasterInfo
                .getMetadata()
                .put(
                        ResourceInfo.CUSTOM_DIMENSION_PREFIX + CustomFormat.CUSTOM_DIMENSION_NAME,
                        dimensionInfo);
        getCatalog().save(rasterInfo);
        // check that we correctly retrieve the custom dimension
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(1));
        // disable the custom dimension
        dimensionInfo.setEnabled(false);
        rasterInfo
                .getMetadata()
                .put(
                        ResourceInfo.CUSTOM_DIMENSION_PREFIX + CustomFormat.CUSTOM_DIMENSION_NAME,
                        dimensionInfo);
        getCatalog().save(rasterInfo);
        // no dimensions should be available
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(0));
    }

    @Test
    public void testGetDefaultValue() {
        testDefaultValueStrategy(Strategy.MINIMUM, "CustomDimValueA");
        testDefaultValueStrategy(Strategy.MAXIMUM, "CustomDimValueC");
    }

    @Test
    public void testGetDomainsValues() throws Exception {
        testDomainsValuesRepresentation(0, "CustomDimValueA--CustomDimValueC");
        testDomainsValuesRepresentation(
                DimensionsUtils.NO_LIMIT, "CustomDimValueA", "CustomDimValueB", "CustomDimValueC");
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        CoverageInfo rasterInfo = getCoverageInfo();
        Dimension dimension =
                new RasterCustomDimension(
                        wms, getLayerInfo(), CustomFormat.CUSTOM_DIMENSION_NAME, dimensionInfo);
        rasterInfo
                .getMetadata()
                .put(
                        ResourceInfo.CUSTOM_DIMENSION_PREFIX + CustomFormat.CUSTOM_DIMENSION_NAME,
                        dimensionInfo);
        getCatalog().save(rasterInfo);
        return dimension;
    }

    @Test
    public void testGetHistogram() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, null);
        assertThat(histogram.first, is("CustomDimValueA,CustomDimValueB,CustomDimValueC"));
        assertThat(histogram.second, containsInAnyOrder(1, 1, 1));
    }

    /** Helper method that just returns the current layer info. */
    private LayerInfo getLayerInfo() {
        return catalog.getLayerByName(RASTER_CUSTOM.getLocalPart());
    }

    /** Helper method that just returns the current coverage info. */
    private CoverageInfo getCoverageInfo() {
        LayerInfo layerInfo = getLayerInfo();
        assertThat(layerInfo.getResource(), instanceOf(CoverageInfo.class));
        return (CoverageInfo) layerInfo.getResource();
    }
}
