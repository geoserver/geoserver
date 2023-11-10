/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.geoserver.catalog.DimensionInfo.NearestFailBehavior.EXCEPTION;
import static org.geoserver.platform.ServiceException.INVALID_DIMENSION_VALUE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DimensionsVectorGetMapTest extends WMSDimensionsTestSupport {

    @Test
    public void testNoDimension() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90"
                                + "&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326&layers="
                                + getLayerId(V_TIME_ELEVATION),
                        "image/png");

        // we should get everything black, all four squares
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testElevationDefault() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION),
                        "image/png");

        // we should get only the first
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testElevationSingle() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&elevation=1.0",
                        "image/png");

        // we should get only the second
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testElevationListMulti() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&elevation=1.0,3.0",
                        "image/png");

        // we should get second and third
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testElevationListExtra() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&elevation=1.0,3.0,5.0",
                        "image/png");

        // we should get only second and third
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testElevationInterval() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&elevation=1.0/3.0",
                        "image/png");

        // we should get last three
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testElevationIntervalResolution() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&elevation=0.0/4.0/2.0",
                        "image/png");

        // we should get second and fourth
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testElevationIntervalResolutionTooManyDefault() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&elevation=0.0/4.0/0.01");

        assertEquals("application/vnd.ogc.se_xml", getBaseMimeType(response.getContentType()));
        Document dom = dom(response, true);
        // print(dom);
        String text =
                checkLegacyException(dom, ServiceException.INVALID_PARAMETER_VALUE, "elevation");
        assertThat(
                text,
                containsString(
                        "More than "
                                + DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES
                                + " elevations"));
    }

    @Test
    public void testElevationIntervalResolutionTooManyCustom() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setMaxRequestedDimensionValues(2);
        gs.save(wms);
        try {
            // adding a extra elevation that is simply not there, should not break
            setupVectorDimension(
                    ResourceInfo.ELEVATION,
                    "elevation",
                    DimensionPresentation.LIST,
                    null,
                    UNITS,
                    UNIT_SYMBOL);
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wms?service=WMS&version=1.1.1&request=GetMap"
                                    + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                    + "&layers="
                                    + getLayerId(V_TIME_ELEVATION)
                                    + "&elevation=0.0/4.0/0.01");

            assertEquals("application/vnd.ogc.se_xml", getBaseMimeType(response.getContentType()));
            Document dom = dom(response, true);
            // print(dom);
            String text =
                    checkLegacyException(
                            dom, ServiceException.INVALID_PARAMETER_VALUE, "elevation");
            assertThat(text, containsString("More than 2 elevations"));
        } finally {
            wms.setMaxRequestedDimensionValues(
                    DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES);
            gs.save(wms);
        }
    }

    @Test
    public void testTimeDefault() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION),
                        "image/png");

        // we should get only the last one (current)
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeCurrent() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=CURRENT",
                        "image/png");

        // we should get only the last one (current)
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeCurrentForEmptyLayer() throws Exception {
        setupVectorDimension(
                "TimeElevationEmpty",
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                null,
                null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION_EMPTY)
                                + "&time=CURRENT",
                        "image/png");

        // we should get only the last one (current)
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeSingle() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-02",
                        "image/png");

        // we should get only the second
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeSingleNoNearestClose() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-02T01:00:00Z",
                        "image/png");

        // not an exact match, should not get anything
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeSingleNearestClose() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-02T01:00:00Z",
                        "image/png");
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-02T00:00:00.000Z");

        // we should get only the second (nearest match)
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeSingleNearestAcceptableRange() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);

        String baseURL =
                "wms?service=WMS&version=1.1.1&request=GetMap"
                        + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                        + "&layers="
                        + getLayerId(V_TIME_ELEVATION);

        // big enough range
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true, "P1D");
        getAsImage(baseURL + "&time=2011-05-02T01:00:00Z", "image/png");
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-02T00:00:00.000Z");

        // too small range, won't match
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true, "PT1M");
        getAsImage(baseURL + "&time=2011-05-02T01:00:00Z", "image/png");
        assertWarningCount(1);
        assertNoNearestWarning(getLayerId(V_TIME_ELEVATION), ResourceInfo.TIME);

        // same as above, but with exception on failed match
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true, "PT1M", EXCEPTION, false);
        Document dom = getAsDOM(baseURL + "&time=2011-05-02T01:00:00Z");
        String message = checkLegacyException(dom, INVALID_DIMENSION_VALUE, "time");
        assertThat(
                message,
                containsString("No nearest match found on sf:TimeElevation for time dimension"));

        // big enough towards future
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true, "PT0M/P1D");
        getAsImage(baseURL + "&time=2011-05-02T01:00:00Z", "image/png");
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-03T00:00:00.000Z");
    }

    @Test
    public void testTimeSingleNearestAfter() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2013-05-02",
                        "image/png");

        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-04T00:00:00.000Z");

        // we should get only the last
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeSingleNearestBefore() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=1990-05-02",
                        "image/png");

        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-01T00:00:00.000Z");

        // we should get only the first
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeListMulti() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-02,2011-05-04",
                        "image/png");

        // we should get only second and fourth
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeListExtra() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-02,2011-05-04,2011-05-10",
                        "image/png");

        // we should get only second and fourth
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeInterval() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-02/2011-05-05",
                        "image/png");

        // last three
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeIntervalResolution() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-01/2011-05-04/P2D",
                        "image/png");

        // first and third
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeIntervalResolutionTooManyDefault() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&time=2011-05-01/2011-06-01/PT1H");
        assertEquals("application/vnd.ogc.se_xml", getBaseMimeType(response.getContentType()));
        Document dom = dom(response, true);
        // print(dom);
        String text = checkLegacyException(dom, ServiceException.INVALID_PARAMETER_VALUE, "time");
        assertThat(
                text,
                containsString(
                        "More than "
                                + DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES
                                + " times"));
    }

    @Test
    public void testTimeIntervalResolutionTooManyCustom() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setMaxRequestedDimensionValues(2);
        gs.save(wms);
        try {
            setupVectorDimension(
                    ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wms?service=WMS&version=1.1.1&request=GetMap"
                                    + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                    + "&layers="
                                    + getLayerId(V_TIME_ELEVATION)
                                    + "&time=2011-05-01/2011-05-04/P1D",
                            "image/png");

            assertEquals("application/vnd.ogc.se_xml", getBaseMimeType(response.getContentType()));
            Document dom = dom(response, true);
            // print(dom);
            String text =
                    checkLegacyException(dom, ServiceException.INVALID_PARAMETER_VALUE, "time");
            assertThat(text, containsString("More than 2 times"));
        } finally {
            wms.setMaxRequestedDimensionValues(
                    DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES);
            gs.save(wms);
        }
    }

    @Test
    public void testElevationDefaultAsRange() throws Exception {
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("1/3");
        setupResourceDimensionDefaultValue(
                V_TIME_ELEVATION, ResourceInfo.ELEVATION, defaultValueSetting, "elevation");

        // request with default values
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION),
                        "image/png");

        // RenderedImageBrowser.showChain(image);

        // the last three show up, the first does not
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeDefaultAsRange() throws Exception {
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("2011-05-02/2011-05-03");
        setupResourceDimensionDefaultValue(
                V_TIME_ELEVATION, ResourceInfo.TIME, defaultValueSetting, "time");

        // request with default values
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION),
                        "image/png");

        // RenderedImageBrowser.showChain(image);

        // the last three show up, the first does not
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testSortAllAscending() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION_STACKED)
                                + "&sortBy=time, elevation",
                        "image/png");

        // all blue
        assertPixel(image, 20, 10, Color.BLUE);
    }

    @Test
    public void testSortTimeAElevationD() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION_STACKED)
                                + "&sortBy=time, elevation D",
                        "image/png");

        // all blue
        assertPixel(image, 20, 10, Color.GREEN);
    }

    @Test
    public void testSortTimeDElevationA() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION_STACKED)
                                + "&sortBy=time D, elevation",
                        "image/png");

        // all blue
        assertPixel(image, 20, 10, Color.RED);
    }

    @Test
    public void testSortDescending() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION_STACKED)
                                + "&sortBy=time D,elevation D",
                        "image/png");

        // all black
        assertPixel(image, 20, 10, Color.BLACK);
    }

    @Test
    public void testSortInvalidAttribute() throws Exception {
        Document dom =
                getAsDOM(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION_STACKED)
                                + "&sortBy=foo");
        // print(dom);
        XpathEngine xp = XMLUnit.newXpathEngine();
        assertEquals(
                ServiceException.INVALID_PARAMETER_VALUE,
                xp.evaluate("/ServiceExceptionReport/ServiceException/@code", dom));
        assertEquals(
                "sortBy", xp.evaluate("/ServiceExceptionReport/ServiceException/@locator", dom));
        assertThat(
                xp.evaluate("/ServiceExceptionReport/ServiceException", dom),
                containsString("'foo'"));
    }

    @Test
    public void testSortDescendingMultiLayer() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(SystemTestData.LAKES)
                                + ","
                                + getLayerId(V_TIME_ELEVATION_STACKED)
                                + "&sortBy=()(time D,elevation D)",
                        "image/png");

        // all black
        assertPixel(image, 20, 10, Color.BLACK);
    }

    @Test
    public void testCustomDimension() throws Exception {
        setupVectorDimension(
                "dim_custom",
                //    			ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&dim_custom=1.0",
                        "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testCustomDimensionRange() throws Exception {
        setupVectorDimension(
                "dim_custom", "elevation", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&dim_custom=0.5/1.5",
                        "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testCustomDimensionString() throws Exception {
        setupVectorDimension(
                "dim_custom", "shared_key", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&dim_custom=str1",
                        "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testCustomDimensionBoolean() throws Exception {
        setupVectorDimension(
                "dim_custom", "enabled", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&dim_custom=true",
                        "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testCustomDimensionDate() throws Exception {
        setupVectorDimension(
                "dim_custom", "time", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&dim_custom=2011-05-02",
                        "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testCustomDimensionDateRange() throws Exception {
        setupVectorDimension(
                "dim_custom", "time", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetMap"
                                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                + "&layers="
                                + getLayerId(V_TIME_ELEVATION)
                                + "&dim_custom=2011-05-01T20:00:00Z/2011-05-02T05:00:00Z",
                        "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }
}
