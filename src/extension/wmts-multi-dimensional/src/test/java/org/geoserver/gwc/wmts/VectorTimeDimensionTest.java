/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.geoserver.gwc.wmts.MultiDimensionalExtension.ALL_DOMAINS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Date;
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
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.Converters;
import org.junit.Before;
import org.junit.Test;

/** This class contains tests that check that time dimensions values are correctly extracted from vector data. */
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
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS)
                        .size(),
                is(1));
        // disable the time dimension
        dimensionInfo.setEnabled(false);
        vectorInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(vectorInfo);
        // no dimensions should be available
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS)
                        .size(),
                is(0));
    }

    @Test
    public void testGetDefaultValue() {
        testDefaultValueStrategy(Strategy.MINIMUM, "2012-02-11T00:00:00Z");
        testDefaultValueStrategy(Strategy.MAXIMUM, "2012-02-12T00:00:00Z");
    }

    @Test
    public void testGetDomainsValues() throws Exception {
        testDomainsValuesRepresentation(
                DimensionsUtils.NO_LIMIT, false, "2012-02-11T00:00:00.000Z", "2012-02-12T00:00:00.000Z");
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

        testDomainsValuesRepresentation(0, true, "2012-02-11T00:00:00.000Z--2012-02-13T00:00:00.000Z");
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

    /**
     * Tests that getHistogram respects a filter restricting the time dimension to a single day. Only 3 of 4 features
     * have startTime=2012-02-11, so the histogram should reflect that.
     */
    @Test
    public void testGetHistogramWithTimeFilter() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        // Create a filter for startTime = 2012-02-11 (matches features 0, 2, 3)
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Date filterDate = Converters.convert("2012-02-11T00:00:00.000Z", Date.class);
        Filter filter = ff.equal(ff.property("startTime"), ff.literal(filterDate), true);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(filter, "P1D");
        // With filter, only 3 features match, all on the same day (2012-02-11)
        // The histogram domain should NOT span to 2012-02-12 or 2012-02-13
        int total = histogram.second.stream().mapToInt(i -> i).sum();
        assertThat("Total histogram count should be 3 for startTime=2012-02-11 filter", total, is(3));
        // Domain should reflect only the filtered data, not the full domain
        assertThat(
                "Histogram domain should not include 2012-02-13 when filtered to 2012-02-11",
                histogram.first,
                is(not("2012-02-11T00:00:00.000Z/2012-02-13T00:00:00.000Z/P1D")));
    }

    /**
     * Tests that getHistogram with a range dimension (endAttribute set) respects a time filter. Only 1 of 4 features
     * has startTime=2012-02-12.
     */
    @Test
    public void testGetHistogramWithRangeValuesAndTimeFilter() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimensionWithEndAttribute(dimensionInfo);
        // Create a filter for startTime = 2012-02-12 (matches only feature 1)
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Date filterDate = Converters.convert("2012-02-12T00:00:00.000Z", Date.class);
        Filter filter = ff.equal(ff.property("startTime"), ff.literal(filterDate), true);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(filter, "P1D");
        // Only feature 1 matches: startTime=2012-02-12, endTime=2012-02-12T10:00:00Z
        int total = histogram.second.stream().mapToInt(i -> i).sum();
        assertThat("Total histogram count should be 1 for startTime=2012-02-12 filter", total, is(1));
        // Full unfiltered range domain is "2012-02-11T00:00:00.000Z/2012-02-14T00:00:00.000Z/P1D"
        // Filtered domain should be different
        assertThat(
                "Range histogram domain should not be the full domain when filtered",
                histogram.first,
                is(not("2012-02-11T00:00:00.000Z/2012-02-14T00:00:00.000Z/P1D")));
    }
}
