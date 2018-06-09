/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.geoserver.catalog.*;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.gwc.wmts.dimensions.RasterTimeDimension;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.feature.type.DateUtil;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * This class contains tests that check that time dimension values are correctly extracted from
 * rasters. Some domain values are dynamically created based on the current time, this matches what
 * is done in WMS dimensions tests.
 *
 * <p>Note, there is some inconsistency between ISO formatter and GeoTools data serializer string
 * output, this is something that needs to be fixed (or maybe not) at GeoServer and GeoTools level.
 * In the mean time this tests will use the two formatter were needed.
 */
public class RasterTimeDimensionTest extends TestsSupport {

    // sorted time domain values as date objects
    protected static Date[] DATE_VALUES =
            new Date[] {
                DateUtil.deserializeDateTime("2008-10-31T00:00:00Z"),
                DateUtil.deserializeDateTime("2008-11-01T00:00:00Z"),
                getGeneratedMinValue(),
                getGeneratedMiddleValue(),
                getGeneratedMaxValue()
            };

    // sorted time domain values as strings formatted with ISO8601 formatter
    protected static String[] STRING_VALUES =
            new String[] {
                formatDate(DATE_VALUES[0]),
                formatDate(DATE_VALUES[1]),
                formatDate(DATE_VALUES[2]),
                formatDate(DATE_VALUES[3]),
                formatDate(DATE_VALUES[4])
            };

    @Test
    public void testDisabledDimension() throws Exception {
        // enable a time dimension
        DimensionInfo dimensionInfo = new DimensionInfoImpl();
        dimensionInfo.setEnabled(true);
        CoverageInfo rasterInfo = getCoverageInfo();
        rasterInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(rasterInfo);
        // check that we correctly retrieve the time dimension
        assertThat(DimensionsUtils.extractDimensions(wms, getLayerInfo()).size(), is(1));
        // disable the time dimension
        dimensionInfo.setEnabled(false);
        rasterInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(rasterInfo);
        // no dimensions should be available
        assertThat(DimensionsUtils.extractDimensions(wms, getLayerInfo()).size(), is(0));
    }

    @Test
    public void testGetDefaultValue() {
        testDefaultValueStrategy(
                Strategy.MINIMUM, DateUtil.serializeDateTime(DATE_VALUES[0].getTime(), true));
        testDefaultValueStrategy(
                Strategy.MAXIMUM, DateUtil.serializeDateTime(DATE_VALUES[4].getTime(), true));
    }

    @Test
    public void testGetDomainsValues() throws Exception {
        testDomainsValuesRepresentation(DimensionPresentation.LIST, STRING_VALUES);
        testDomainsValuesRepresentation(
                DimensionPresentation.CONTINUOUS_INTERVAL,
                STRING_VALUES[0] + "--" + STRING_VALUES[4]);
        testDomainsValuesRepresentation(
                DimensionPresentation.DISCRETE_INTERVAL,
                STRING_VALUES[0] + "--" + STRING_VALUES[4]);
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        return new RasterTimeDimension(wms, getLayerInfo(), dimensionInfo);
    }

    /** Helper method that simply formats a date using the ISO8601 formatter. */
    private static String formatDate(Date date) {
        ISO8601Formatter formatter = new ISO8601Formatter();
        return formatter.format(date);
    }

    /** Generates the current minimum date. */
    private static Date getGeneratedMinValue() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        return calendar.getTime();
    }

    /**
     * Generates the current middle date, this date is one month later than the current minimum
     * date.
     */
    private static Date getGeneratedMiddleValue() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
        return calendar.getTime();
    }

    /**
     * Generates the current maximum date, this date is one year later than the current minimum
     * date.
     */
    private static Date getGeneratedMaxValue() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        // this is the way the original data month is setup, I don't understand this but without
        // this the month may not correspond
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
        return calendar.getTime();
    }

    @Test
    public void testGetHistogram() {
        DimensionInfo dimensionInfo = createDimension(true, DimensionPresentation.LIST, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1Y");
        assertThat(histogram.first, is("2008-10-31T00:00:00.000Z/" + STRING_VALUES[4] + "/P1Y"));
        assertThat(histogram.second.stream().reduce(0, (total, value) -> total + value), is(6));
    }

    /** Helper method that just returns the current layer info. */
    private LayerInfo getLayerInfo() {
        return catalog.getLayerByName(RASTER_TIME.getLocalPart());
    }

    /** Helper method that just returns the current coverage info. */
    private CoverageInfo getCoverageInfo() {
        LayerInfo layerInfo = getLayerInfo();
        assertThat(layerInfo.getResource(), instanceOf(CoverageInfo.class));
        return (CoverageInfo) layerInfo.getResource();
    }
}
