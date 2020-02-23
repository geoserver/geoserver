/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.media.jai.PlanarImage;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.GetLegendGraphic;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.xml.styling.SLDParser;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Tets the functioning of the abstract legend producer for raster formats, which relies on
 * Geotools' StyledShapePainter.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class BufferedImageLegendGraphicOutputFormatTest
        extends BaseLegendTest<BufferedImageLegendGraphicBuilder> {
    @Before
    public void setLegendProducer() throws Exception {
        this.legendProducer = new BufferedImageLegendGraphicBuilder();

        service = new GetLegendGraphic(getWMS());
    }
    /**
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle
     * has more than one rule, and one of them is requested by the RULE parameter.
     */
    @org.junit.Test
    public void testUserSpecifiedRule() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle =
                getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        assertNotNull(multipleRulesStyle);

        Rule rule = multipleRulesStyle.featureTypeStyles().get(0).rules().get(0);
        LOGGER.info(
                "testing single rule "
                        + rule.getName()
                        + " from style "
                        + multipleRulesStyle.getName());

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(multipleRulesStyle);
        req.setRule(rule.getName());
        req.setLegendOptions(new HashMap());

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testUserSpecifiedRule", image, LegendUtils.DEFAULT_BG_COLOR);

        // was created only one rule?
        String errMsg =
                "expected just one legend of height "
                        + HEIGHT_HINT
                        + ", for the rule "
                        + rule.getName();
        int resultLegendCount = image.getHeight() / HEIGHT_HINT;
        assertEquals(errMsg, 1, resultLegendCount);
    }

    /**
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle
     * has more than one rule, and one of them is requested by the RULE parameter.
     */
    @org.junit.Test
    public void testRainfall() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle = getCatalog().getStyleByName("rainfall").getStyle();

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
            req.setLegendOptions(new HashMap());

            final int HEIGHT_HINT = 30;
            req.setHeight(HEIGHT_HINT);

            // use default values for the rest of parameters
            this.legendProducer.buildLegendGraphic(req);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);

            // was the legend painted?
            assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

            // was the legend painted?
            assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);
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

    /**
     * Tests that the legend graphic is still produced when the request's strict parameter is set to
     * false and a layer is not specified
     */
    @org.junit.Test
    public void testNoLayerProvidedAndNonStrictRequest() throws Exception {
        Style style = getCatalog().getStyleByName("rainfall").getStyle();
        assertNotNull(style);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setStrict(false);
        req.setLayer(null);
        req.setStyle(style);

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);
    }
    /** Tests that the legend graphic is produced for multiple layers */
    @org.junit.Test
    public void testMultipleLayers() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        int titleHeight = getTitleHeight(req);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());

        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testMultipleLayers", image, LegendUtils.DEFAULT_BG_COLOR);
        int height = image.getHeight();

        LegendRequest legend = new LegendRequest(ftInfo.getFeatureType());
        legend.setStyle(
                getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());
        req.getLegends().add(legend);

        this.legendProducer.buildLegendGraphic(req);

        image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testMultipleLayers", image, LegendUtils.DEFAULT_BG_COLOR);
        // with 2 layers we should have a legend at least 2 times taller (title + 2 layers)

        assertEquals(2 * (height + titleHeight), image.getHeight());

        // first title
        assertPixel(image, 1, titleHeight / 2, new Color(0, 0, 0));

        // first layer
        assertPixel(image, 10, 10 + titleHeight, new Color(192, 160, 0));

        assertPixel(image, 10, 30 + titleHeight, new Color(0, 0, 0));

        assertPixel(image, 10, 50 + titleHeight, new Color(224, 64, 0));

        // second title
        assertPixel(image, 1, 60 + titleHeight + titleHeight / 2, new Color(0, 0, 0));

        // same colors for the second layer
        assertPixel(image, 10, 70 + titleHeight * 2, new Color(192, 160, 0));

        assertPixel(image, 10, 90 + titleHeight * 2, new Color(0, 0, 0));

        assertPixel(image, 10, 110 + titleHeight * 2, new Color(224, 64, 0));
    }

    /** Tests that with forceTitles option off no title is rendered */
    @org.junit.Test
    public void testForceTitlesOff() throws Exception {
        Catalog cat = getCatalog();

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        Map<String, String> options = new HashMap<String, String>();
        options.put("forceTitles", "off");
        req.setLegendOptions(options);

        FeatureTypeInfo ftInfo =
                cat.getFeatureTypeByName(
                        MockData.ROAD_SEGMENTS.getNamespaceURI(),
                        MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers = new ArrayList<FeatureType>();
        req.setLayer(ftInfo.getFeatureType());

        req.setStyle(cat.getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());

        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testMultipleLayers", image, LegendUtils.DEFAULT_BG_COLOR);
        int height = image.getHeight();

        LegendRequest legend = new LegendRequest(ftInfo.getFeatureType());
        legend.setStyle(cat.getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());
        req.getLegends().add(legend);

        this.legendProducer.buildLegendGraphic(req);

        image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testForceTitlesOff", image, LegendUtils.DEFAULT_BG_COLOR);

        assertEquals(2 * height, image.getHeight());

        // first layer
        assertPixel(image, 10, 10, new Color(192, 160, 0));

        assertPixel(image, 10, 30, new Color(0, 0, 0));

        assertPixel(image, 10, 50, new Color(224, 64, 0));

        // same colors for the second layer
        assertPixel(image, 10, 70, new Color(192, 160, 0));

        assertPixel(image, 10, 90, new Color(0, 0, 0));

        assertPixel(image, 10, 110, new Color(224, 64, 0));
    }

    /**
     * Tests that the legend graphic is produced for multiple layers with different style for each
     * layer.
     */
    @org.junit.Test
    public void testMultipleLayersWithDifferentStyles() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        int titleHeight = getTitleHeight(req);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers = new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());
        layers.add(ftInfo.getFeatureType());
        layers.forEach(ft -> req.getLegends().add(new LegendRequest(ft)));

        Style style1 =
                getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        req.getLegends().get(0).setStyle(style1);

        Style style2 = getCatalog().getStyleByName(MockData.LAKES.getLocalPart()).getStyle();
        req.getLegends().get(1).setStyle(style2);

        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // first layer
        assertPixel(image, 10, 10 + titleHeight, new Color(192, 160, 0));

        assertPixel(image, 10, 30 + titleHeight, new Color(0, 0, 0));

        assertPixel(image, 10, 50 + titleHeight, new Color(224, 64, 0));

        // different color (style) for the second layer
        assertPixel(image, 10, 70 + titleHeight * 2, new Color(64, 64, 192));
    }

    /**
     * Tests that the legend graphic is produced for multiple layers with vector and coverage
     * layers.
     */
    @org.junit.Test
    public void testMultipleLayersWithVectorAndCoverage() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        int titleHeight = getTitleHeight(req);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers = new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());

        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            layers.add(feature.getSchema());

            layers.forEach(ft -> req.getLegends().add(new LegendRequest(ft)));

            Style style1 =
                    getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
            req.getLegends().get(0).setStyle(style1);

            Style style2 = getCatalog().getStyleByName("rainfall").getStyle();
            req.getLegends().get(1).setStyle(style2);

            this.legendProducer.buildLegendGraphic(req);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);

            // vector layer
            assertPixel(image, 10, 10 + titleHeight, new Color(192, 160, 0));

            assertPixel(image, 10, 30 + titleHeight, new Color(0, 0, 0));

            assertPixel(image, 10, 50 + titleHeight, new Color(224, 64, 0));

            // coverage layer
            assertPixel(image, 10, 70 + titleHeight * 2, new Color(115, 38, 0));
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

    /**
     * Tests that the legend graphic is produced for multiple layers with vector and coverage
     * layers, when coverage is not visible at current scale.
     */
    @org.junit.Test
    public void testMultipleLayersWithVectorAndInvisibleCoverage() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setScale(1000);
        int titleHeight = getTitleHeight(req);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers = new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());

        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            layers.add(feature.getSchema());

            layers.forEach(ft -> req.getLegends().add(new LegendRequest(ft)));

            Style style1 =
                    getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
            req.getLegends().get(0).setStyle(style1);
            req.getLegends().get(1).setStyle(readSLD("InvisibleRaster.sld"));

            this.legendProducer.buildLegendGraphic(req);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);

            // vector layer
            assertPixel(image, 10, 10 + titleHeight, new Color(192, 160, 0));

            assertPixel(image, 10, 30 + titleHeight, new Color(0, 0, 0));

            assertPixel(image, 10, 50 + titleHeight, new Color(224, 64, 0));

            // no coverage
            assertTrue(image.getHeight() < 70 + titleHeight * 2);
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

    /**
     * Tests that the legend graphic is produced for multiple layers, one of which cannot be seen at
     * the current scale
     */
    @org.junit.Test
    public void testMultipleLayersWithVectorAndInvisibleVector() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setScale(1000);
        int titleHeight = getTitleHeight(req);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers = new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());
        layers.add(ftInfo.getFeatureType());
        layers.forEach(ft -> req.getLegends().add(new LegendRequest(ft)));

        List<Style> styles = new ArrayList<Style>();
        final StyleInfo roadStyle =
                getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart());
        styles.add(roadStyle.getStyle());
        styles.add(readSLD("InvisibleLine.sld"));

        Iterator<Style> stylesIterator = styles.iterator();
        for (LegendRequest legend : req.getLegends()) {
            if (!stylesIterator.hasNext()) {
                break; // no more styles
            }
            legend.setStyle(stylesIterator.next());
        }

        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // vector layer
        assertPixel(image, 10, 10 + titleHeight, new Color(192, 160, 0));

        assertPixel(image, 10, 30 + titleHeight, new Color(0, 0, 0));

        assertPixel(image, 10, 50 + titleHeight, new Color(224, 64, 0));

        // no second vector layer
        assertTrue(image.getHeight() < 70 + titleHeight * 2);
    }

    @Test
    public void testMixedGeometry() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("MIXEDGEOMETRY");
        builder.setNamespaceURI("test");
        builder.setDefaultGeometry("GEOMETRY");
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        builder.setCRS(crs);

        GeometryFactory geometryFactory = new GeometryFactory();

        AttributeType at =
                new AttributeTypeImpl(
                        new NameImpl("ID"),
                        String.class,
                        false,
                        false,
                        Collections.EMPTY_LIST,
                        null,
                        null);
        builder.add(new AttributeDescriptorImpl(at, new NameImpl("ID"), 0, 1, false, null));

        GeometryType gt =
                new GeometryTypeImpl(
                        new NameImpl("GEOMETRY"),
                        Geometry.class,
                        crs,
                        false,
                        false,
                        Collections.EMPTY_LIST,
                        null,
                        null);

        builder.add(new GeometryDescriptorImpl(gt, new NameImpl("GEOMETRY"), 0, 1, false, null));

        FeatureType fType = builder.buildFeatureType();

        req.setLayer(fType);

        req.setStyle(readSLD("MixedGeometry.sld"));

        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("testMixedGeometry", image, LegendUtils.DEFAULT_BG_COLOR);

        // LineSymbolizer
        assertPixel(image, 10, 10, new Color(0, 0, 0));

        // PolygonSymbolizer
        assertPixel(image, 10, 30, new Color(0, 0, 255));

        // PointSymbolizer
        assertPixel(image, 10, 50, new Color(255, 0, 0));
    }

    /** Tests that symbols are not bigger than the requested icon size. */
    @org.junit.Test
    public void testSymbolContainedInIcon() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("BigSymbol.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("testSymbolSize", image, LegendUtils.DEFAULT_BG_COLOR);

        // background at borders
        assertPixel(image, 1, 1, new Color(255, 255, 255));

        // symbol in the center
        assertPixel(image, 10, 10, new Color(255, 0, 0));
    }

    /**
     * Tests that symbols are not bigger than the requested icon size, also if an expression is used
     * for the symbol Size.
     */
    @org.junit.Test
    public void testSymbolContainedInIconUsingExpression() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("SymbolExpression.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank(
                "testSymbolContainedInIconUsingExpression", image, LegendUtils.DEFAULT_BG_COLOR);

        // background at borders
        assertPixel(image, 1, 20, new Color(255, 255, 255));

        // symbol in the center (second symbol, the first one is attribute dependent and would not
        // be drawn normally)
        assertPixel(image, 10, 30, new Color(255, 0, 0));
    }

    /** Tests that symbols relative sizes are proportional. */
    @org.junit.Test
    public void testProportionalSymbolSize() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbols.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("testProportionalSymbolSize", image, LegendUtils.DEFAULT_BG_COLOR);

        // biggest symbol
        assertPixel(image, 1, 1, new Color(255, 255, 255));
        assertPixel(image, 5, 5, new Color(255, 0, 0));
        assertPixel(image, 10, 10, new Color(255, 0, 0));

        // second symbol
        assertPixel(image, 1, 21, new Color(255, 255, 255));
        assertPixel(image, 5, 25, new Color(255, 255, 255));
        assertPixel(image, 7, 27, new Color(255, 0, 0));
        assertPixel(image, 10, 30, new Color(255, 0, 0));

        // third symbol
        assertPixel(image, 1, 41, new Color(255, 255, 255));
        assertPixel(image, 5, 45, new Color(255, 255, 255));
        assertPixel(image, 6, 46, new Color(255, 255, 255));
        assertPixel(image, 10, 50, new Color(255, 0, 0));

        // smallest symbol
        assertPixel(image, 1, 61, new Color(255, 255, 255));
        assertPixel(image, 6, 68, new Color(255, 255, 255));
        assertPixel(image, 10, 70, new Color(255, 0, 0));
    }

    /** Tests that symbols relative sizes are proportional. */
    @org.junit.Test
    public void testProportionalSymbolThickBorder() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbolsThickBorder.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank(
                "testProportionalSymbolSizeThickBorder", image, LegendUtils.DEFAULT_BG_COLOR);

        // biggest symbol, thick border
        assertPixel(image, 1, 1, new Color(255, 255, 255)); // outside
        assertPixel(image, 5, 5, new Color(0, 0, 0)); // border
        assertPixel(image, 10, 10, new Color(255, 0, 0)); // inside

        // second symbol, small, no border
        assertPixel(image, 1, 21, new Color(255, 255, 255)); // outside
        assertPixel(image, 5, 25, new Color(255, 255, 255)); // small, still outside
        assertPixel(image, 10, 30, new Color(255, 0, 0)); // inside
    }

    /** Tests that symbols relative sizes are proportional. */
    @org.junit.Test
    public void testProportionalSymbolsLine() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbolsLine.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("ProportionalSymbolsLine", image, LegendUtils.DEFAULT_BG_COLOR);

        // biggest symbol, thick border
        assertPixel(image, 1, 1, new Color(255, 255, 255)); // outside
        assertPixel(image, 5, 5, new Color(0, 0, 0)); // border
        assertPixel(image, 7, 12, new Color(255, 0, 0)); // inside

        // second symbol, small, no border
        assertPixel(image, 1, 21, new Color(255, 255, 255)); // outside
        assertPixel(image, 5, 25, new Color(255, 255, 255)); // small, still outside
        assertPixel(image, 10, 30, new Color(255, 0, 0)); // inside
    }

    /** Tests that symbols relative sizes are proportional also if using uoms. */
    @org.junit.Test
    public void testProportionalSymbolSizeUOM() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbolsUOM.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("testProportionalSymbolSize", image, LegendUtils.DEFAULT_BG_COLOR);

        // biggest symbol
        assertPixel(image, 1, 1, new Color(255, 255, 255));
        assertPixel(image, 5, 5, new Color(255, 0, 0));
        assertPixel(image, 10, 10, new Color(255, 0, 0));

        // second symbol
        assertPixel(image, 1, 21, new Color(255, 255, 255));
        assertPixel(image, 5, 25, new Color(255, 255, 255));
        assertPixel(image, 7, 27, new Color(255, 0, 0));
        assertPixel(image, 10, 30, new Color(255, 0, 0));

        // third symbol
        assertPixel(image, 1, 41, new Color(255, 255, 255));
        assertPixel(image, 5, 45, new Color(255, 255, 255));
        assertPixel(image, 6, 46, new Color(255, 255, 255));
        assertPixel(image, 10, 50, new Color(255, 0, 0));

        // smallest symbol
        assertPixel(image, 1, 61, new Color(255, 255, 255));
        assertPixel(image, 6, 68, new Color(255, 255, 255));
        assertPixel(image, 10, 70, new Color(255, 0, 0));
    }

    /**
     * Tests that symbols relative sizes are proportional also if using uoms in some Symbolizer and
     * not using them in others.
     */
    @org.junit.Test
    public void testProportionalSymbolSizePartialUOM() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;

        req.setScale(RendererUtilities.calculatePixelsPerMeterRatio(10, Collections.EMPTY_MAP));

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbolsPartialUOM.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("testProportionalSymbolSize", image, LegendUtils.DEFAULT_BG_COLOR);

        // UOM symbol
        assertPixel(image, 1, 1, new Color(255, 255, 255));
        assertPixel(image, 5, 5, new Color(255, 0, 0));
        assertPixel(image, 10, 10, new Color(255, 0, 0));

        // non UOM symbol
        assertPixel(image, 1, 1, new Color(255, 255, 255));
        assertPixel(image, 5, 5, new Color(255, 0, 0));
        assertPixel(image, 10, 10, new Color(255, 0, 0));
    }

    /** Tests that minSymbolSize legend option is respected. */
    @org.junit.Test
    public void testMinSymbolSize() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        Map<String, String> options = new HashMap<String, String>();
        options.put("minSymbolSize", "10");
        req.setLegendOptions(options);

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbols.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("testProportionalSymbolSize", image, LegendUtils.DEFAULT_BG_COLOR);

        // biggest symbol
        assertPixel(image, 1, 1, new Color(255, 255, 255));
        assertPixel(image, 5, 5, new Color(255, 0, 0));
        assertPixel(image, 10, 10, new Color(255, 0, 0));

        // smallest symbol
        assertPixel(image, 1, 61, new Color(255, 255, 255));
        assertPixel(image, 7, 67, new Color(255, 0, 0));
        assertPixel(image, 10, 70, new Color(255, 0, 0));
    }

    /** Tests that minSymbolSize legend option is respected. */
    @org.junit.Test
    public void testInternationalizedLabels() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;

        Map<String, String> options = new HashMap<String, String>();
        options.put("forceLabels", "on");
        req.setLegendOptions(options);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("Internationalized.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);
        int noLocalizedWidth = image.getWidth();
        // ImageIO.write(image, "PNG", new File("/tmp/default.png"));
        req.setLocale(Locale.ITALIAN);
        image = this.legendProducer.buildLegendGraphic(req);
        // test that using localized labels we get a different label than when not using it
        int itWidth = image.getWidth();
        assertTrue(itWidth != noLocalizedWidth);
        // ImageIO.write(image, "PNG", new File("/tmp/it.png"));
        req.setLocale(Locale.ENGLISH);
        image = this.legendProducer.buildLegendGraphic(req);
        // test that using localized labels we get a different label than when not using it
        int enWidth = image.getWidth();
        assertTrue(enWidth != noLocalizedWidth);
        assertTrue(enWidth != itWidth);
        // ImageIO.write(image, "PNG", new File("/tmp/en.png"));
    }

    /**
     * Test that the legend is not the same if there is a rendering transformation that converts the
     * rendered layer from raster to vector
     */
    @org.junit.Test
    public void testRenderingTransformationRasterVector() throws Exception {

        Style transformStyle = readSLD("RenderingTransformRasterVector.sld");

        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        CoverageInfo cInfo =
                getCatalog()
                        .getCoverageByName(
                                MockData.TASMANIA_DEM.getNamespaceURI(),
                                MockData.TASMANIA_DEM.getLocalPart());
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            req.setLayer(feature.getSchema());
            req.setStyle(transformStyle);
            req.setLegendOptions(new HashMap());

            this.legendProducer.buildLegendGraphic(req);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);

            // ImageIO.write(image, "PNG", new File("/tmp/rv.png"));

            assertNotBlank("testRenderingTransform", image, LegendUtils.DEFAULT_BG_COLOR);

            assertPixel(image, 1, 1, new Color(255, 255, 255));
            assertPixel(image, 10, 10, new Color(0, 0, 0));
            assertPixel(image, 19, 19, new Color(255, 255, 255));

        } catch (Exception e) {
            fail(e.getMessage());
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

    /**
     * Test that the legend is not the same if there is a rendering transformation that converts the
     * rendered layer from raster to vector
     */
    @org.junit.Test
    public void testColorMapWithCql() throws Exception {

        Style style = readSLD("ColorMapWithCql.sld");
        assertNotNull(style.featureTypeStyles());
        assertEquals(1, style.featureTypeStyles().size());
        FeatureTypeStyle fts = style.featureTypeStyles().get(0);
        assertNotNull(fts.rules());
        assertEquals(1, fts.rules().size());
        Rule rule = fts.rules().get(0);
        assertNotNull(rule.symbolizers());
        assertEquals(1, rule.symbolizers().size());
        assertTrue(rule.symbolizers().get(0) instanceof RasterSymbolizer);
        RasterSymbolizer symbolizer = (RasterSymbolizer) rule.symbolizers().get(0);
        assertNotNull(symbolizer.getColorMap());
        assertEquals(3, symbolizer.getColorMap().getColorMapEntries().length);
        ColorMapEntry[] entries = symbolizer.getColorMap().getColorMapEntries();

        Color color = LegendUtils.color(entries[0]);
        int red = color.getRed();
        assertEquals(255, red);
        int green = color.getGreen();
        assertEquals(0, green);
        int blue = color.getBlue();
        assertEquals(0, blue);

        double quantity = LegendUtils.getQuantity(entries[1]);
        assertEquals(20.0, quantity, 0.0);

        double opacity = LegendUtils.getOpacity(entries[2]);
        assertEquals(0.5, opacity, 0.0);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            req.setLayer(feature.getSchema());
            req.setStyle(style);
            req.setLegendOptions(new HashMap());

            final int HEIGHT_HINT = 30;
            req.setHeight(HEIGHT_HINT);

            // use default values for the rest of parameters
            this.legendProducer.buildLegendGraphic(req);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);

            // was the legend painted?
            assertNotBlank("testColorMapWithCql", image, LegendUtils.DEFAULT_BG_COLOR);
            int fixedWidth = image.getWidth();

            // "-49 - -20 mm"
            String fixedLabel = entries[2].getLabel();
            entries[2].setLabel(
                    "${Concatenate('-', '4', '9', ' ', '-', ' ', '-', '2', '0', ' ', 'm', 'm')}");
            assertEquals(fixedLabel, LegendUtils.getLabel(entries[2]));

            image = this.legendProducer.buildLegendGraphic(req);

            // was the legend painted?
            assertNotBlank("testColorMapWithCql", image, LegendUtils.DEFAULT_BG_COLOR);

            // check that the legend images with the fixed label string and the
            // expression that generates the label string have the same widths.
            assertEquals(fixedWidth, image.getWidth());
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

    /**
     * Test that the legend is not the same if there is a rendering transformation that converts the
     * rendered layer from vector to raster
     */
    @org.junit.Test
    public void testRenderingTransformationVectorRaster() throws Exception {

        Style transformStyle = readSLD("RenderingTransformVectorRaster.sld");

        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.NAMED_PLACES.getNamespaceURI(),
                                MockData.NAMED_PLACES.getLocalPart());
        assertNotNull(ftInfo);

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(transformStyle);
        req.setLegendOptions(new HashMap());

        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // ImageIO.write(image, "PNG", new File("/tmp/vr.png"));

        assertNotBlank("testRenderingTransform", image, LegendUtils.DEFAULT_BG_COLOR);

        assertPixel(image, 10, 70, new Color(188, 188, 255));
        assertPixel(image, 10, 80, new Color(68, 68, 255));
        assertPixel(image, 10, 130, new Color(255, 152, 0));
    }

    /** Tests that a legend containing an ExternalGraphic icon is rendered properly. */
    @org.junit.Test
    public void testExternalGraphic() throws Exception {
        // load a style with 3 rules
        Style externalGraphicStyle = readSLD("ExternalGraphicDemo.sld");

        assertNotNull(externalGraphicStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            LegendRequest legend = new LegendRequest();
            legend.setStyle(externalGraphicStyle);

            req.getLegends().add(legend);
            req.setScale(1.0);

            final int HEIGHT_HINT = 30;
            req.setHeight(HEIGHT_HINT);

            // use default values for the rest of parameters
            this.legendProducer.buildLegendGraphic(req);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);

            // was our external graphic icon painted?
            assertPixel(image, 10, HEIGHT_HINT + HEIGHT_HINT / 2, Color.YELLOW);
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

    /** Tests labelMargin legend option */
    @org.junit.Test
    public void testLabelMargin() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.POINTS.getNamespaceURI(), MockData.POINTS.getLocalPart());
        req.setLayer(ftInfo.getFeatureType());
        Style externalGraphicStyle = readSLD("ExternalGraphicDemo.sld");
        req.setStyle(externalGraphicStyle);

        final int HEIGHT_HINT = 20;
        req.setHeight(HEIGHT_HINT);

        HashMap legendOptions = new HashMap();
        legendOptions.put("labelMargin", "10");
        req.setLegendOptions(legendOptions);

        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);
        assertEquals(HEIGHT_HINT * 2, image.getHeight());
        for (int x = 21; x <= 29; x++) {
            assertPixel(image, x, HEIGHT_HINT / 2, new Color(255, 255, 255));
        }

        legendOptions.put("labelMargin", "20");
        req.setLegendOptions(legendOptions);

        this.legendProducer.buildLegendGraphic(req);

        image = this.legendProducer.buildLegendGraphic(req);
        assertEquals(HEIGHT_HINT * 2, image.getHeight());
        for (int x = 21; x <= 39; x++) {
            assertPixel(image, x, HEIGHT_HINT / 2, new Color(255, 255, 255));
        }
    }

    /** Tests labelMargin legend option */
    @org.junit.Test
    public void testAbsoluteMargins() throws Exception {
        Style style = readSLD("ColorMapWithLongLabels.sld");
        assertNotNull(style.featureTypeStyles());
        assertEquals(1, style.featureTypeStyles().size());
        FeatureTypeStyle fts = style.featureTypeStyles().get(0);
        assertNotNull(fts.rules());
        assertEquals(1, fts.rules().size());
        Rule rule = fts.rules().get(0);
        assertNotNull(rule.symbolizers());
        assertEquals(1, rule.symbolizers().size());
        assertTrue(rule.symbolizers().get(0) instanceof RasterSymbolizer);
        RasterSymbolizer symbolizer = (RasterSymbolizer) rule.symbolizers().get(0);
        assertNotNull(symbolizer.getColorMap());
        assertEquals(3, symbolizer.getColorMap().getColorMapEntries().length);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        try {
            SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
            req.setLayer(feature.getSchema());
            req.setStyle(style);
            HashMap legendOptions = new HashMap();
            legendOptions.put("dx", "0.5");
            legendOptions.put("dy", "0");
            req.setLegendOptions(legendOptions);

            final int HEIGHT_HINT = 30;
            req.setHeight(HEIGHT_HINT);

            // use default values for the rest of parameters
            this.legendProducer.buildLegendGraphic(req);

            BufferedImage image = this.legendProducer.buildLegendGraphic(req);
            int absoluteWidth = image.getWidth();
            legendOptions.put("absoluteMargins", "false");
            image = this.legendProducer.buildLegendGraphic(req);
            assertTrue(image.getWidth() > absoluteWidth);
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

    /** Tests that symbols relative sizes are proportional. */
    @org.junit.Test
    public void testThickPolygonBorder() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ThickBorder.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("testThickPolygonBorder", image, LegendUtils.DEFAULT_BG_COLOR);

        // thick symbol, there is padding, black thick border, and center
        assertPixel(image, 1, 1, new Color(255, 255, 255));
        assertPixel(image, 6, 6, new Color(0, 0, 0));
        assertPixel(image, 10, 10, new Color(255, 0, 0));

        // second symbol, padding, border, green center
        assertPixel(image, 1, 21, new Color(255, 255, 255));
        // assertPixel(image, 4, 25, new Color(0, 0, 0)); // unsafe, the border is thin here
        assertPixel(image, 10, 30, new Color(0, 255, 0));
    }

    /** Tests that symbols relative sizes are proportional. */
    @org.junit.Test
    public void testThickPolygonAsymmetricSymbol() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        req.setWidth(40);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ThickBorder.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("testThickPolygonBorder", image, LegendUtils.DEFAULT_BG_COLOR);

        // thick symbol, there is padding, black thick border, and center
        assertPixel(image, 1, 1, new Color(255, 255, 255));
        assertPixel(image, 9, 6, new Color(0, 0, 0));
        assertPixel(image, 20, 10, new Color(255, 0, 0));

        // second symbol, padding, border, green center
        assertPixel(image, 1, 23, new Color(255, 255, 255));
        // assertPixel(image, 4, 25, new Color(0, 0, 0)); // unsafe, the border is thin here
        assertPixel(image, 20, 30, new Color(0, 255, 0));
    }

    /** Tests that symbols relative sizes are proportional. */
    @org.junit.Test
    public void testLargeCirclePlacement() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        req.setWidth(48);
        req.setHeight(25);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("largeCircle.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("largeCircle", image, LegendUtils.DEFAULT_BG_COLOR);

        // the border is visible both top middle and bottom middle. Different JDK
        // build wildly different colors for the border unfortunately, so the test
        // checks that pixels at top/middle bottom/middle are similar color (they used to be
        // different, significantly)
        Color colorTop = getPixelColor(image, 24, 0);
        Color colorBottom = getPixelColor(image, 24, 24);
        assertColorSimilar(colorTop, colorBottom, 20);
    }

    private void assertColorSimilar(Color expected, Color actual, int componentTolerance) {
        assertEquals(expected.getRed(), actual.getRed(), componentTolerance);
        assertEquals(expected.getGreen(), actual.getGreen(), componentTolerance);
        assertEquals(expected.getBlue(), actual.getBlue(), componentTolerance);
    }

    /** Tests that symbols relative sizes are proportional. */
    @org.junit.Test
    public void testSimpleLine() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest(null);
        ;
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("line.sld"));

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        assertNotBlank("line", image, LegendUtils.DEFAULT_BG_COLOR);

        // line in the middle, but off the middle, it's white
        Color colorCenter = getPixelColor(image, 10, 10);
        assertColorSimilar(Color.BLUE, colorCenter, 20);
        Color colorOutsideCenter = getPixelColor(image, 6, 6);
        assertColorSimilar(Color.WHITE, colorOutsideCenter, 20);
    }

    /** */
    private Style readSLD(String sldName) throws IOException {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        SLDParser stylereader = new SLDParser(styleFactory, getClass().getResource(sldName));
        Style[] readStyles = stylereader.readXML();

        Style style = readStyles[0];
        return style;
    }
}
