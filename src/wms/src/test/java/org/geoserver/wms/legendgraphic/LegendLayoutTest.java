/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.HashMap;
import javax.media.jai.PlanarImage;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.image.util.ImageUtilities;
import org.geotools.styling.Style;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;

public class LegendLayoutTest extends BaseLegendTest {

    /** Tests horizontal layout for raster with RAMP */
    @org.junit.Test
    public void testRampHorizontalRaster() throws Exception {

        Style multipleRulesStyle = getCatalog().getStyleByName("rainfall_ramp").getStyle();

        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            req.setLayer(feature.getSchema());
            req.setStyle(multipleRulesStyle);

            final int HEIGHT_HINT = 30;
            req.setHeight(HEIGHT_HINT);
            HashMap legendOptions = new HashMap();
            req.setLegendOptions(legendOptions);

            // use default values for the rest of parameters
            this.legendProducer.buildLegendGraphic(req);

            BufferedImage vImage = this.legendProducer.buildLegendGraphic(req);

            // Change layout
            legendOptions = new HashMap();
            legendOptions.put("layout", "horizontal");
            req.setLegendOptions(legendOptions);

            BufferedImage hImage = this.legendProducer.buildLegendGraphic(req);

            // Check rotation
            assertEquals(vImage.getHeight(), hImage.getWidth());
            assertEquals(vImage.getWidth(), hImage.getHeight());

        } finally {
            RenderedImage ri = coverage.getRenderedImage();
            if (coverage instanceof GridCoverage2D) {
                ((GridCoverage2D) coverage).dispose(true);
            }
            if (ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
    }

    /** Tests horizontal layout for raster with CLASSES */
    @org.junit.Test
    public void testClassesHorizontalRaster() throws Exception {

        Style multipleRulesStyle =
                getCatalog().getStyleByName("rainfall_classes_nolabels").getStyle();

        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            req.setLayer(feature.getSchema());
            req.setStyle(multipleRulesStyle);

            final int HEIGHT_HINT = 30;
            req.setHeight(HEIGHT_HINT);

            // Change layout
            HashMap legendOptions = new HashMap();
            legendOptions.put("layout", "horizontal");
            legendOptions.put("mx", "0");
            legendOptions.put("my", "0");
            legendOptions.put("dx", "0");
            legendOptions.put("dy", "0");
            legendOptions.put("forceRule", "false");
            req.setLegendOptions(legendOptions);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);

            // Check output
            assertEquals(HEIGHT_HINT, image.getHeight());
            assertPixel(image, 9, HEIGHT_HINT / 2, new Color(115, 38, 0));
            assertPixel(image, 230, HEIGHT_HINT / 2, new Color(38, 115, 0));

        } finally {
            RenderedImage ri = coverage.getRenderedImage();
            if (coverage instanceof GridCoverage2D) {
                ((GridCoverage2D) coverage).dispose(true);
            }
            if (ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
    }

    /** Tests horizontal layout for raster with CLASSES and columns limits */
    @org.junit.Test
    public void testClassesRasterColumnsLimits() throws Exception {

        Style multipleRulesStyle =
                getCatalog().getStyleByName("rainfall_classes_nolabels").getStyle();

        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            req.setLayer(feature.getSchema());
            req.setStyle(multipleRulesStyle);

            final int HEIGHT_HINT = 30;
            req.setHeight(HEIGHT_HINT);

            // Change layout
            HashMap legendOptions = new HashMap();
            legendOptions.put("layout", "vertical");
            legendOptions.put("columnheight", "85");
            legendOptions.put("columns", "1");
            legendOptions.put("mx", "0");
            legendOptions.put("my", "0");
            legendOptions.put("dx", "0");
            legendOptions.put("dy", "0");
            legendOptions.put("forceRule", "false");
            req.setLegendOptions(legendOptions);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);

            // Check output
            assertEquals(3 * HEIGHT_HINT, image.getHeight());
            assertPixel(image, 9, 13, new Color(115, 38, 0));
            assertPixel(image, 9, 43, new Color(168, 0, 0));

        } finally {
            RenderedImage ri = coverage.getRenderedImage();
            if (coverage instanceof GridCoverage2D) {
                ((GridCoverage2D) coverage).dispose(true);
            }
            if (ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
    }

    /** Tests horizontal layout for raster with CLASSES and rows limits */
    @org.junit.Test
    public void testClassesRasterRowsLimits() throws Exception {

        Style multipleRulesStyle =
                getCatalog().getStyleByName("rainfall_classes_nolabels").getStyle();

        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            req.setLayer(feature.getSchema());
            req.setStyle(multipleRulesStyle);

            final int HEIGHT_HINT = 30;
            req.setHeight(HEIGHT_HINT);

            // Change layout
            HashMap legendOptions = new HashMap();
            legendOptions.put("layout", "horizontal");
            legendOptions.put("rowwidth", "100");
            legendOptions.put("rows", "2");
            legendOptions.put("mx", "0");
            legendOptions.put("my", "0");
            legendOptions.put("dx", "0");
            legendOptions.put("dy", "0");
            legendOptions.put("forceRule", "false");
            req.setLegendOptions(legendOptions);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);
            // Check output
            assertEquals(2 * HEIGHT_HINT, image.getHeight());
            assertPixel(image, 9, 13, new Color(115, 38, 0));
            assertPixel(image, 110, 43, new Color(38, 115, 0));

        } finally {
            RenderedImage ri = coverage.getRenderedImage();
            if (coverage instanceof GridCoverage2D) {
                ((GridCoverage2D) coverage).dispose(true);
            }
            if (ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
    }

    /** Tests horizontal layout for vector */
    @org.junit.Test
    public void testVectorLayersHorizontal() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());

