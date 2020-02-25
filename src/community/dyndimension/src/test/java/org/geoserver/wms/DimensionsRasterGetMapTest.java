/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.junit.Test;

public class DimensionsRasterGetMapTest extends WMSDimensionsTestSupport {

    static final String BASE_URL =
            "wms?service=WMS&version=1.1.0"
                    + "&request=GetMap&layers=watertemp&styles="
                    + "&bbox=0.237,40.562,14.593,44.558&width=200&height=80"
                    + "&srs=EPSG:4326&format=image/png";
    static final String MIME = "image/png";

    @Test
    public void testNoDimension() throws Exception {
        BufferedImage image = getAsImage(BASE_URL, MIME);

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

        BufferedImage image = getAsImage(BASE_URL, "image/png");

        // should be light red pixel and the first pixel is there only at the default elevation
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 185, 185));
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

        BufferedImage image = getAsImage(BASE_URL + "&elevation=100", "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0, 0, 0));
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

        BufferedImage image = getAsImage(BASE_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));
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

        BufferedImage image = getAsImage(BASE_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));
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
                getAsImage(BASE_URL + "&time=2008-10-31T00:00:00.000Z&elevation=100", "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0, 0, 0));
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

        String baseUrl =
                "wms?LAYERS="
                        + getLayerId(TIMERANGES)
                        + "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326"
                        + "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&bgcolor=0xFFFFFF";

        // in the last range, it's bluish
        BufferedImage image =
                getAsImage(
                        baseUrl + "&TIME=2008-11-05T00:00:00.000Z/2008-11-06T12:00:00.000Z",
                        "image/png");
        assertPixel(image, 36, 31, new Color(0, 0, 0));
        assertPixel(image, 68, 72, new Color(249, 249, 255));

        // in the middle hole, no data, thus white
        image =
                getAsImage(
                        baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z",
                        "image/png");
        assertPixel(image, 36, 31, Color.WHITE);
        assertPixel(image, 68, 72, Color.WHITE);

        // first range, red-ish
        image =
                getAsImage(
                        baseUrl + "&TIME=2008-10-31T12:00:00.000Z/2008-10-31T16:00:00.000Z",
                        "image/png");
        assertPixel(image, 36, 31, Color.BLACK);
        assertPixel(image, 68, 72, new Color(255, 170, 170));
    }
}
