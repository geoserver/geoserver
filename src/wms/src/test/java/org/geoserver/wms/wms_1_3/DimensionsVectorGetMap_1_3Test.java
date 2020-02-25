/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Test;

public class DimensionsVectorGetMap_1_3Test extends WMSDimensionsTestSupport {

    @Test
    public void testCustomDimensionNumber() throws Exception {
        setupVectorDimension(
                "dim_custom", "elevation", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.3.0&request=GetMap"
                                + "&bbox=-90,-180,90,180&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
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
    public void testCustomDimensionNumberRange() throws Exception {
        setupVectorDimension(
                "dim_custom", "elevation", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.3.0&request=GetMap"
                                + "&bbox=-90,-180,90,180&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
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
                        "wms?service=WMS&version=1.3.0&request=GetMap"
                                + "&bbox=-90,-180,90,180&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
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
                        "wms?service=WMS&version=1.3.0&request=GetMap"
                                + "&bbox=-90,-180,90,180&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
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
                        "wms?service=WMS&version=1.3.0&request=GetMap"
                                + "&bbox=-90,-180,90,180&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
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
                        "wms?service=WMS&version=1.3.0&request=GetMap"
                                + "&bbox=-90,-180,90,180&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
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
