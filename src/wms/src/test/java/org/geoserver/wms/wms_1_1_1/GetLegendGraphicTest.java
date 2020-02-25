/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URLEncoder;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.Converters;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetLegendGraphicTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addStyle("paramFill", "paramFill.sld", getClass(), catalog);
        testData.addStyle("paramStroke", "paramStroke.sld", getClass(), catalog);
        testData.addStyle("raster", "raster.sld", getClass(), catalog);
        testData.addStyle("rasterScales", "rasterScales.sld", getClass(), catalog);
        testData.addStyle("Population", "Population.sld", getClass(), catalog);
        testData.addStyle("uom", "uomStroke.sld", getClass(), catalog);
        testData.addStyle("scaleDependent", "scaleDependent.sld", getClass(), catalog);

        testData.addVectorLayer(
                new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                Collections.EMPTY_MAP,
                "states.properties",
                getClass(),
                catalog);

        LegendInfo legend = new LegendInfoImpl();
        legend.setWidth(22);
        legend.setHeight(22);
        legend.setFormat("image/png");
        legend.setOnlineResource("legend.png");
        File file = getResourceLoader().createFile("styles", "legend.png");
        getResourceLoader().copyFromClassPath("../legend.png", file, getClass());
        testData.addStyle(null, "custom", "point_test.sld", getClass(), catalog, legend);
        // add a raster_legend style with legend to test on raster
        testData.addStyle(null, "raster_legend", "raster.sld", getClass(), catalog, legend);
        // setup a ws specific style with custom legend too
        WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
        File wsFile =
                getResourceLoader()
                        .createFile(
                                "workspaces", defaultWorkspace.getName(), "styles", "legend.png");
        getResourceLoader().copyFromClassPath("../legend.png", wsFile, getClass());
        testData.addStyle(
                defaultWorkspace, "wsCustom", "point_test.sld", getClass(), catalog, legend);
    }

    /**
     * Tests GML output does not break when asking for an area that has no data with GML feature
     * bounding enabled
     */
    @Test
    public void testPlain() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                + "&layer="
                                + getLayerId(MockData.LAKES)
                                + "&style=Lakes"
                                + "&format=image/png&width=20&height=20",
                        "image/png");
        assertPixel(image, 10, 10, Converters.convert("#4040C0", Color.class));
    }

    /**
     * Tests GML output does not break when asking for an area that has no data with GML feature
     * bounding enabled
     */
    @Test
    public void testEnv() throws Exception {
        // no params, use fallback
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + getLayerId(MockData.LAKES)
                        + "&style=paramFill"
                        + "&format=image/png&width=20&height=20";
        BufferedImage image = getAsImage(base, "image/png");
        assertPixel(image, 10, 10, Converters.convert("#FFFFFF", Color.class));

        // specify color explicitly
        image = getAsImage(base + "&env=color:#FF0000", "image/png");
        assertPixel(image, 10, 10, Converters.convert("#FF0000", Color.class));
    }

    /** Tests an custom legend graphic */
    @Test
    public void testCustomLegend() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=custom"
                        + "&format=image/png&width=22&height=22";

        BufferedImage image = getAsImage(base, "image/png");
        Resource resource = getResourceLoader().get("styles/legend.png");
        BufferedImage expected = ImageIO.read(resource.file());

        assertEquals(getPixelColor(expected, 10, 2).getRGB(), getPixelColor(image, 10, 2).getRGB());

        // test external image dimensions
        base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=custom"
                        + "&format=image/png";

        image = getAsImage(base, "image/png");

        assertEquals("width", image.getWidth(), expected.getWidth());
        assertEquals("height", image.getHeight(), expected.getHeight());

        Color expectedColor = getPixelColor(expected, 11, 11);
        Color actualColor = getPixelColor(image, 11, 11);
        assertEquals("red", expectedColor.getRed(), actualColor.getRed());
        assertEquals("green", expectedColor.getGreen(), actualColor.getGreen());
        assertEquals("blue", expectedColor.getBlue(), actualColor.getBlue());
        assertEquals("alpha", expectedColor.getAlpha(), actualColor.getAlpha());

        // test rescale
        base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=custom"
                        + "&format=image/png&width=16&height=16";

        image = getAsImage(base, "image/png");

        expectedColor = getPixelColor(expected, 11, 11);
        actualColor = getPixelColor(image, 8, 8);
        assertEquals("red", expectedColor.getRed(), actualColor.getRed());
        assertEquals("green", expectedColor.getGreen(), actualColor.getGreen());
        assertEquals("blue", expectedColor.getBlue(), actualColor.getBlue());
        assertEquals("alpha", expectedColor.getAlpha(), actualColor.getAlpha());
    }

    /** Tests a custom legend graphic with a workspace specific style */
    @Test
    public void testCustomLegendWsSpecific() throws Exception {
        String wsName = getCatalog().getDefaultWorkspace().getName();
        String styleName = wsName + ":wsCustom";
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states"
                        + "&format=image/png&width=22&height=22&style="
                        + styleName;

        BufferedImage image = getAsImage(base, "image/png");

        Resource resource = getResourceLoader().get("/workspaces/" + wsName + "/styles/legend.png");
        BufferedImage expected = ImageIO.read(resource.file());
        ImageAssert.assertEquals(expected, image, 0);
    }

    /** Tests an custom legend graphic */
    @Test
    public void testCustomLegendOnDefaultStyle() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo statesLayer = catalog.getLayerByName("states");
        StyleInfo statesDefaultStyle = statesLayer.getDefaultStyle();
        StyleInfo customStyle = catalog.getStyleByName("custom");
        StyleInfo wsCustomStyle = catalog.getStyleByName("wsCustom");

        Resource resource = getResourceLoader().get("styles/legend.png");
        BufferedImage expected = ImageIO.read(resource.file());
        try {
            // alter the default style
            statesLayer.setDefaultStyle(customStyle);
            catalog.save(statesLayer);

            // get the default legend graphic, it should be the custom image
            BufferedImage image =
                    getAsImage(
                            "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                    + "&layer=sf:states&format=image/png&width=22&height=22",
                            "image/png");
            ImageAssert.assertEquals(expected, image, 0);

            // the above again, but setting the workspace specific style
            statesLayer.setDefaultStyle(wsCustomStyle);
            catalog.save(statesLayer);
            image =
                    getAsImage(
                            "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                    + "&layer=sf:states&format=image/png&width=22&height=22",
                            "image/png");
            ImageAssert.assertEquals(expected, image, 0);
        } finally {
            statesLayer.setDefaultStyle(statesDefaultStyle);
            catalog.save(statesLayer);
        }
    }

    /** Tests an unscaled states legend */
    @Test
    public void testStatesLegend() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=Population"
                        + "&format=image/png&width=20&height=20";
        BufferedImage image = getAsImage(base, "image/png");

        // check RGB is in the expected positions
        assertPixel(image, 10, 10, Color.RED);
        assertPixel(image, 10, 30, Color.GREEN);
        assertPixel(image, 10, 50, Color.BLUE);
    }

    /** Tests a dpi rescaled legend */
    @Test
    public void testStatesLegendDpiRescaled() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=Population"
                        + "&format=image/png&width=20&height=20&legend_options=dpi:180";
        BufferedImage image = getAsImage(base, "image/png");

        assertPixel(image, 20, 20, Color.RED);
        assertPixel(image, 20, 60, Color.GREEN);
        assertPixel(image, 20, 100, Color.BLUE);
        Color linePixel = getPixelColor(image, 20, 140);
        assertTrue(linePixel.getRed() < 10);
        assertTrue(linePixel.getGreen() < 10);
        assertTrue(linePixel.getBlue() < 10);
    }

    /** Tests a dpi rescaled legend with specific rule name */
    @Test
    public void testStatesLegendDpiRescaledSingleRule() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=Population"
                        + "&format=image/png&width=20&height=20&legend_options=dpi:180&rule=2-4M";
        BufferedImage image = getAsImage(base, "image/png");

        // ImageIO.write(image, "PNG", new java.io.File("/tmp/rule.png"));

        // just one rule
        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());

        // the red one, big
        assertPixel(image, 20, 20, Color.RED);
    }

    /** Tests a uom rescaled legend */
    @Test
    public void testStatesLegendUomRescaled() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=uom"
                        + "&format=image/png&width=20&height=20&scale=1000000";
        BufferedImage image = getAsImage(base, "image/png");

        assertPixel(image, 10, 10, Color.BLUE);
        assertPixel(image, 5, 10, Color.WHITE);
        assertPixel(image, 1, 10, Color.WHITE);

        // halve the scale denominator, we're zooming in, the thickness should double
        base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=uom"
                        + "&format=image/png&width=20&height=20&scale=500000";
        image = getAsImage(base, "image/png");

        assertPixel(image, 10, 10, Color.BLUE);
        assertPixel(image, 5, 10, Color.BLUE);
        assertPixel(image, 1, 10, Color.WHITE);
    }

    /** Tests a dpi _and_ uom rescaled image */
    @Test
    public void testStatesLegendDpiUomRescaled() throws Exception {
        // halve the scale denominator, we're zooming in, the thickness should double
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=uom"
                        + "&format=image/png&width=20&height=20&scale=1000000&&legend_options=dpi:180";
        BufferedImage image = getAsImage(base, "image/png");

        assertPixel(image, 30, 10, Color.BLUE);
        assertPixel(image, 20, 20, Color.BLUE);
        assertPixel(image, 10, 30, Color.BLUE);

        // this assertion check that the thickness is not greater than twice the previous value
        assertPixel(image, 24, 6, Color.WHITE);

        assertPixel(image, 1, 20, Color.WHITE);
    }

    @Test
    public void testEntityExpansionSldBody() throws Exception {
        String base =
                "wms?LEGEND_OPTIONS=forceLabels:on&REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=200&HEIGHT=20&LAYER="
                        + getLayerId(MockData.POLYGONS)
                        + "&SLD_BODY=";
        String sld = IOUtils.toString(TestData.class.getResource("externalEntities.sld"), "UTF-8");
        MockHttpServletResponse response =
                getAsServletResponse(base + URLEncoder.encode(sld, "UTF-8"));
        // should fail with an error message poiting at entity resolution
        assertEquals("application/vnd.ogc.se_xml", response.getContentType());
        final String content = response.getContentAsString();
        assertThat(content, containsString("Entity resolution disallowed"));
        assertThat(content, containsString("/this/file/does/not/exist"));
    }

    @Test
    public void testNoLegendBelowMinScaleDenominator() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                + "&layer="
                                + getLayerId(MockData.LAKES)
                                + "&style=scaleDependent"
                                + "&format=image/png&width=20&height=20&scale=5000",
                        "image/png");
        assertEquals(1, image.getHeight());
    }

    @Test
    public void testNoLegendAboveMinScaleDenominator() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                + "&layer="
                                + getLayerId(MockData.LAKES)
                                + "&style=scaleDependent"
                                + "&format=image/png&width=20&height=20&scale=150000",
                        "image/png");
        assertEquals(1, image.getHeight());
    }

    @Test
    public void testLegendHeight() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=sf:states&style=Population"
                        + "&format=image/png&width=20&height=20";
        BufferedImage image = getAsImage(base, "image/png");
        assertEquals(80, image.getHeight());
    }

    /**
     * Test for GEOS-7636 GetLegendGraphic ignores the OnlineResource configured in the style for
     * raster layers
     */
    @Test
    public void testLegendOnRaster() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=wcs:World&style=raster_legend"
                        + "&format=image/png&width=22&height=22";

        BufferedImage image = getAsImage(base, "image/png");
        Resource resource = getResourceLoader().get("styles/legend.png");
        BufferedImage expected = ImageIO.read(resource.file());

        assertEquals(getPixelColor(expected, 10, 2).getRGB(), getPixelColor(image, 10, 2).getRGB());
    }

    @Test
    public void testJpegRasterLegend() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=wcs:World&style=raster"
                        + "&format=image/jpeg&width=32&height=32";

        BufferedImage image = getAsImage(base, "image/jpeg");
        BufferedImage expected =
                ImageIO.read(getClass().getResourceAsStream("../rasterLegend.png"));

        ImageAssert.assertEquals(expected, image, 0);
    }
}
