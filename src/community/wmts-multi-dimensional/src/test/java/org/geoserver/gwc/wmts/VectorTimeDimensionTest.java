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
import org.geoserver.gwc.wmts.dimensions.VectorTimeDimension;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * This class contains tests that check that time dimensions values are correctly extracted from
 * vector data.
 */
public class VectorTimeDimensionTest extends VectorTimeTestSupport {

    @Test
    public void testDisabledDimension() throws Exception {
        // enable a time dimension
        DimensionInfo dimensionInfo = new DimensionInfoImpl();
        dimensionInfo.setEnabled(true);
        FeatureTypeInfo vectorInfo = getVectorInfo();
        vectorInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(vectorInfo);
        // check that we correctly retrieve the time dimension
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(1));
        // disable the time dimension
        dimensionInfo.setEnabled(false);
        vectorInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(vectorInfo);
        // no dimensions should be available
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(0));
    }

    @Test
    public void testGetDefaultValue() {
        testDefaultValueStrategy(Strategy.MINIMUM, "2012-02-11T00:00:00Z");
        testDefaultValueStrategy(Strategy.MAXIMUM, "2012-02-12T00:00:00Z");
    }

    @Test
    public void testGetDomainsValues() throws Exception {
        testDomainsValuesRepresentation(
                DimensionsUtils.NO_LIMIT, "2012-02-11T00:00:00.000Z", "2012-02-12T00:00:00.000Z");
        testDomainsValuesRepresentation(0, "2012-02-11T00:00:00.000Z--2012-02-12T00:00:00.000Z");
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        dimensionInfo.setAttribute("startTime");
        FeatureTypeInfo rasterInfo = getVectorInfo();
        Dimension dimension = new VectorTimeDimension(wms, getLayerInfo(), dimensionInfo);
        rasterInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(rasterInfo);
        return dimension;
    }

    @Test
    public void testGetHistogram() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-13T00:00:00.000Z/P1D"));
        assertThat(histogram.second, equalTo(Arrays.asList(3, 1)));
    }
}
