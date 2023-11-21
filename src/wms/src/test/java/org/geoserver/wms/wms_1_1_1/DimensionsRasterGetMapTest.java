/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.geoserver.catalog.DimensionInfo.NearestFailBehavior.EXCEPTION;
import static org.geoserver.catalog.DimensionPresentation.LIST;
import static org.geoserver.catalog.ResourceInfo.ELEVATION;
import static org.geoserver.catalog.ResourceInfo.TIME;
import static org.geoserver.platform.ServiceException.INVALID_DIMENSION_VALUE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.util.NearestMatchFinder;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.junit.Test;
import org.w3c.dom.Document;

public class DimensionsRasterGetMapTest extends WMSDimensionsTestSupport {

    static final String BASE_URL =
            "wms?service=WMS&version=1.1.0"
                    + "&request=GetMap&layers=watertemp&styles="
                    + "&bbox=0.237,40.562,14.593,44.558&width=200&height=80"
                    + "&srs=EPSG:4326";
    static final String BASE_PNG_URL = BASE_URL + "&format=image/png";
    static final String MIME = "image/png";

    @Test
    public void testNoDimension() throws Exception {
        BufferedImage image = getAsImage(BASE_PNG_URL, MIME);

        // the result is really just the result of how the tiles are setup in the mosaic,
        // but since they overlap with each other we just want to check the image is not
        // empty
        assertNotBlank("water temperature", image);
    }

