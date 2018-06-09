/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests the NcWMS GetTimeSeries method
 *
 * @author Cesar Martinez Izquierdo
 */
public class NcWmsGetTimeSeriesTest extends WMSDimensionsTestSupport {

    static final String BASE_URL_4326 =
            "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&FORMAT=image%2Fjpeg&LAYERS=watertemp&QUERY_LAYERS=watertemp&STYLES&&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG%3A4326&WIDTH=101&HEIGHT=101&BBOX=3.724365234375%2C40.81420898437501%2C5.943603515625%2C43.03344726562501";

    static final String BASE_URL_3857 =
            "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&FORMAT=image%2Fjpeg&QUERY_LAYERS=watertemp&STYLES&LAYERS=watertemp&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG%3A3857&WIDTH=101&HEIGHT=101&BBOX=1007839.2841354463%2C5039196.420677232%2C1254607.205826792%2C5285964.342368577";

    static final String TIME_RANGE_COMPLETE =
            "&TIME=2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z";

    static final String TIME_RANGE_EXTRA =
            "&TIME=2008-10-01T00:00:00.000Z/2008-11-01T00:00:00.000Z";

    static final String TIME_RANGE_SLICE1 =
            "&TIME=2008-10-31T00:00:00.000Z/2008-10-31T00:00:00.000Z";

    static final String TIME_RANGE_SLICE2 =
            "&TIME=2008-11-01T00:00:00.000Z/2008-11-01T00:00:00.000Z";

    static final String CSV_FORMAT = "&INFO_FORMAT=text%2Fcsv";

    static final String PNG_FORMAT = "&INFO_FORMAT=image%2Fpng";

    /**
     * Tests the number of output lines, the CSV headers and the returned date and value per each
     * CSV record.
     */
    @Test
    public void testCsvOutput() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, "degrees");

        String url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_COMPLETE;
        MockHttpServletResponse response = getAsServletResponse(url);
        assertEquals("text/csv", response.getContentType());
        assertEquals("inline; filename=watertemp.csv", response.getHeader("Content-Disposition"));
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 5, csvLines.length);
        Assert.assertEquals("CSV header", "Time (UTC),sf:watertemp", csvLines[2]);
        assertCsvLine(
                "value 2008-10-31",
                csvLines[3],
                "2008-10-31T00:00:00.000Z",
                16.88799985218793,
                0.000000000001);
        assertCsvLine(
                "value 2008-11-01",
                csvLines[4],
                "2008-11-01T00:00:00.000Z",
                17.120999863254838,
                0.000000000001);
    }

    /** Tests that lat lon values are returned for a geographic CRS request */
    @Test
    public void testCsvLatLon() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, "degrees");

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
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, "degrees");
        String url = BASE_URL_3857 + CSV_FORMAT + TIME_RANGE_COMPLETE;

        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertTrue("Projected output", csvLines[0].startsWith("# X:"));
        double x = Double.parseDouble(csvLines[0].substring(5));
        double y = Double.parseDouble(csvLines[1].substring(5));
        Assert.assertEquals("x", 5163802.004897614, x, 0.000001);
        Assert.assertEquals("y", 1130001.6216064095, y, 0.000001);
    }

    private void assertCsvLine(
            String message, String line, String expectedDate, double expectedValue, double delta) {
        String[] lineSplit = line.split(",");
        String date = lineSplit[0];
        double value = Double.parseDouble(lineSplit[1]);
        Assert.assertEquals(message + " date", expectedDate, date);
        Assert.assertEquals(message + " value", expectedValue, value, delta);
    }

    /** Ensures we get the right results with shorter or wider time ranges */
    @Test
    public void testTimeRanges() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, "degrees");

        String url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_EXTRA;
        String rawCsv = getAsString(url);
        String[] csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 5, csvLines.length);
        assertCsvLine(
                "value 2008-10-31",
                csvLines[3],
                "2008-10-31T00:00:00.000Z",
                16.88799985218793,
                0.000000000001);
        assertCsvLine(
                "value 2008-11-01",
                csvLines[4],
                "2008-11-01T00:00:00.000Z",
                17.120999863254838,
                0.000000000001);

        url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_SLICE1;
        rawCsv = getAsString(url);
        csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 4, csvLines.length);
        assertCsvLine(
                "value 2008-10-31",
                csvLines[3],
                "2008-10-31T00:00:00.000Z",
                16.88799985218793,
                0.000000000001);

        url = BASE_URL_4326 + CSV_FORMAT + TIME_RANGE_SLICE2;
        rawCsv = getAsString(url);
        csvLines = rawCsv.split("\\r?\\n");
        Assert.assertEquals("CSV Number of results", 4, csvLines.length);
        assertCsvLine(
                "value 2008-11-01",
                csvLines[3],
                "2008-11-01T00:00:00.000Z",
                17.120999863254838,
                0.000000000001);
    }

    /** Tests the chart output */
    @Test
    public void testChartOutput() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, "degrees");
        BufferedImage image =
                getAsImage(BASE_URL_4326 + PNG_FORMAT + TIME_RANGE_COMPLETE, "image/png");
        assertPixel(image, 679, 50, new Color(255, 85, 85));
        assertPixel(image, 75, 536, new Color(255, 85, 85));
        assertPixel(image, 317, 373, Color.WHITE);
    }
}
