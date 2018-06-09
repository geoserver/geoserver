/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.GetMap;
import org.geoserver.wms.GetMapCallback;
import org.geoserver.wms.GetMapCallbackAdapter;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

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
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        BufferedImage image = getAsImage(BASE_PNG_URL, "image/png");

        // should be light red pixel and the first pixel is there only at the default elevation
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 187, 187));
    }

    /** Same as above, but obtained via sorting on one attribute instead of using both dimensions */
    @Test
    public void testSortTimeDescending() throws Exception {
        // setting up only elevation, the time will be picked by sorting
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);

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
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        BufferedImage image = getAsImage(BASE_PNG_URL + "&elevation=100", "image/png");

        // at this elevation the pixel is NODATA -> bgcolor
        assertPixel(image, 36, 31, new Color(255, 255, 255));
        // and this one a light blue
        assertPixel(image, 68, 72, new Color(246, 246, 255));
    }

    /** Same as above, but obtained via sorting instead of using dimensions */
    @Test
    public void testSortElevationDescending() throws Exception {
        BufferedImage image =
                getAsImage(
                        BASE_PNG_URL + "&bgcolor=0xFF0000&sortBy=elevation D,ingestion D",
                        "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(255, 0, 0));
        // and this one a light blue
        assertPixel(image, 68, 72, new Color(246, 246, 255));
    }

    @Test
    public void testTime() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        BufferedImage image =
                getAsImage(BASE_PNG_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));
    }

    @Test
    public void testTimeAnimation() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        List<BufferedImage> images =
                getAsAnimation(
                        BASE_URL + "&time=2008-10-01/2008-11-31&format=image/gif;subtype=animated",
                        "image/gif");
        assertEquals(2, images.size());
        BufferedImage imageOctober = images.get(0);
        BufferedImage imageNovember = images.get(1);

        // this should be the same as "testTime"
        assertPixel(imageOctober, 36, 31, new Color(246, 246, 255));
        assertPixel(imageOctober, 68, 72, new Color(255, 181, 181));

        // this should be the same as testDefault
        assertPixel(imageNovember, 36, 31, new Color(246, 246, 255));
        assertPixel(imageNovember, 68, 72, new Color(255, 187, 187));
    }

    @Test
    public void testTimeAnimationTimeout() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        // setup a short timeout
        final int TIMEOUT_MS = 10;
        final GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getMetadata().put(WMS.MAX_RENDERING_TIME, String.valueOf(TIMEOUT_MS));
        gs.save(wms);

        // make extra sure we are going to take more than that
        GetMap getMap = GeoServerExtensions.bean(GetMap.class);
        List<GetMapCallback> originalCallbacks =
                GeoServerExtensions.extensions(GetMapCallback.class);

        GetMapCallback timeoutCallback =
                new GetMapCallbackAdapter() {
                    @Override
                    public WMSMapContent beforeRender(WMSMapContent mapContent) {

                        try {
                            Thread.sleep(TIMEOUT_MS * 2);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return super.beforeRender(mapContent);
                    }
                };
        try {
            getMap.setGetMapCallbacks(Arrays.asList(timeoutCallback));

            // run the request that will time out
            MockHttpServletResponse resp =
                    getAsServletResponse(
                            BASE_URL
                                    + "&time=2008-10-01/2008-11-31&format=image/gif;subtype=animated");
            assertEquals("application/vnd.ogc.se_xml", resp.getContentType());
            assertTrue(resp.getContentAsString().contains("This animation request used more time"));
        } finally {
            wms.getMetadata().remove(WMS.MAX_RENDERING_TIME);
            gs.save(wms);
            getMap.setGetMapCallbacks(originalCallbacks);
        }
    }

    @Test
    public void testElevationAnimation() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        List<BufferedImage> images =
                getAsAnimation(
                        BASE_URL + "&elevation=-100/500&format=image/gif;subtype=animated",
                        "image/gif");
        assertEquals(2, images.size());
        BufferedImage image0 = images.get(0);
        BufferedImage image100 = images.get(1);

        // this should be the same as "testElevatin"
        assertPixel(image100, 36, 31, new Color(255, 255, 255)); // nodata -> bgcolor
        assertPixel(image100, 68, 72, new Color(246, 246, 255));

        // this should be the same as testDefault
        assertPixel(image0, 36, 31, new Color(246, 246, 255));
        assertPixel(image0, 68, 72, new Color(255, 187, 187));
    }

    @Test
    public void testTimeTwice() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        BufferedImage image =
                getAsImage(BASE_PNG_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));
    }

    @Test
    public void testTimeElevation() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

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
        setupRasterDimension(
                TIMERANGES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        setupRasterDimension(
                TIMERANGES,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                TIMERANGES, "wavelength", DimensionPresentation.LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", DimensionPresentation.LIST, null, null, null);

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
        image =
                getAsImage(
                        baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z",
                        "image/png");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, Color.BLUE);

        // first range, red-ish
        image =
                getAsImage(
                        baseUrl + "&TIME=2008-10-31T12:00:00.000Z/2008-10-31T16:00:00.000Z",
                        "image/png");
        assertPixel(image, 36, 31, Color.BLUE);
        assertPixel(image, 68, 72, new Color(255, 172, 172));
    }

    @Test
    public void testTimeDefaultAsRange() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("2008-10-30T23:00:00.000Z/2008-10-31T01:00:00.000Z");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.TIME, defaultValueSetting);

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
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("99/101");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.ELEVATION, defaultValueSetting);

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
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.TIME, defaultValueSetting);
        // setup a range default for elevation
        defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("99/101");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.ELEVATION, defaultValueSetting);

        // use defaults for both time and elevation
        BufferedImage image = getAsImage(BASE_PNG_URL, "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(255, 255, 255)); // nodata -> bgcolor
        // and this one a light blue, but slightly darker than before
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }
}