    @Test
    public void testDefaultValues() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);

        BufferedImage image = getAsImage(BASE_PNG_URL, "image/png");

        // should be light red pixel and the first pixel is there only at the default elevation
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 187, 187));
    }

    /** Same as above, but obtained via sorting on one attribute instead of using both dimensions */
    @Test
    public void testSortTimeDescending() throws Exception {
        // setting up only elevation, the time will be picked by sorting
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);

        BufferedImage image = getAsImage(BASE_PNG_URL + "&sortBy=ingestion D", "image/png");

        // should be light red pixel and the first pixel is there only at the default elevation
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 187, 187));
    }

    /** Same as above, but obtained via sorting on two attributes instead of using dimensions */
    @Test
    public void testSortTwoAttributes() throws Exception {
        // setting up no dimension, will also sort on elevation

        BufferedImage image =
                getAsImage(BASE_PNG_URL + "&sortBy=ingestion D,elevation", "image/png");

        // should be light red pixel and the first pixel is there only at the default elevation
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 187, 187));
    }

    @Test
    public void testElevation() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);

        BufferedImage image = getAsImage(BASE_PNG_URL + "&elevation=100", "image/png");

        // at this elevation the pixel is NODATA -> bgcolor
        assertPixel(image, 36, 31, new Color(255, 255, 255));
        // and this one a light blue
        assertPixel(image, 68, 72, new Color(246, 246, 255));
    }

    @Test
    public void testElevationInvalidIgnore() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);
        setExceptionsOnInvalidDimension(false);

        BufferedImage image = getAsImage(BASE_PNG_URL + "&elevation=-100", "image/png");

        // the two test pixels should both be bgcolor, no data selected
        assertPixel(image, 36, 31, Color.WHITE);
        assertPixel(image, 68, 72, Color.WHITE);
    }

    @Test
    public void testElevationInvalidException() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);
        setExceptionsOnInvalidDimension(true);

        Document dom = getAsDOM(BASE_PNG_URL + "&elevation=-100");

        String message = checkLegacyException(dom, INVALID_DIMENSION_VALUE, "elevation");
        assertThat(message, containsString("Could not find a match for 'elevation' value: '-100'"));
    }

    /** Same as above, but obtained via sorting instead of using dimensions */
    @Test
    public void testSortElevationDescending() throws Exception {
        // dataset has no data, avoid interference from lower layers
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(WATTEMP));
        ci.getParameters().put(ImageMosaicFormat.MAX_ALLOWED_TILES.getName().getCode(), "1");
        getCatalog().save(ci);

        try {
            BufferedImage image =
                    getAsImage(
                            BASE_PNG_URL + "&bgcolor=0xFF0000&sortBy=elevation D,ingestion D",
                            "image/png");

            // at this elevation the pixel is black
            assertPixel(image, 36, 31, new Color(255, 0, 0));
            // and this one a light blue
            assertPixel(image, 68, 72, new Color(246, 246, 255));
        } finally {
            ci = getCatalog().getCoverageByName(getLayerId(WATTEMP));
            ci.getParameters().remove(ImageMosaicFormat.MAX_ALLOWED_TILES.getName().getCode());
            getCatalog().save(ci);
        }
    }

    @Test
    public void testTime() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);

        BufferedImage image =
                getAsImage(BASE_PNG_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));
    }

    @Test
    public void testTimeNoNearestCloseIgnore() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);
        setExceptionsOnInvalidDimension(false);

        BufferedImage image =
                getAsImage(BASE_PNG_URL + "&time=2008-10-31T08:00:00.000Z", "image/png");

        // no match, so we're getting th default background color
        assertPixel(image, 36, 31, Color.WHITE);
        assertPixel(image, 68, 72, Color.WHITE);
    }

    @Test
    public void testTimeNoNearestCloseException() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);
        setExceptionsOnInvalidDimension(true);

        Document dom = getAsDOM(BASE_PNG_URL + "&time=2008-10-31T08:00:00.000Z");
        String message = checkLegacyException(dom, INVALID_DIMENSION_VALUE, "time");
        assertThat(
                message,
                containsString(
                        "Could not find a match for 'time' value: '2008-10-31T08:00:00.000Z'"));
    }

    @Test
    public void testTimeNearestClose() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, ResourceInfo.TIME_UNIT, null);
        setupNearestMatch(WATTEMP, TIME, true);

        BufferedImage image =
                getAsImage(BASE_PNG_URL + "&time=2008-10-31T08:00:00.000Z", "image/png");
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-10-31T00:00:00.000Z");

        // same as testTime
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));
    }

    @Test
    public void testTimeNearestCloseNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeNearestClose();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeNearestAcceptableRange() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, ResourceInfo.TIME_UNIT, null);

        // setup an acceptable range that's big enough
        setupNearestMatch(WATTEMP, TIME, true, "P1D");
        getAsImage(BASE_PNG_URL + "&time=2008-10-31T08:00:00.000Z", "image/png");
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-10-31T00:00:00.000Z");

        // now one that's not big enough
        setupNearestMatch(WATTEMP, TIME, true, "PT4H/P0D");
        getAsImage(BASE_PNG_URL + "&time=2008-10-31T08:00:00.000Z", "image/png");
        assertNoNearestWarning(getLayerId(WATTEMP), "time");

        // same as above, but with exception on failed match
        setupNearestMatch(WATTEMP, TIME, true, "PT4H/P0D", EXCEPTION, false);
        Document dom = getAsDOM(BASE_PNG_URL + "&time=2008-10-31T08:00:00.000Z");
        String message = checkLegacyException(dom, INVALID_DIMENSION_VALUE, "time");
        assertThat(
                message,
                containsString("No nearest match found on sf:watertemp for time dimension"));

        // now force a search in the future only
        setupNearestMatch(WATTEMP, TIME, true, "P0D/P10D");
        getAsImage(BASE_PNG_URL + "&time=2008-10-31T08:00:00.000Z", "image/png");
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-11-01T00:00:00.000Z");
    }

    @Test
    public void testTimeNearestAcceptableRangeNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeNearestAcceptableRange();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeNearestBefore() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, ResourceInfo.TIME_UNIT, null);
        setupNearestMatch(WATTEMP, TIME, true);

        BufferedImage image = getAsImage(BASE_PNG_URL + "&time=1990-10-31", "image/png");
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-10-31T00:00:00.000Z");

        // same as testTime, it was the first time
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));
    }

    @Test
    public void testTimeNearestBeforeNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeNearestBefore();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeNearestAfter() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, ResourceInfo.TIME_UNIT, null);
        setupNearestMatch(WATTEMP, TIME, true);

        BufferedImage image = getAsImage(BASE_PNG_URL + "&time=2009-10-31", "image/png");
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-11-01T00:00:00.000Z");

        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 187, 187));
    }

    @Test
    public void testTimeNearestAfterNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeNearestAfter();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeTwice() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);

        BufferedImage image =
                getAsImage(BASE_PNG_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));
    }

    @Test
    public void testTimeElevation() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);

        BufferedImage image =
                getAsImage(
                        BASE_PNG_URL
                                + "&time=2008-10-31T00:00:00.000Z&elevation=100&bgcolor=0xFF0000",
                        "image/png");

        // at this elevation the pixel is NODATA -> bgcolor
        assertPixel(image, 36, 31, new Color(255, 0, 0));
        // and this one a light blue, but slightly darker than before
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }

    @Test
    public void testTimeRange() throws Exception {
        setupRasterDimension(TIMERANGES, TIME, LIST, null, null, null);
        setupRasterDimension(TIMERANGES, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(TIMERANGES, "wavelength", LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", LIST, null, null, null);

        // Setting a BLUE Background Color
        String baseUrl =
                "wms?LAYERS="
                        + getLayerId(TIMERANGES)
                        + "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326"
                        + "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&bgcolor=0x0000FF";

        // in the last range, it's bluish
        BufferedImage image =
                getAsImage(
                        baseUrl + "&TIME=2008-11-05T00:00:00.000Z/2008-11-06T12:00:00.000Z",
                        "image/png");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(249, 249, 255));

        // in the middle hole, no data, thus blue
        setExceptionsOnInvalidDimension(false);
        image =
                getAsImage(
                        baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z",
                        "image/png");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, Color.BLUE);

        // same as above, but with more attitude
        setExceptionsOnInvalidDimension(true);
        Document dom =
                getAsDOM(baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z");
        String message = checkLegacyException(dom, INVALID_DIMENSION_VALUE, "time");
        assertThat(
                message,
                containsString(
                        "Could not find a match for 'time' value: '2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z'"));

        // first range, red-ish
        image =
                getAsImage(
                        baseUrl + "&TIME=2008-10-31T12:00:00.000Z/2008-10-31T16:00:00.000Z",
                        "image/png");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(255, 172, 172));
    }

    @Test
    public void testTimeRangeNearestMatch() throws Exception {
        setupRasterDimension(TIMERANGES, TIME, LIST, null, ResourceInfo.TIME_UNIT, null);
        setupRasterDimension(TIMERANGES, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(TIMERANGES, "wavelength", LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", LIST, null, null, null);
        setupNearestMatch(TIMERANGES, TIME, true);

        // Setting a BLUE Background Color
        String timeRangesId = getLayerId(TIMERANGES);
        String baseUrl =
                "wms?LAYERS="
                        + timeRangesId
                        + "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326"
                        + "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&bgcolor=0x0000FF";

        // after last range, as a range
        BufferedImage image = getAsImage(baseUrl + "&TIME=2018-11-8/2018-11-09", "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-11-07T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(249, 249, 255));

        // after last range, as an instant
        image = getAsImage(baseUrl + "&TIME=20018-11-05", "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-11-07T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(249, 249, 255));

        // in the middle hole, but closer to the latest value, as a range
        image =
                getAsImage(
                        baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z",
                        "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-11-05T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(249, 249, 255));

        // in the middle hole, but closer to the latest value, as an instant
        image = getAsImage(baseUrl + "&TIME=2008-11-04T16:00:00.000Z", "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-11-05T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(249, 249, 255));

        // before first range, as a range
        image = getAsImage(baseUrl + "&TIME=2000-10-31/2000-10-31", "image/png");
        assertNearestTimeWarning(timeRangesId, "2008-10-31T00:00:00.000Z");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(255, 172, 172));

        // before first range, as an instant
        image = getAsImage(baseUrl + "&TIME=2000-10-31", "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-10-31T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(255, 172, 172));
    }

    @Test
    public void testTimeRangeNearestMatchNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeRangeNearestMatch();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeRangeNearestMatchAcceptableRange() throws Exception {
        setupRasterDimension(TIMERANGES, TIME, LIST, null, ResourceInfo.TIME_UNIT, null);
        setupRasterDimension(TIMERANGES, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(TIMERANGES, "wavelength", LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", LIST, null, null, null);

        // Setting a BLUE Background Color
        String timeRangesId = getLayerId(TIMERANGES);
        String baseUrl =
                "wms?LAYERS="
                        + timeRangesId
                        + "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326"
                        + "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&bgcolor=0x0000FF";

        // after last range, as a range, large enough acceptable range to find it
        setupNearestMatch(TIMERANGES, TIME, true, "P100Y");
        getAsImage(baseUrl + "&TIME=2018-11-8/2018-11-09", "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-11-07T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");

        // same as above but with an instant
        getAsImage(baseUrl + "&TIME=2018-11-05", "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-11-07T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");

        // after last range, as a range, small enough that it won't be found
        setupNearestMatch(TIMERANGES, TIME, true, "P1D");
        getAsImage(baseUrl + "&TIME=2018-11-8/2018-11-09", "image/png");
        assertWarningCount(2);
        assertNoNearestWarning(timeRangesId, TIME);
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");

        // same as above, but with an instant
        getAsImage(baseUrl + "&TIME=20018-11-05", "image/png");
        assertWarningCount(2);
        assertNoNearestWarning(timeRangesId, TIME);
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");

        // in the middle hole, closer to the latest value, but with a search radius that will match
        // the earlier one
        setupNearestMatch(TIMERANGES, TIME, true, "P1D/P0D");
        getAsImage(
                baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z", "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-11-04T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");

        // same as above, but with an instant
        getAsImage(baseUrl + "&TIME=2008-11-04T16:00:00.000Z", "image/png");
        assertWarningCount(2);
        assertNearestTimeWarning(timeRangesId, "2008-11-04T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");

        // before first range, as a range, with a range that won't allow match
        setupNearestMatch(TIMERANGES, TIME, true, "P1D");
        getAsImage(baseUrl + "&TIME=2000-10-31/2000-10-31", "image/png");
        assertWarningCount(2);
        assertNoNearestWarning(timeRangesId, TIME);
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");

        // same as above, as an instant
        getAsImage(baseUrl + "&TIME=2000-10-31", "image/png");
        assertWarningCount(2);
        assertNoNearestWarning(timeRangesId, TIME);
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");
    }

    @Test
    public void testTimeRangeNearestMatchAcceptableRangeNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeRangeNearestMatchAcceptableRange();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeDefaultAsRange() throws Exception {
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("2008-10-30T23:00:00.000Z/2008-10-31T01:00:00.000Z");
        setupResourceDimensionDefaultValue(WATTEMP, TIME, defaultValueSetting);

        // default time, specific elevation
        // BufferedImage image = getAsImage(BASE_URL +
        // "&time=2008-10-31T00:00:00.000Z&elevation=100", "image/png");
        BufferedImage image = getAsImage(BASE_PNG_URL + "&elevation=100", "image/png");

        // at this elevation the pixel is NODATA, thus becomes white, the bgcolor
        assertPixel(image, 36, 31, new Color(255, 255, 255));
        // and this one a light blue, but slightly darker than before
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }

    @Test
    public void testElevationDefaultAsRange() throws Exception {
        setupRasterDimension(WATTEMP, TIME, LIST, null, null, null);
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("99/101");
        setupResourceDimensionDefaultValue(WATTEMP, ELEVATION, defaultValueSetting);

        // default elevation, specific time
        BufferedImage image =
                getAsImage(BASE_PNG_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // at this elevation the pixel is NODATA -> bgcolor
        assertPixel(image, 36, 31, new Color(255, 255, 255));
        // and this one a light blue, but slightly darker than before
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }

    @Test
    public void testTimeElevationDefaultAsRange() throws Exception {
        // setup a range default for time
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("2008-10-30T23:00:00.000Z/2008-10-31T01:00:00.000Z");
        setupResourceDimensionDefaultValue(WATTEMP, TIME, defaultValueSetting);
        // setup a range default for elevation
        defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("99/101");
        setupResourceDimensionDefaultValue(WATTEMP, ELEVATION, defaultValueSetting);

        // use defaults for both time and elevation
        BufferedImage image = getAsImage(BASE_PNG_URL, "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(255, 255, 255)); // nodata -> bgcolor
        // and this one a light blue, but slightly darker than before
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }

    @Test
    public void testNearestMatchTwoLayers() throws Exception {
        // setup time ranges
        setupRasterDimension(TIMERANGES, TIME, LIST, null, ResourceInfo.TIME_UNIT, null);
        setupRasterDimension(TIMERANGES, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(TIMERANGES, "wavelength", LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", LIST, null, null, null);
        setupNearestMatch(TIMERANGES, TIME, true);

        // setup water temp
        setupRasterDimension(WATTEMP, ELEVATION, LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, TIME, LIST, null, ResourceInfo.TIME_UNIT, null);
        setupNearestMatch(WATTEMP, TIME, true);

        // Setting a BLUE Background Color
        String timeRangesId = getLayerId(TIMERANGES);
        String waterTempId = getLayerId(WATTEMP);
        String baseUrl =
                "wms?LAYERS="
                        + timeRangesId
                        + ","
                        + waterTempId
                        + "&STYLES=,&FORMAT=image%2Fpng"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326"
                        + "&BBOX=-180,-90,180,90&WIDTH=200&HEIGHT=80&bgcolor=0x0000FF";

        // before both
        getAsImage(baseUrl + "&TIME=2000-01-01", "image/png");
        assertWarningCount(4);
        assertNearestTimeWarning(timeRangesId, "2008-10-31T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");
        assertNearestTimeWarning(waterTempId, "2008-10-31T00:00:00.000Z");
        assertDefaultDimensionWarning(waterTempId, ELEVATION, UNITS, "0.0");

        // after both
        getAsImage(baseUrl + "&TIME=2100-01-01", "image/png");
        assertWarningCount(4);
        assertNearestTimeWarning(timeRangesId, "2008-11-07T00:00:00.000Z");
        assertDefaultDimensionWarning(timeRangesId, ELEVATION, UNITS, "20.0");
        assertNearestTimeWarning(waterTempId, "2008-11-01T00:00:00.000Z");
        assertDefaultDimensionWarning(waterTempId, ELEVATION, UNITS, "0.0");
    }

    @Test
    public void testNearestTimes() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, LIST, null, null, null);

        CoverageInfo info = getCatalog().getCoverageByName(TIMESERIES.getLocalPart());
        DimensionInfo dim = (DimensionInfo) info.getMetadata().get(TIME);
        dim.setNearestMatchEnabled(true);
        dim.setAcceptableInterval("P2D");
        getCatalog().save(info);

        TimeParser parser = new TimeParser();
        @SuppressWarnings("unchecked")
        List<Object> queryRanges = (List<Object>) parser.parse("2014-01-01/2019-01-01/P1Y");
        // 1Y Period is 365.25 days so the parsing will result in days not aligned to 1st of January
        // at 00:00:00

        // Add a duplicate
        @SuppressWarnings("unchecked")
        Date duplicate = (Date) ((List<Object>) parser.parse("2014-01-01T00:00:00.000Z")).get(0);
        queryRanges.add(duplicate);
        assertEquals(7, queryRanges.size());

        WMS wms = new WMS(getGeoServer());
        TreeSet<Date> times = wms.queryCoverageNearestMatchTimes(info, queryRanges);
        assertEquals(6, times.size());
        for (int i = 2014; i < 2020; i++) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) parser.parse(i + "-01-01T00:00:00.000Z");
            Date date = (Date) list.get(0);
            assertTrue(times.contains(date));
        }
    }
}
