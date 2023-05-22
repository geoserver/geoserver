/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.legendgraphic.LegendGraphicBuilder;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.Converters;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GetLegendGraphicTest extends WMSTestSupport {

    public static final QName SF_STATES = new QName(MockData.SF_URI, "states", MockData.SF_PREFIX);
    private static final String SF_STATES_ID = "sf:states";
    private static final String FOOTPRINTS_STYLE = "footprints";

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
        testData.addStyle(FOOTPRINTS_STYLE, "footprints.sld", getClass(), catalog);

        testData.addVectorLayer(
                SF_STATES, Collections.emptyMap(), "states.properties", getClass(), catalog);

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

    @After
    public void cleanMemoryLimit() throws Exception {
        setMemoryLimit(0);
    }

    void setMemoryLimit(int kb) throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setMaxRequestMemory(kb);
        gs.save(wms);
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
     * Tests that GetLegendGraphic works with a style that has a non-process function
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testFootprints() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                + "&layer="
                                + getLayerId(MockData.LAKES)
                                + "&style="
                                + FOOTPRINTS_STYLE
                                + "&format=image/png&width=20&height=20",
                        "image/png");
        assertPixel(image, 10, 10, Converters.convert("#d4d4d4", Color.class));
    }

    @Test
    public void testPlainMemoryLimit() throws Exception {
        // 1kb, not enough for even the smallest image
        setMemoryLimit(1);
        Document dom =
                getAsDOM(
                        "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                + "&layer="
                                + getLayerId(MockData.LAKES)
                                + "&style=Lakes"
                                + "&format=image/png&width=20&height=20");
        String message = checkLegacyException(dom, ServiceException.MAX_MEMORY_EXCEEDED, null);
        assertEquals(LegendGraphicBuilder.MEMORY_USAGE_EXCEEDED, message.trim());
    }

    @Test
    public void testHighMemoryLimit() throws Exception {
        // going to increase the size of the image sample until we get OOM
        String template =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + getLayerId(MockData.LAKES)
                        + "&style=Lakes"
                        + "&format=image/png&width=%d&height=%d";

        // 8 MB limit, a 2000x2000 uses 16MB
        setMemoryLimit(8196);
        int[] sizes = {20, 200, 1000, 2000};
        for (int size : sizes) {
            MockHttpServletResponse response =
                    getAsServletResponse(String.format(template, size, size));
            if (size < 2000) {
                assertEquals("image/png", getBaseMimeType(response.getContentType()));
            } else {
                assertEquals(
                        "application/vnd.ogc.se_xml", getBaseMimeType(response.getContentType()));
                Document dom = dom(response, true);
                String message =
                        checkLegacyException(dom, ServiceException.MAX_MEMORY_EXCEEDED, null);
                assertEquals(LegendGraphicBuilder.MEMORY_USAGE_EXCEEDED, message.trim());
            }
        }
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
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=custom"
                        + "&format=image/png&width=22&height=22";

        BufferedImage image = getAsImage(base, "image/png");
        Resource resource = getResourceLoader().get("styles/legend.png");
        BufferedImage expected = ImageIO.read(resource.file());

        assertEquals(getPixelColor(expected, 10, 2).getRGB(), getPixelColor(image, 10, 2).getRGB());

        // test external image dimensions
        base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=custom"
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
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=custom"
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
                        + "&layer="
                        + SF_STATES_ID
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
                                    + "&layer="
                                    + SF_STATES_ID
                                    + "&format=image/png&width=22&height=22",
                            "image/png");
            ImageAssert.assertEquals(expected, image, 0);

            // the above again, but setting the workspace specific style
            statesLayer.setDefaultStyle(wsCustomStyle);
            catalog.save(statesLayer);
            image =
                    getAsImage(
                            "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                    + "&layer="
                                    + SF_STATES_ID
                                    + "&format=image/png&width=22&height=22",
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
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=Population"
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
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=Population"
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

    @Test
    public void testMemoryLimitDpiRescaled() throws Exception {
        // 40 kb are enough to store the legend image (uses 17KB), but not when scaled up 2x
        setMemoryLimit(40);
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=Population"
                        + "&format=image/png&width=20&height=20";
        // this one works, can be parsed as an image
        getAsImage(base, "image/png");
        // this one throws an exception
        Document dom = getAsDOM(base + "&legend_options=dpi:180");

        String message = checkLegacyException(dom, ServiceException.MAX_MEMORY_EXCEEDED, null);
        assertEquals(LegendGraphicBuilder.MEMORY_USAGE_EXCEEDED, message.trim());
    }

    /** Tests a dpi rescaled legend with specific rule name */
    @Test
    public void testStatesLegendDpiRescaledSingleRule() throws Exception {
        String base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=Population"
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
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=uom"
                        + "&format=image/png&width=20&height=20&scale=1000000";
        BufferedImage image = getAsImage(base, "image/png");

        assertPixel(image, 10, 10, Color.BLUE);
        assertPixel(image, 5, 10, Color.WHITE);
        assertPixel(image, 1, 10, Color.WHITE);

        // halve the scale denominator, we're zooming in, the thickness should double
        base =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=uom"
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
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=uom"
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
        String sld =
                IOUtils.toString(
                        TestData.class.getResource("externalEntities.sld"), StandardCharsets.UTF_8);
        MockHttpServletResponse response =
                getAsServletResponse(base + URLEncoder.encode(sld, UTF_8.name()));
        // should fail with an error message poiting at entity resolution
        assertEquals("application/vnd.ogc.se_xml", getBaseMimeType(response.getContentType()));
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
    public void testTransparentBelowMinScaleDenominator() throws Exception {
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                                + "&layer="
                                + getLayerId(MockData.LAKES)
                                + "&style=scaleDependent"
                                + "&format=image/png&width=20&height=20&scale=5000"
                                + "&transparent=true",
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
                        + "&layer="
                        + SF_STATES_ID
                        + "&style=Population"
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

    @Test
    public void testLayerListMemoryLimit() throws Exception {
        // small memory limit, will only fit a few rows
        setMemoryLimit(15);

        Catalog catalog = getCatalog();

        // setup base group, one layer
        String groupName = "MEMORY_TEST_GROUP";
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(groupName);
        group.getLayers().add(catalog.getLayerByName(getLayerId(SF_STATES)));
        group.getStyles().add(null);
        new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(group);
        catalog.add(group);

        // going to increase the number of layers until it does beyond limit
        String request =
                "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                        + "&layer=MEMORY_TEST_GROUP"
                        + "&style="
                        + "&format=image/png&width=20&height=20";

        // one layer
        BufferedImage image = getAsImage(request, "image/png");
        assertThat(image.getWidth() * image.getHeight() * 4, Matchers.lessThan(15 * 1024));

        // two layers
        group = catalog.getLayerGroupByName(groupName);
        group.getLayers().add(catalog.getLayerByName(getLayerId(MockData.LAKES)));
        group.getStyles().add(null);
        new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(group);
        catalog.save(group);
        image = getAsImage(request, "image/png");
        assertThat(image.getWidth() * image.getHeight() * 4, Matchers.lessThan(15 * 1024));

        // three layers, and boom!
        group = catalog.getLayerGroupByName(groupName);
        group.getLayers().add(catalog.getLayerByName(getLayerId(MockData.BUILDINGS)));
        group.getStyles().add(null);
        new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(group);
        catalog.save(group);
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("application/vnd.ogc.se_xml", getBaseMimeType(response.getContentType()));
        Document dom = dom(response, true);
        String message = checkLegacyException(dom, ServiceException.MAX_MEMORY_EXCEEDED, null);
        assertEquals(LegendGraphicBuilder.MEMORY_USAGE_EXCEEDED, message.trim());
    }
}
