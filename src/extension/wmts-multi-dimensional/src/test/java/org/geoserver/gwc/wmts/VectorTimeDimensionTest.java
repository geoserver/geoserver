/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.geoserver.gwc.wmts.MultiDimensionalExtension.ALL_DOMAINS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.gwc.wmts.dimensions.VectorTimeDimension;
import org.geotools.api.filter.Filter;
import org.junit.Before;
import org.junit.Test;

/**
 * This class contains tests that check that time dimensions values are correctly extracted from
 * vector data.
 */
public class VectorTimeDimensionTest extends VectorTimeTestSupport {

    @Before
    public void before() {
        System.setProperty("WMTS_HISTOGRAM_IN_MEMORY", "false");
    }

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
                DimensionsUtils.NO_LIMIT,
                false,
                "2012-02-11T00:00:00.000Z",
                "2012-02-12T00:00:00.000Z");
        testDomainsValuesRepresentation(0, "2012-02-11T00:00:00.000Z--2012-02-12T00:00:00.000Z");
    }

    @Test
    public void testGetDomainsValuesRange() throws Exception {
        testDomainsValuesRepresentation(
                3,
                true,
                "2012-02-11T00:00:00.000Z/2012-02-11T11:00:00.000Z",
                "2012-02-11T00:00:00.000Z/2012-02-13T00:00:00.000Z",
                "2012-02-12T00:00:00.000Z/2012-02-12T10:00:00.000Z");

        testDomainsValuesRepresentation(
                0, true, "2012-02-11T00:00:00.000Z--2012-02-13T00:00:00.000Z");
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        dimensionInfo.setAttribute("startTime");
        FeatureTypeInfo vectorInfo = getVectorInfo();
        Dimension dimension = new VectorTimeDimension(wms, getLayerInfo(), dimensionInfo);
        vectorInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(vectorInfo);
        return dimension;
    }

    @Override
    protected Dimension buildDimensionWithEndAttribute(DimensionInfo dimensionInfo) {
        dimensionInfo.setEndAttribute("endTime");
        return buildDimension(dimensionInfo);
    }

    @Test
    public void testGetHistogram() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-13T00:00:00.000Z/P1D"));
        assertThat(histogram.second, equalTo(Arrays.asList(3, 1)));
    }

    @Test
    public void testGetHistogramWithRangeValues() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimensionWithEndAttribute(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-14T00:00:00.000Z/P1D"));
        assertThat(histogram.second, equalTo(Arrays.asList(3, 3, 2)));
    }

    @Test
    public void testGetHistogramWithRangeValues2() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimensionWithEndAttribute(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P2D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-15T00:00:00.000Z/P2D"));
        assertThat(histogram.second, equalTo(Arrays.asList(4, 2)));
    }

    @Test
    public void testGetHistogramWithRangeValues3() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimensionWithEndAttribute(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "PT12H");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-13T12:00:00.000Z/PT12H"));
        assertThat(histogram.second, equalTo(Arrays.asList(3, 2, 3, 2, 2)));
    }

    @Test
    public void testGetHistogramWithRangeValuesInMemory() {
        System.setProperty("WMTS_HISTOGRAM_IN_MEMORY", "true");
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimensionWithEndAttribute(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-14T00:00:00.000Z/P1D"));
        assertThat(histogram.second, equalTo(Arrays.asList(3, 3, 2)));
    }

    @Test
    public void testGetHistogramWithRangeValuesInMemory2() {
        System.setProperty("WMTS_HISTOGRAM_IN_MEMORY", "true");
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimensionWithEndAttribute(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P2D");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-15T00:00:00.000Z/P2D"));
        assertThat(histogram.second, equalTo(Arrays.asList(4, 2)));
    }

    @Test
    public void testGetHistogramWithRangeValuesInMemory3() {
        System.setProperty("WMTS_HISTOGRAM_IN_MEMORY", "true");
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimensionWithEndAttribute(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "PT12H");
        assertThat(histogram.first, is("2012-02-11T00:00:00.000Z/2012-02-13T12:00:00.000Z/PT12H"));
        assertThat(histogram.second, equalTo(Arrays.asList(3, 2, 3, 2, 2)));
    }
}
