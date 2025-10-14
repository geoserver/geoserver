/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import static org.geoserver.catalog.DimensionPresentation.LIST;
import static org.geoserver.catalog.ResourceInfo.CUSTOM_DIMENSION_PREFIX;
import static org.geoserver.catalog.ResourceInfo.ELEVATION;
import static org.geoserver.catalog.ResourceInfo.TIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.geoserver.wms.WMSInfo;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests the NcWMS GetTimeSeries method
 *
 * @author Cesar Martinez Izquierdo
 */
public class NcWmsGetTimeSeriesTest extends WMSDimensionsTestSupport {

    private static final double EPS = 1e-6;
    private static final double DELTA = EPS;

    private static final int CSV_HEADER_ROWS = 3;

    static final String BASE_URL_4326 =
            "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&FORMAT=image%2Fjpeg&LAYERS=watertemp&QUERY_LAYERS=watertemp&STYLES&&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG%3A4326&WIDTH=101&HEIGHT=101&BBOX=3.724365234375%2C40.81420898437501%2C5.943603515625%2C43.03344726562501";

    static final String BASE_URL_3857 =
            "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&FORMAT=image%2Fjpeg&QUERY_LAYERS=watertemp&STYLES&LAYERS=watertemp&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG%3A3857&WIDTH=101&HEIGHT=101&BBOX=1007839.2841354463%2C5039196.420677232%2C1254607.205826792%2C5285964.342368577";

    static final String TIME_RANGE_COMPLETE = "&TIME=2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z";

    static final String TIME_RANGE_NO_VALUES = "&TIME=2005/2006";

    static final String TIME_RANGE_EXTRA = "&TIME=2008-10-01T00:00:00.000Z/2008-11-01T00:00:00.000Z";

    static final String TIME_RANGE_SLICE1 = "&TIME=2008-10-31T00:00:00.000Z/2008-10-31T00:00:00.000Z";

    static final String TIME_RANGE_SLICE2 = "&TIME=2008-11-01T00:00:00.000Z/2008-11-01T00:00:00.000Z";

    static final String BASE_URL_4326_TIMESERIES =
            "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&FORMAT=image%2Fjpeg&LAYERS=timeseries&QUERY_LAYERS=timeseries&STYLES&&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG%3A4326&WIDTH=101&HEIGHT=101&BBOX=3.724365234375%2C40.81420898437501%2C5.943603515625%2C43.03344726562501";

    static final String REQUEST_ON_NODATA_PIXEL =
            "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&FORMAT=image%2Fjpeg&LAYERS=timeseries&QUERY_LAYERS=timeseries&STYLES&&FEATURE_COUNT=1&BBOX=1,42,2,44&width=90&height=45&x=35&y=38&TIME=2014-01-01T00:00:00.000Z";

    static final String TIME_LIST_PRECISE = "&TIME=2014-01-01,2015-01-01,2016-01-01,2017-01-01";

    static final String TIME_LIST_NEAREST = "&TIME=2014-01-01,2015-01-02,2015-12-31,2017-01-01";

    static final String TIME_PERIOD = "&TIME=2014-01-01/2019-01-01/P1Y";

    static final String CSV_FORMAT = "&INFO_FORMAT=text%2Fcsv";

    static final String PNG_FORMAT = "&INFO_FORMAT=image%2Fpng";

    @After
    public void resetWMSConfig() {
        revertService(WMSInfo.class, null);
    }

    /** Tests the number of output lines, the CSV headers and the returned date and value per each CSV record. */
    @Test
    public void testCsvOutput() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees");

