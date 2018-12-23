/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.media.jai.PlanarImage;
import javax.xml.transform.TransformerException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
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
import org.geotools.xml.styling.SLDTransformer;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the functioning of the abstract legend producer for JSON format,
 *
 * @author Ian Turton
 * @version $Id$
 */
public class JSONLegendGraphicOutputFormatTest extends BaseLegendTest {

    static final String JSONFormat = "application/json";
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

        Rule rule = multipleRulesStyle.getFeatureTypeStyles()[0].getRules()[0];
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
        // printStyle(multipleRulesStyle);
        req.setStyle(multipleRulesStyle);
        req.setRule(rule.getName());
        req.setLegendOptions(new HashMap());
        req.setFormat(JSONFormat);
        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);

        // System.out.println(result.toString(2));

        // check there is only one rule in the legend

        JSONArray rules = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, rules.size());
        assertEquals(
                rule.getDescription().getTitle().toString(),
                rules.getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES)
                        .getJSONObject(0)
                        .get(JSONLegendGraphicBuilder.TITLE));
    }

    /**
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle
     * has more than one rule, and one of them is requested by the RULE parameter.
     */
    @org.junit.Test
    public void testRainfall() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle = getCatalog().getStyleByName("rainfall").getStyle();
        // printStyle(multipleRulesStyle);
        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);

        SimpleFeatureCollection feature;
        feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.setLayer(feature.getSchema());
        req.setStyle(multipleRulesStyle);
        req.setLegendOptions(new HashMap());
        req.setFormat(JSONFormat);

        // use default values for the rest of parameters

        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
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
        // printStyle(style);
        req.setFormat(JSONFormat);

        JSONObject resp = (JSONObject) this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotNull(resp);

        // was the legend painted?
        // System.out.println(resp.toString(2) );
        assertEquals(1, resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND).size());
    }
    /** Tests that the legend graphic is produced for multiple layers */
    @org.junit.Test
    public void testMultipleLayers() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        req.setLayer(ftInfo.getFeatureType());
        Style style = getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        // printStyle(style);
        req.setStyle(style);
        req.setFormat(JSONFormat);

        JSONObject resp = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        // System.out.println(resp.toString(2));
        // was the legend painted?
        assertNotNull(resp);

        LegendRequest legend = req.new LegendRequest(ftInfo.getFeatureType());
        legend.setStyle(style);
        req.getLegends().add(legend);

        resp = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        // System.out.println(resp.toString(2));
        // was the legend painted?
        assertNotNull(resp);
    }

    /**
     * Tests that the legend graphic is produced for multiple layers with different style for each
     * layer.
     */
    @org.junit.Test
    public void testMultipleLayersWithDifferentStyles() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers = new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());
        layers.add(ftInfo.getFeatureType());
        req.setLayers(layers);

        List<Style> styles = new ArrayList<Style>();
        Style style1 =
                getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        styles.add(style1);
        // printStyle(style1);
        Style style2 = getCatalog().getStyleByName(MockData.LAKES.getLocalPart()).getStyle();
        styles.add(style2);
        // printStyle(style2);
        req.setStyles(styles);

        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        System.out.println(result.toString(2));
        assertNotNull(result);
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        // System.out.println(legend.toString(2) );
        assertEquals(2, legend.size());
    }

    /**
     * Tests that the legend graphic is produced for multiple layers with vector and coverage
     * layers.
     */
    @org.junit.Test
    public void testMultipleLayersWithVectorAndCoverage() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setFeatureType(JSONFormat);
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
        SimpleFeatureCollection feature;
        feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        layers.add(feature.getSchema());

        req.setLayers(layers);

        List<Style> styles = new ArrayList<Style>();
        Style style1 =
                getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        styles.add(style1);

        Style style2 = getCatalog().getStyleByName("rainfall").getStyle();
        styles.add(style2);
        req.setStyles(styles);

        JSONObject resp = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        assertNotNull(resp);
        // System.out.println(resp.toString(3));
        // vector layer
        assertEquals(
                "RoadSegments",
                resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                        .getJSONObject(0)
                        .get(JSONLegendGraphicBuilder.LAYER_NAME));
        // coverage layer
        assertEquals(
                "GridCoverage",
                resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                        .getJSONObject(1)
                        .get(JSONLegendGraphicBuilder.LAYER_NAME));
    }

    /**
     * Tests that the legend graphic is produced for multiple layers with vector and coverage
     * layers, when coverage is not visible at current scale.
     */
    @org.junit.Test
    public void testMultipleLayersWithVectorAndInvisibleCoverage() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setScale(1000);
        req.setFormat(JSONFormat);

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

        SimpleFeatureCollection feature;
        feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        layers.add(feature.getSchema());

        req.setLayers(layers);

        List<Style> styles = new ArrayList<Style>();
        Style style1 =
                getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        styles.add(style1);

        styles.add(readSLD("InvisibleRaster.sld"));

        req.setStyles(styles);

        JSONObject resp = (JSONObject) this.legendProducer.buildLegendGraphic(req);

        assertNotNull(resp);
        // System.out.println(resp.toString(3));
        JSONArray legends = resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, legends.size());
        // vector layer
        assertEquals(
                "RoadSegments", legends.getJSONObject(0).get(JSONLegendGraphicBuilder.LAYER_NAME));
    }

    /**
     * Tests that the legend graphic is produced for multiple layers, one of which cannot be seen at
     * the current scale
     */
    @org.junit.Test
    public void testMultipleLayersWithVectorAndInvisibleVector() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setScale(1000);
        req.setFormat(JSONFormat);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.ROAD_SEGMENTS.getNamespaceURI(),
                                MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers = new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());
        layers.add(ftInfo.getFeatureType());
        req.setLayers(layers);

        List<Style> styles = new ArrayList<Style>();
        final StyleInfo roadStyle =
                getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart());
        styles.add(roadStyle.getStyle());
        styles.add(readSLD("InvisibleLine.sld"));

        req.setStyles(styles);

        JSONObject resp = (JSONObject) this.legendProducer.buildLegendGraphic(req);

        assertNotNull(resp);
        System.out.println(resp.toString(3));
        JSONArray legends = resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, legends.size());
        // vector layer
        assertEquals(
                "RoadSegments", legends.getJSONObject(0).get(JSONLegendGraphicBuilder.LAYER_NAME));
    }

    @Test
    public void testMixedGeometry() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setFormat(JSONFormat);
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
        List<FeatureType> layers = new ArrayList<FeatureType>();
        layers.add(fType);

        req.setLayers(layers);

        List<Style> styles = new ArrayList<Style>();
        styles.add(readSLD("MixedGeometry.sld"));
        req.setStyles(styles);

        JSONObject resp = (JSONObject) this.legendProducer.buildLegendGraphic(req);

        assertNotNull(resp);
        System.out.println(resp.toString(3));
        JSONArray legends = resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, legends.size());
        // vector layer
        JSONObject legend = legends.getJSONObject(0);
        assertEquals("MIXEDGEOMETRY", legend.get(JSONLegendGraphicBuilder.LAYER_NAME));
        JSONArray rules = legend.getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertTrue(
                rules.getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                        .getJSONObject(0)
                        .containsKey(JSONLegendGraphicBuilder.LINE));
        assertTrue(
                rules.getJSONObject(1)
                        .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                        .getJSONObject(0)
                        .containsKey(JSONLegendGraphicBuilder.POLYGON));
        assertTrue(
                rules.getJSONObject(2)
                        .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                        .getJSONObject(0)
                        .containsKey(JSONLegendGraphicBuilder.POINT));
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

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbols.sld"));

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbolsThickBorder.sld"));

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbolsLine.sld"));

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("ProportionalSymbolsUOM.sld");
        // printStyle(style);
        req.setStyle(style);
        req.setFormat(JSONFormat);
        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // System.out.println(result.toString(2));
        JSONArray rules =
                result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                        .getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES);
        Iterator<?> iterator = rules.iterator();
        String[] expectedSizes = {"40", "20", "10", "1"};
        int counter = 0;
        while (iterator.hasNext()) {
            JSONObject rule = (JSONObject) iterator.next();
            assertNotNull(rule);
            JSONObject symbolizer =
                    rule.getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS).getJSONObject(0);

            JSONObject pointSymb = symbolizer.getJSONObject(JSONLegendGraphicBuilder.POINT);
            assertEquals(expectedSizes[counter++], pointSymb.get(JSONLegendGraphicBuilder.SIZE));
            assertEquals("m", pointSymb.get(JSONLegendGraphicBuilder.UOM));
            assertEquals(
                    "circle",
                    pointSymb
                            .getJSONArray(JSONLegendGraphicBuilder.GRAPHICS)
                            .getJSONObject(0)
                            .get(JSONLegendGraphicBuilder.MARK));
        }
    }

    /**
     * Tests that symbols relative sizes are proportional also if using uoms in some Symbolizer and
     * not using them in others.
     */
    @org.junit.Test
    public void testProportionalSymbolSizePartialUOM() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        req.setScale(RendererUtilities.calculatePixelsPerMeterRatio(10, Collections.EMPTY_MAP));

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbolsPartialUOM.sld"));

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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

    @org.junit.Test
    public void testInternationalizedLabels() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

        Map<String, String> options = new HashMap<String, String>();
        options.put("forceLabels", "on");
        req.setLegendOptions(options);
        req.setFormat(JSONFormat);
        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("Internationalized.sld");
        printStyle(style);
        req.setStyle(style);

        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        // System.out.println(result.toString(2));
        assertEquals(
                "title",
                result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                        .getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES)
                        .getJSONObject(0)
                        .get(JSONLegendGraphicBuilder.TITLE));
        req.setLocale(Locale.ITALIAN);
        result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        System.out.println(result.toString(2));
        assertEquals(
                "titolomoltolungo",
                result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                        .getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES)
                        .getJSONObject(0)
                        .get(JSONLegendGraphicBuilder.TITLE));
        req.setLocale(Locale.ENGLISH);
        result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        System.out.println(result.toString(2));
        assertEquals(
                "anothertitle",
                result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                        .getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES)
                        .getJSONObject(0)
                        .get(JSONLegendGraphicBuilder.TITLE));
        // test that using localized labels we get a different label than when not using it
    }

    /**
     * Test that the legend is not the same if there is a rendering transformation that converts the
     * rendered layer from raster to vector
     */
    @org.junit.Test
    public void testRenderingTransformationRasterVector() throws Exception {

        Style transformStyle = readSLD("RenderingTransformRasterVector.sld");

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
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

            BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
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

            BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

            // was the legend painted?
            assertNotBlank("testColorMapWithCql", image, LegendUtils.DEFAULT_BG_COLOR);

            // was the legend painted?
            assertNotBlank("testColorMapWithCql", image, LegendUtils.DEFAULT_BG_COLOR);
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

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.NAMED_PLACES.getNamespaceURI(),
                                MockData.NAMED_PLACES.getLocalPart());
        assertNotNull(ftInfo);

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(transformStyle);
        printStyle(transformStyle);
        req.setLegendOptions(new HashMap());
        req.setFormat(JSONFormat);

        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);

        // TODO add tests here
        System.out.println(result.toString(2));
    }

    /** Tests that a legend containing an ExternalGraphic icon is rendered properly. */
    @org.junit.Test
    public void testExternalGraphic() throws Exception {
        // load a style with 3 rules
        Style externalGraphicStyle = readSLD("ExternalGraphicDemo.sld");

        assertNotNull(externalGraphicStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        LegendRequest legend = req.new LegendRequest();
        legend.setStyle(externalGraphicStyle);
        // printStyle(externalGraphicStyle);
        req.getLegends().add(legend);
        req.setScale(1.0);
        req.setFormat(JSONFormat);
        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // System.out.println(result.toString(2));
        JSONArray lx = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, lx.size());
        // rule 1 is a mark
        JSONObject pointSymb =
                lx.getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES)
                        .getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                        .getJSONObject(0)
                        .getJSONObject(JSONLegendGraphicBuilder.POINT);

        assertEquals("14.0", pointSymb.get(JSONLegendGraphicBuilder.SIZE));
        assertEquals(
                "circle",
                pointSymb
                        .getJSONArray(JSONLegendGraphicBuilder.GRAPHICS)
                        .getJSONObject(0)
                        .get(JSONLegendGraphicBuilder.MARK));
        // rule 2 is a LegendGraphic
        assertEquals(
                "image/png",
                lx.getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES)
                        .getJSONObject(1)
                        .getJSONObject(JSONLegendGraphicBuilder.LEGEND_GRAPHIC)
                        .get(JSONLegendGraphicBuilder.EXTERNAL_GRAPHIC_TYPE));
    }

    /** Tests labelMargin legend option */
    @org.junit.Test
    public void testLabelMargin() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();

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

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);
        assertEquals(HEIGHT_HINT * 2, image.getHeight());
        for (int x = 21; x <= 29; x++) {
            assertPixel(image, x, HEIGHT_HINT / 2, new Color(255, 255, 255));
        }

        legendOptions.put("labelMargin", "20");
        req.setLegendOptions(legendOptions);

        this.legendProducer.buildLegendGraphic(req);

        image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);
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

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
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

            BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);
            int absoluteWidth = image.getWidth();
            legendOptions.put("absoluteMargins", "false");
            image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);
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
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("ThickBorder.sld");
        req.setStyle(style);
        // printStyle(style);
        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // System.out.println(result.toString(2));
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        assertFalse(legend.isEmpty());
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers =
                rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject polySymb1 =
                symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POLYGON);
        assertNotNull(polySymb1);
        assertEquals("#FF0000", polySymb1.get(JSONLegendGraphicBuilder.FILL));
        assertEquals("#000000", polySymb1.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("4", polySymb1.get(JSONLegendGraphicBuilder.STROKE_WIDTH));

        JSONObject polySymb2 =
                rules.getJSONObject(1)
                        .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                        .getJSONObject(0)
                        .getJSONObject(JSONLegendGraphicBuilder.POLYGON);
        assertNotNull(polySymb2);
        assertEquals("#00FF00", polySymb2.get(JSONLegendGraphicBuilder.FILL));
        assertEquals("#000000", polySymb2.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("1", polySymb2.get(JSONLegendGraphicBuilder.STROKE_WIDTH));
    }

    /** Tests that symbols relative sizes are proportional. */
    @org.junit.Test
    public void testThickPolygonAsymmetricSymbol() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setWidth(40);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ThickBorder.sld"));

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setWidth(48);
        req.setHeight(25);

        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("largeCircle.sld"));

        BufferedImage image = (BufferedImage) this.legendProducer.buildLegendGraphic(req);

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

    @org.junit.Test
    public void testSimplePoint() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setWidth(20);
        req.setHeight(20);
        req.setFormat(JSONFormat);
        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("point.sld");
        req.setStyle(style);
        printStyle(style);
        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        System.out.println(result.toString(2));
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers =
                rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject pointSymb =
                symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
    }

    @org.junit.Test
    public void testSimpleLine() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setWidth(20);
        req.setHeight(20);
        req.setFormat(JSONFormat);
        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("line.sld");
        req.setStyle(style);
        printStyle(style);
        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        System.out.println(result.toString(2));
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers =
                rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject lineSymb =
                symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.LINE);
        assertNotNull(lineSymb);

        assertEquals("#0000FF", lineSymb.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("2", lineSymb.get(JSONLegendGraphicBuilder.STROKE_WIDTH));
    }

    @org.junit.Test
    public void testSimplePolygon() throws Exception {
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setWidth(20);
        req.setHeight(20);
        req.setFormat(JSONFormat);
        FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MockData.MPOINTS.getNamespaceURI(),
                                MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("polygon.sld");
        req.setStyle(style);
        printStyle(style);
        JSONObject result = (JSONObject) this.legendProducer.buildLegendGraphic(req);
        System.out.println(result.toString(2));
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers =
                rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject polySymb =
                symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POLYGON);
        assertNotNull(polySymb);
        assertEquals("#0099cc", polySymb.get(JSONLegendGraphicBuilder.FILL));
        assertEquals("#000000", polySymb.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("0.5", polySymb.get(JSONLegendGraphicBuilder.STROKE_WIDTH));
    }
    /**
     * @param sldName
     * @throws IOException
     */
    private Style readSLD(String sldName) throws IOException {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        SLDParser stylereader = new SLDParser(styleFactory, getClass().getResource(sldName));
        Style[] readStyles = stylereader.readXML();

        Style style = readStyles[0];
        return style;
    }
    /**
     * @param style
     * @throws TransformerException
     */
    private void printStyle(Style style) throws TransformerException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SLDTransformer transformer = new SLDTransformer();
        transformer.setIndentation(2);
        transformer.transform(style, bos);
        String styleStr = bos.toString();
        System.out.println(styleStr);
    }
}
