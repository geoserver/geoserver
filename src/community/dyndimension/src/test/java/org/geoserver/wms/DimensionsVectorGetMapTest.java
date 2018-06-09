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
import org.geoserver.wms.dimension.DefaultValueConfiguration;
import org.geoserver.wms.dimension.DefaultValueConfiguration.DefaultValuePolicy;
import org.junit.Before;
import org.junit.Test;

public class DimensionsVectorGetMapTest extends WMSDynamicDimensionTestSupport {

    String baseGetMap;

    @Before
    public void setup() throws Exception {
        baseGetMap =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90"
                        + "&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326&layers="
                        + getLayerId(V_TIME_ELEVATION);
    }

    @Test
    public void testNoDimension() throws Exception {
        BufferedImage image = getAsImage(baseGetMap, "image/png");

        // we should get everything black, all four squares
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testBothDimensionsStaticDefaults() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);

        BufferedImage image = getAsImage(baseGetMap, "image/png");

        // we should get everything white, none of the squares is coming back
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeDynamicRestriction() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(ResourceInfo.TIME, DefaultValuePolicy.LIMIT_DOMAIN));

        BufferedImage image = getAsImage(baseGetMap + "&elevation=1.0", "image/png");

        // this select the second feature
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeExpressionFull() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN),
                new DefaultValueConfiguration(
                        ResourceInfo.TIME, "Concatenate('2011-05-0', round(elevation + 1))"));

        BufferedImage image = getAsImage(baseGetMap, "image/png");

        // this select the first feature
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeExpressionSingleElevation() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.TIME, "Concatenate('2011-05-0', round(elevation + 1))"));

        BufferedImage image = getAsImage(baseGetMap + "&elevation=1.0", "image/png");

        // elevation = 1.0 -> second feature
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testElevationDynamicRestriction() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN));

        BufferedImage image = getAsImage(baseGetMap + "&time=2011-05-02", "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testExplicitDefaultTime() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(ResourceInfo.TIME, DefaultValuePolicy.LIMIT_DOMAIN));

        BufferedImage image = getAsImage(baseGetMap + "&elevation=1.0&time=current", "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testExplicitDefaultElevation() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN));

        BufferedImage image = getAsImage(baseGetMap + "&elevation=&time=2011-05-03", "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.WHITE);
    }
}