        String url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_COMPLETE;
        MockHttpServletResponse response = getAsServletResponse(url);
        assertEquals("text/csv", response.getContentType());
        assertEquals("inline; filename=watertemp.csv", response.getHeader("Content-Disposition"));
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 5, csvLines.length);
        Assert.assertEquals("CSV header", "Time (UTC),sf:watertemp", csvLines[2]);
        assertCsvLine("value 2008-10-31", csvLines[3], "2008-10-31T00:00:00.000Z", 16.88799985218793, EPS);
        assertCsvLine("value 2008-11-01", csvLines[4], "2008-11-01T00:00:00.000Z", 17.120999863254838, EPS);
    }

    /** Tests that lat lon values are returned for a geographic CRS request */
    @Test
    public void testCsvLatLon() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees");

        String url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_COMPLETE;
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertTrue("Geographic output", csvLines[0].startsWith("# Latitude:"));
        double latitude = Double.parseDouble(csvLines[0].substring(12));
        double longitude = Double.parseDouble(csvLines[1].substring(13));
        Assert.assertEquals("latitude", 41.93481445312501, latitude, 0.000001);
        Assert.assertEquals("longitude", 4.822998046875, longitude, 0.000001);
    }

    /** Tests that x y values are returned for a projected CRS request */
    @Test
    public void testCsvXY() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees");
        String url = BASE_URL_3857 + CSV_FORMAT + TIME_RANGE_COMPLETE;

        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertTrue("Projected output", csvLines[0].startsWith("# X:"));
        double x = Double.parseDouble(csvLines[0].substring(5));
        double y = Double.parseDouble(csvLines[1].substring(5));
        Assert.assertEquals("x", 5163802.004897614, x, 0.000001);
        Assert.assertEquals("y", 1130001.6216064095, y, 0.000001);
    }

    private void assertCsvLine(String message, String line, String expectedDate, double expectedValue, double delta) {
        String[] lineSplit = line.split(",");
        String date = lineSplit[0];
        double value = Double.parseDouble(lineSplit[1]);
        Assert.assertEquals(message + " date", expectedDate, date);
        Assert.assertEquals(message + " value", expectedValue, value, delta);
    }

    /** Ensures we get the right results with shorter or wider time ranges */
    @Test
    public void testTimeRanges() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees");

        String url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_EXTRA;
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 5, csvLines.length);
        assertCsvLine("value 2008-10-31", csvLines[3], "2008-10-31T00:00:00.000Z", 16.88799985218793, EPS);
        assertCsvLine("value 2008-11-01", csvLines[4], "2008-11-01T00:00:00.000Z", 17.120999863254838, EPS);

        url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_SLICE1;
        rawCsv = getAsString(url);
        csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 4, csvLines.length);
        assertCsvLine("value 2008-10-31", csvLines[3], "2008-10-31T00:00:00.000Z", 16.88799985218793, EPS);

        url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_SLICE2;
        rawCsv = getAsString(url);
        csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 4, csvLines.length);
        assertCsvLine("value 2008-11-01", csvLines[3], "2008-11-01T00:00:00.000Z", 17.120999863254838, EPS);
    }

    /** Ensures we get the right results when specifying a timeList with/without nearest match enabled */
    @Test
    public void testTimeList() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, LIST, null, null, null);

        String url = BASE_URL_4326_TIMESERIES + CSV_FORMAT + TIME_LIST_PRECISE;
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        final int numberOfResults = 4;
        Assert.assertEquals("CSV Number of results", CSV_HEADER_ROWS + numberOfResults, csvLines.length);
        double[] expectedValues =
                new double[] {16.88799985218793, 13.399999686516821, 13.331999683286995, 17.120999863254838};
        final int referenceYear = 2014;
        for (int i = 0; i < numberOfResults; i++) {
            String date = (referenceYear + i) + "-01-01";
            assertCsvLine(
                    "value " + date, csvLines[CSV_HEADER_ROWS + i], date + "T00:00:00.000Z", expectedValues[i], DELTA);
        }

        url = BASE_URL_4326_TIMESERIES + CSV_FORMAT + TIME_LIST_NEAREST;
        rawCsv = getAsString(url);
        csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 5, csvLines.length);
        assertCsvLine("value 2014-01-01", csvLines[3], "2014-01-01T00:00:00.000Z", 16.88799985218793, DELTA);
        assertCsvLine("value 2017-01-01", csvLines[4], "2017-01-01T00:00:00.000Z", 17.120999863254838, DELTA);

        // Enable Nearest Match
        setNearestMatch(TIMESERIES, TIME, "P2D");
        url = BASE_URL_4326_TIMESERIES + CSV_FORMAT + TIME_LIST_NEAREST;
        rawCsv = getAsString(url);
        csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 7, csvLines.length);
        assertCsvLine("value 2010-01-01", csvLines[3], "2014-01-01T00:00:00.000Z", 16.88799985218793, DELTA);
        assertCsvLine("value 2015-01-01", csvLines[4], "2015-01-01T00:00:00.000Z", 13.399999686516821, DELTA);
        assertCsvLine("value 2016-01-01", csvLines[5], "2016-01-01T00:00:00.000Z", 13.331999683286995, DELTA);
        assertCsvLine("value 2017-01-01", csvLines[6], "2017-01-01T00:00:00.000Z", 17.120999863254838, DELTA);
        setNearestMatch(TIMESERIES, TIME, null);
    }

    @Test
    public void testTimeRangeWithPeriod() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, LIST, null, null, null);

        String url = BASE_URL_4326_TIMESERIES + CSV_FORMAT + TIME_PERIOD;
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 5, csvLines.length);
        assertCsvLine("value 2014-01-01", csvLines[3], "2014-01-01T00:00:00.000Z", 16.88799985218793, DELTA);
        assertCsvLine("value 2018-01-01", csvLines[4], "2018-01-01T00:00:00.000Z", 16.88799985218793, DELTA);

        // Enable nearest match to catch more results at the beginning of each year
        setNearestMatch(TIMESERIES, TIME, "P2D");
        url = BASE_URL_4326_TIMESERIES + CSV_FORMAT + TIME_PERIOD;
        rawCsv = getAsString(url);
        csvLines = rawCsv.split("\\r?\\n");

        final int numberOfResults = 6;
        Assert.assertEquals("CSV Number of results", CSV_HEADER_ROWS + numberOfResults, csvLines.length);
        double[] expectedValues = new double[] {
            16.88799985218793,
            13.399999686516821,
            13.331999683286995,
            17.120999863254838,
            16.88799985218793,
            13.399999686516821
        };
        final int referenceYear = 2014;
        for (int i = 0; i < numberOfResults; i++) {
            String date = (referenceYear + i) + "-01-01";
            assertCsvLine(
                    "value " + date, csvLines[CSV_HEADER_ROWS + i], date + "T00:00:00.000Z", expectedValues[i], DELTA);
        }
        setNearestMatch(TIMESERIES, TIME, null);
    }

    /** Test we are getting an empty result when hitting a nodata pixel */
    @Test
    public void testEmptyResultsWhenNodata() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, LIST, null, null, null);
        String url = REQUEST_ON_NODATA_PIXEL;
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");

        // Make sure we aren getting an empty value on nodata
        Assert.assertEquals("CSV Number of results", CSV_HEADER_ROWS + 1, csvLines.length);
        String line = csvLines[CSV_HEADER_ROWS];
        String[] lineSplit = line.split(",");
        // Nothing get found after the comma, meaning the result is empty
        Assert.assertEquals(1, lineSplit.length);
    }

    private void setNearestMatch(QName layer, String dimension, String nearestAcceptableInterval) {
        CoverageInfo info = getCatalog().getCoverageByName(layer.getLocalPart());
        DimensionInfo dim = (DimensionInfo) info.getMetadata().get(dimension);
        if (dim != null) {
            boolean nearestEnabled = nearestAcceptableInterval != null;
            dim.setNearestMatchEnabled(nearestEnabled);
            if (nearestEnabled) {
                dim.setAcceptableInterval(nearestAcceptableInterval);
            }
        }
        getCatalog().save(info);
    }

    /** Tests the chart output */
    @Test
    public void testChartOutput() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees");
        BufferedImage image = getAsImage(BASE_URL_4326 + PNG_FORMAT + TIME_RANGE_COMPLETE, "image/png");
        assertPixel(image, 679, 50, new Color(255, 85, 85));
        assertPixel(image, 75, 536, new Color(255, 85, 85));
        assertPixel(image, 317, 373, Color.WHITE);
    }

    /**
     * Ensures we get all values in interval when nearest match is enabled and querying with a simple interval without
     * period
     */
    @Test
    public void testTimeRangesNearestWithSimpleRange() throws Exception {
        // setup both dimensions to have a reliable output (otherwise which elevation
        // wins is dependent on the file system listing order)
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees", true, null);
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, null, "degrees", true, null);
        String url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_COMPLETE;
        String rawCsv = getAsString(url);
        String[] lines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 5, lines.length);
        assertCsvLine("date 2008-10-31", lines[3], "2008-10-31T00:00:00.000Z", 16.887999, EPS);
        assertCsvLine("date 2008-11-01", lines[4], "2008-11-01T00:00:00.000Z", 17.120999, EPS);
    }

    /** Ensures that with no values found a csv with no dates listed is produced */
    @Test
    public void testTimeRangesNearestWithNoValuesFound() throws Exception {
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees", true, "PT101M/PT0H");
        String url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_NO_VALUES;
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 3, csvLines.length);
    }

    @Test
    public void testTooManyValuesPeriod() throws Exception {
        // set a specific number of max values
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        NcWmsInfo ncwms = new NcWMSInfoImpl();
        ncwms.setMaxTimeSeriesValues(3);
        wms.getMetadata().put(NcWmsService.WMS_CONFIG_KEY, ncwms);
        gs.save(wms);

        // enable time (otherwise GetTimeSeries is refused)
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees", true, "PT101M/PT0H");

        // ask for too many (less than the default WMS one, but more than the above configuration)
        Document dom = getAsDOM("wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&FORMAT=image%2Fjpeg&LAYERS"
                + "=watertemp&QUERY_LAYERS=watertemp&STYLES&&FEATURE_COUNT=50&X=50&Y=50"
                + "&SRS=EPSG%3A4326&WIDTH=101&HEIGHT=101&BBOX=3.724365234375%2C40"
                + ".81420898437501%2C5.943603515625%2C43.03344726562501&time=&TIME=2005/2006/P1M");
        assertThat(
                checkLegacyException(dom, "InvalidParameterValue", "time"),
                CoreMatchers.containsString("More than 3 times specified in the request, bailing out."));
    }

    @Test
    public void testSourceWithTimeRanges() throws Exception {
        // setup all the dimensions
        setupRasterDimension(TIMERANGES, TIME, LIST, null, null, null);
        setupRasterDimension(TIMERANGES, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(TIMERANGES, CUSTOM_DIMENSION_PREFIX + "WAVELENGTH", LIST, null, null, null);
        // not setting up the date custom dimension as it just uses the same columns as time

        // prepare URL
        String layer = getLayerId(TIMERANGES);
        String baseUrl = "wms?LAYERS="
                + layer
                + "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&SRS=EPSG:4326"
                + "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&query_layers="
                + layer
                + "&x=68&y=72";
        String url = baseUrl + "&TIME=2008-10-31T12:00:00.000Z/2008-11-06T12:00:00.000Z";

        // run and check
        String rawCsv = getAsString(url);
        String[] lines = rawCsv.split("\\r?\\n");
        Assert.assertEquals(5, lines.length);
        assertCsvLine("date 2008-10-31", lines[3], "2008-10-31T00:00:00.000Z", 20.027, EPS);
        assertCsvLine("date 2008-11-05", lines[4], "2008-11-05T00:00:00.000Z", 14.782, EPS);
    }

    @Test
    public void testTooManyValuesRange() throws Exception {
        // set a specific number of max values
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        NcWmsInfo ncwms = new NcWMSInfoImpl();
        ncwms.setMaxTimeSeriesValues(1);
        wms.getMetadata().put(NcWmsService.WMS_CONFIG_KEY, ncwms);
        gs.save(wms);

        // enable time (otherwise GetTimeSeries is refused)
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, "degrees");

        // ask for too many (less than the default WMS one, but more than the above configuration)
        Document dom = getAsDOM(BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_COMPLETE);
        assertThat(
                checkLegacyException(dom, "InvalidParameterValue", "time"),
                CoreMatchers.containsString(
                        "This request would process 2 times, while the maximum allowed is 1. Please reduce the size of the requested time range."));
    }
}