        final int HEIGHT_HINT = 20;
        req.setHeight(HEIGHT_HINT);

        HashMap legendOptions = new HashMap();
        legendOptions.put("layout", "horizontal");
        legendOptions.put("forceLabels", "off");
        req.setLegendOptions(legendOptions);

        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertEquals(HEIGHT_HINT, image.getHeight());
        assertPixel(image, 10, HEIGHT_HINT / 2, new Color(192, 160, 0));
        assertPixel(image, 50, HEIGHT_HINT / 2, new Color(224, 64, 0));
    }

    @Test
    public void testLayerGroupTitles() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo lakesFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        FeatureTypeInfo placesFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.NAMED_PLACES.getNamespaceURI(),
                                MockData.NAMED_PLACES.getLocalPart());
        FeatureTypeInfo roadsFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());

        StyleInfo lakesStyle = getCatalog().getStyleByName(MockData.LAKES.getLocalPart());
        StyleInfo placesStyle = getCatalog().getStyleByName(MockData.NAMED_PLACES.getLocalPart());
        StyleInfo roadsStyle = getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart());

        req.setLayers(
                Arrays.asList(
                        lakesFt.getFeatureType(),
                        placesFt.getFeatureType(),
                        roadsFt.getFeatureType()));
        req.setStyles(
                Arrays.asList(
                        lakesStyle.getStyle(), placesStyle.getStyle(), roadsStyle.getStyle()));

        // Each icon will be 20px high (Labels are 14-15px)
        // Lakes have 1 icon, places have 2, and roads have 3
        final int HEIGHT_HINT = 20;
        req.setHeight(HEIGHT_HINT);

        HashMap legendOptions = new HashMap();
        legendOptions.put("forceTitles", "on");
        legendOptions.put("fontName", "Bitstream Vera Sans");
        req.setLegendOptions(legendOptions);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // Title height may vary between test environments
        assertTrue(
                "Expected height >= " + (HEIGHT_HINT * 6 + 42) + " but was " + image.getHeight(),
                HEIGHT_HINT * 6 + 42 <= image.getHeight());
        assertTrue(
                "Expected height <= " + (HEIGHT_HINT * 6 + 48) + " but was " + image.getHeight(),
                HEIGHT_HINT * 6 + 48 >= image.getHeight());
        // Verify the first icon of each layer is in the right place
        assertPixel(image, 10, 14 + HEIGHT_HINT / 2, new Color(64, 64, 192));
        assertPixel(image, 10, 28 + HEIGHT_HINT + HEIGHT_HINT / 2, new Color(170, 170, 170));
        assertPixel(image, 10, 42 + 3 * HEIGHT_HINT + HEIGHT_HINT / 2, new Color(192, 160, 0));

        legendOptions.put("forceTitles", "off");
        req.setLegendOptions(legendOptions);

        image = this.legendProducer.buildLegendGraphic(req);

        assertEquals(HEIGHT_HINT * 6, image.getHeight());
        // Verify the first icon of each layer is in the right place
        assertPixel(image, 10, HEIGHT_HINT / 2, new Color(64, 64, 192));
        assertPixel(image, 10, HEIGHT_HINT + HEIGHT_HINT / 2, new Color(170, 170, 170));
        assertPixel(image, 10, 3 * HEIGHT_HINT + HEIGHT_HINT / 2, new Color(192, 160, 0));
    }

    @Test
    public void testLayerGroupLabels() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo lakesFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        FeatureTypeInfo placesFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.NAMED_PLACES.getNamespaceURI(),
                                MockData.NAMED_PLACES.getLocalPart());
        FeatureTypeInfo roadsFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());

        StyleInfo lakesStyle = getCatalog().getStyleByName(MockData.LAKES.getLocalPart());
        StyleInfo placesStyle = getCatalog().getStyleByName(MockData.NAMED_PLACES.getLocalPart());
        StyleInfo roadsStyle = getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart());

        req.setLayers(
                Arrays.asList(
                        lakesFt.getFeatureType(),
                        placesFt.getFeatureType(),
                        roadsFt.getFeatureType()));
        req.setStyles(
                Arrays.asList(
                        lakesStyle.getStyle(), placesStyle.getStyle(), roadsStyle.getStyle()));

        // Each icon will be 20px high (Labels are 14-15px)
        // Lakes have 1 icon, places have 2, and roads have 3
        final int HEIGHT_HINT = 20;
        req.setHeight(HEIGHT_HINT);

        HashMap legendOptions = new HashMap();
        legendOptions.put("forceTitles", "off");
        legendOptions.put("forceLabels", "on");
        req.setLegendOptions(legendOptions);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertEquals(HEIGHT_HINT * 6, image.getHeight());
        assertTrue("Expected witdh > 40 but was " + image.getWidth(), 40 < image.getWidth());
        // Verify the first icon of each layer is in the right place
        assertPixel(image, 10, HEIGHT_HINT / 2, new Color(64, 64, 192));
        assertPixel(image, 10, HEIGHT_HINT + HEIGHT_HINT / 2, new Color(170, 170, 170));
        assertPixel(image, 10, 3 * HEIGHT_HINT + HEIGHT_HINT / 2, new Color(192, 160, 0));

        legendOptions.put("forceTitles", "off");
        legendOptions.put("forceLabels", "off");
        req.setLegendOptions(legendOptions);

        image = this.legendProducer.buildLegendGraphic(req);

        assertEquals(HEIGHT_HINT * 6, image.getHeight());
        // With no titles and no labels, legend should be as wide as a single icon
        assertEquals(24, image.getWidth());
        // Verify the first icon of each layer is in the right place
        assertPixel(image, 10, HEIGHT_HINT / 2, new Color(64, 64, 192));
        assertPixel(image, 10, HEIGHT_HINT + HEIGHT_HINT / 2, new Color(170, 170, 170));
        assertPixel(image, 10, 3 * HEIGHT_HINT + HEIGHT_HINT / 2, new Color(192, 160, 0));
    }

    @Test
    public void testLayerGroupLayout() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo lakesFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        FeatureTypeInfo placesFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.NAMED_PLACES.getNamespaceURI(),
                                MockData.NAMED_PLACES.getLocalPart());
        FeatureTypeInfo roadsFt =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());

        StyleInfo lakesStyle = getCatalog().getStyleByName(MockData.LAKES.getLocalPart());
        StyleInfo placesStyle = getCatalog().getStyleByName(MockData.NAMED_PLACES.getLocalPart());
        StyleInfo roadsStyle = getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart());

        req.setLayers(
                Arrays.asList(
                        lakesFt.getFeatureType(),
                        placesFt.getFeatureType(),
                        roadsFt.getFeatureType()));
        req.setStyles(
                Arrays.asList(
                        lakesStyle.getStyle(), placesStyle.getStyle(), roadsStyle.getStyle()));

        // Each icon will be 20px high
        // Lakes have 1 icon, places have 2, and roads have 3
        final int HEIGHT_HINT = 20;
        req.setHeight(HEIGHT_HINT);

        // Test layout with grouplayout=VERTICAL

        HashMap legendOptions = new HashMap();
        legendOptions.put("forceTitles", "off");
        legendOptions.put("forceLabels", "off");
        legendOptions.put("layout", "VERTICAL");
        legendOptions.put("grouplayout", "VERTICAL");
        req.setLegendOptions(legendOptions);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        /* Legend layout:
         *
         * L1
         * P1
         * P2
         * R1
         * R2
         * R3
         *
         */
        assertEquals(6 * HEIGHT_HINT, image.getHeight());
        assertEquals(HEIGHT_HINT + 4, image.getWidth());
        // Verify the first icon of each layer is in the right place
        assertPixel(image, 10, HEIGHT_HINT / 2, new Color(64, 64, 192));
        assertPixel(image, 10, HEIGHT_HINT + HEIGHT_HINT / 2, new Color(170, 170, 170));
        assertPixel(image, 10, 3 * HEIGHT_HINT + HEIGHT_HINT / 2, new Color(192, 160, 0));

        legendOptions.put("forceTitles", "off");
        legendOptions.put("forceLabels", "off");
        legendOptions.put("layout", "HORIZONTAL");
        legendOptions.put("grouplayout", "VERTICAL");
        req.setLegendOptions(legendOptions);

        image = this.legendProducer.buildLegendGraphic(req);

        /* Legend layout:
         *
         * L1
         * P1 P2
         * R1 R2 R3
         *
         */
        assertEquals(3 * HEIGHT_HINT, image.getHeight());
        assertEquals(3 * HEIGHT_HINT + 4, image.getWidth());
        // Verify the first icon of each layer is in the right place
        assertPixel(image, 10, HEIGHT_HINT / 2, new Color(64, 64, 192));
        assertPixel(image, 10, HEIGHT_HINT + HEIGHT_HINT / 2, new Color(170, 170, 170));
        assertPixel(image, 10, 2 * HEIGHT_HINT + HEIGHT_HINT / 2, new Color(192, 160, 0));

        // Test layout with grouplayout=HORIZONTAL

        legendOptions.put("forceTitles", "off");
        legendOptions.put("forceLabels", "off");
        legendOptions.put("layout", "VERTICAL");
        legendOptions.put("grouplayout", "HORIZONTAL");
        req.setLegendOptions(legendOptions);

        image = this.legendProducer.buildLegendGraphic(req);

        /* Legend layout:
         *
         * L1 P1 R1
         *    P2 R2
         *       R3
         *
         */
        assertEquals(3 * HEIGHT_HINT, image.getHeight());
        assertEquals(3 * HEIGHT_HINT + 6, image.getWidth());
        // Verify the first icon of each layer is in the right place
        assertPixel(image, HEIGHT_HINT / 2, 10, new Color(64, 64, 192));
        assertPixel(image, HEIGHT_HINT + HEIGHT_HINT / 2, 10, new Color(170, 170, 170));
        assertPixel(image, 2 * HEIGHT_HINT + HEIGHT_HINT / 2, 10, new Color(192, 160, 0));

        legendOptions.put("forceTitles", "off");
        legendOptions.put("forceLabels", "off");
        legendOptions.put("layout", "HORIZONTAL");
        legendOptions.put("grouplayout", "HORIZONTAL");
        req.setLegendOptions(legendOptions);

        image = this.legendProducer.buildLegendGraphic(req);

        /* Legend layout:
         *
         * L1 P1 P2 R1 R2 R3
         *
         */
        assertEquals(HEIGHT_HINT, image.getHeight());
        assertEquals(6 * HEIGHT_HINT + 6, image.getWidth());
        // Verify the first icon of each layer is in the right place
        assertPixel(image, HEIGHT_HINT / 2, 10, new Color(64, 64, 192));
        assertPixel(image, HEIGHT_HINT + HEIGHT_HINT / 2, 10, new Color(170, 170, 170));
        assertPixel(image, 3 * HEIGHT_HINT + HEIGHT_HINT / 2, 10, new Color(192, 160, 0));
    }
}
