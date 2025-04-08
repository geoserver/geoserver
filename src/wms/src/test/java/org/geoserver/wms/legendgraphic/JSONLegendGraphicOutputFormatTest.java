/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.transform.TransformerException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.GetLegendGraphic;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.WMS;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleFactory;
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
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.xml.styling.SLDParser;
import org.geotools.xml.styling.SLDTransformer;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.w3c.dom.Document;

/**
 * Test the functioning of the abstract legend producer for JSON format,
 *
 * @author Ian Turton
 * @version $Id$
 */
public class JSONLegendGraphicOutputFormatTest extends BaseLegendTest<JSONLegendGraphicBuilder> {

    static final String JSONFormat = "application/json";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("tricky_point", this.getClass(), catalog);
        testData.addStyle("arealandmarks", this.getClass(), catalog);
        testData.addStyle("fixedArrows", this.getClass(), catalog);
        testData.addStyle("dynamicArrows", this.getClass(), catalog);
        testData.addStyle("multiLanguageVector", this.getClass(), catalog);
        testData.addStyle("multiLanguageRaster", this.getClass(), catalog);
        testData.addWorkspace("foo", "http://foo", catalog);
        testData.addStyle(
                catalog.getWorkspaceByName("foo"), "arealandmarks", "arealandmarks.sld", this.getClass(), catalog);

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        NamespaceContext ctx = new SimpleNamespaceContext(namespaces);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @Before
    public void setLegendProducer() throws Exception {
        this.legendProducer = new JSONLegendGraphicBuilder();

        service = new GetLegendGraphic(getWMS());
    }
    /**
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle has more than one
     * rule, and one of them is requested by the RULE parameter.
     */
    @Test
    public void testUserSpecifiedRule() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle = getCatalog()
                .getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart())
                .getStyle();
        assertNotNull(multipleRulesStyle);

        Rule rule = multipleRulesStyle.featureTypeStyles().get(0).rules().get(0);
        LOGGER.info("testing single rule " + rule.getName() + " from style " + multipleRulesStyle.getName());

        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        GetLegendGraphicRequest req = getRequest(ftInfo.getFeatureType(), multipleRulesStyle);
        req.setRule(rule.getName());
        req.setLegendOptions(new HashMap<>());
        req.setFormat(JSONFormat);
        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        JSONObject result = this.legendProducer.buildLegendGraphic(req);

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
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle has more than one
     * rule, and one of them is requested by the RULE parameter.
     */
    @Test
    public void testRainfall() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle = getCatalog().getStyleByName("rainfall").getStyle();
        // printStyle(multipleRulesStyle);
        assertNotNull(multipleRulesStyle);

        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);
        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        SimpleFeatureCollection feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        GetLegendGraphicRequest req = getRequest(feature.getSchema(), multipleRulesStyle);

        req.setLegendOptions(new HashMap<>());

        // use default values for the rest of parameters

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
    }

    /**
     * Tests that the legend graphic is still produced when the request's strict parameter is set to false and a layer
     * is not specified
     */
    @Test
    public void testNoLayerProvidedAndNonStrictRequest() throws Exception {
        Style style = getCatalog().getStyleByName("rainfall").getStyle();
        assertNotNull(style);

        GetLegendGraphicRequest req = getRequest(null, style);
        req.setStrict(false);

        JSONObject resp = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotNull(resp);

        // was the legend painted?
        // System.out.println(resp.toString(2) );
        assertEquals(1, resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND).size());
    }
    /** Tests that the legend graphic is produced for multiple layers */
    @Test
    public void testMultipleLayers() throws Exception {

        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        Style style = getCatalog()
                .getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart())
                .getStyle();
        GetLegendGraphicRequest req = getRequest(ftInfo.getFeatureType(), style);

        JSONObject resp = this.legendProducer.buildLegendGraphic(req);
        // was the legend painted?
        assertNotEmpty(resp);

        LegendRequest legend = new LegendRequest(ftInfo.getFeatureType());
        legend.setStyle(style);
        req.getLegends().add(legend);

        resp = this.legendProducer.buildLegendGraphic(req);
        // System.out.println(resp.toString(2));
        // was the legend painted?
        assertNotEmpty(resp);
    }

    /** Tests that the legend graphic is produced for multiple layers with different style for each layer. */
    @Test
    public void testMultipleLayersWithDifferentStyles() throws Exception {
        GetLegendGraphicRequest req = getRequest(null, null);

        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());

        req.getLegends().clear();
        req.getLegends().add(new LegendRequest(ftInfo.getFeatureType()));
        req.getLegends().add(new LegendRequest(ftInfo.getFeatureType()));

        Style style1 = getCatalog()
                .getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart())
                .getStyle();
        req.getLegends().get(0).setStyle(style1);
        Style style2 =
                getCatalog().getStyleByName(MockData.LAKES.getLocalPart()).getStyle();
        req.getLegends().get(1).setStyle(style2);

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        // System.out.println(result.toString(2));
        assertNotNull(result);
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        // System.out.println(legend.toString(2) );
        assertEquals(2, legend.size());
    }

    /** Tests that the legend graphic is produced for multiple layers with vector and coverage layers. */
    @Test
    public void testMultipleLayersWithVectorAndCoverage() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setFeatureType(JSONFormat);
        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers = new ArrayList<>();
        req.getLegends().clear();
        layers.add(ftInfo.getFeatureType());

        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        SimpleFeatureCollection feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        layers.add(feature.getSchema());

        layers.forEach(ft -> req.getLegends().add(new LegendRequest(ft)));

        Style style1 = getCatalog()
                .getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart())
                .getStyle();
        req.getLegends().get(0).setStyle(style1);

        Style style2 = getCatalog().getStyleByName("rainfall").getStyle();
        req.getLegends().get(1).setStyle(style2);

        JSONObject resp = this.legendProducer.buildLegendGraphic(req);
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
     * Tests that the legend graphic is produced for multiple layers with vector and coverage layers, when coverage is
     * not visible at current scale.
     */
    @Test
    public void testMultipleLayersWithVectorAndInvisibleCoverage() throws Exception {
        GetLegendGraphicRequest req = getRequest(null, null);
        req.setScale(1000);
        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        req.getLegends().clear();
        req.getLegends().add(new LegendRequest(ftInfo.getFeatureType()));

        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);
        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        SimpleFeatureCollection feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.getLegends().add(new LegendRequest(feature.getSchema()));

        Style style1 = getCatalog()
                .getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart())
                .getStyle();
        req.getLegends().get(0).setStyle(style1);
        req.getLegends().get(1).setStyle(readSLD("InvisibleRaster.sld"));

        JSONObject resp = this.legendProducer.buildLegendGraphic(req);

        assertNotNull(resp);
        // System.out.println(resp.toString(3));
        JSONArray legends = resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, legends.size());
        // vector layer
        assertEquals("RoadSegments", legends.getJSONObject(0).get(JSONLegendGraphicBuilder.LAYER_NAME));
    }

    /**
     * Tests that the legend graphic is produced for multiple layers, one of which cannot be seen at the current scale
     */
    @Test
    public void testMultipleLayersWithVectorAndInvisibleVector() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setScale(1000);
        req.getLegends().clear();

        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        req.getLegends().add(new LegendRequest(ftInfo.getFeatureType()));
        req.getLegends().add(new LegendRequest(ftInfo.getFeatureType()));

        final StyleInfo roadStyle = getCatalog().getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart());
        req.getLegends().get(0).setStyle(roadStyle.getStyle());
        req.getLegends().get(1).setStyle(readSLD("InvisibleLine.sld"));

        JSONObject resp = this.legendProducer.buildLegendGraphic(req);

        assertNotNull(resp);
        JSONArray legends = resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, legends.size());
        // vector layer
        assertEquals("RoadSegments", legends.getJSONObject(0).get(JSONLegendGraphicBuilder.LAYER_NAME));
    }

    @Test
    public void testMixedGeometry() throws Exception {
        GetLegendGraphicRequest req = getRequest();

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("MIXEDGEOMETRY");
        builder.setNamespaceURI("test");
        builder.setDefaultGeometry("GEOMETRY");
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        builder.setCRS(crs);

        AttributeType at = new AttributeTypeImpl(
                new NameImpl("ID"), String.class, false, false, Collections.emptyList(), null, null);
        builder.add(new AttributeDescriptorImpl(at, new NameImpl("ID"), 0, 1, false, null));

        GeometryType gt = new GeometryTypeImpl(
                new NameImpl("GEOMETRY"), Geometry.class, crs, false, false, Collections.emptyList(), null, null);

        builder.add(new GeometryDescriptorImpl(gt, new NameImpl("GEOMETRY"), 0, 1, false, null));

        FeatureType fType = builder.buildFeatureType();
        req.getLegends().clear();
        LegendRequest lr = new LegendRequest(fType);
        lr.setStyle(readSLD("MixedGeometry.sld"));
        req.getLegends().add(lr);

        JSONObject resp = this.legendProducer.buildLegendGraphic(req);

        assertNotNull(resp);
        JSONArray legends = resp.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, legends.size());
        // vector layer
        JSONObject legend = legends.getJSONObject(0);
        assertEquals("MIXEDGEOMETRY", legend.get(JSONLegendGraphicBuilder.LAYER_NAME));
        JSONArray rules = legend.getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertTrue(rules.getJSONObject(0)
                .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                .getJSONObject(0)
                .containsKey(JSONLegendGraphicBuilder.LINE));
        assertTrue(rules.getJSONObject(1)
                .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                .getJSONObject(0)
                .containsKey(JSONLegendGraphicBuilder.POLYGON));
        assertTrue(rules.getJSONObject(2)
                .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                .getJSONObject(0)
                .containsKey(JSONLegendGraphicBuilder.POINT));
    }

    /**
     * Tests that symbols are not bigger than the requested icon size, also if an expression is used for the symbol
     * Size.
     */
    @Test
    public void testSymbolContainedInIconUsingExpression() throws Exception {
        GetLegendGraphicRequest req = getRequest();

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("SymbolExpression.sld");
        req.setStyle(style);
        // printStyle(style);
        req.setFormat(JSONFormat);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        JSONArray rules = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                .getJSONObject(0)
                .getJSONArray(JSONLegendGraphicBuilder.RULES);
        Iterator<?> iterator = rules.iterator();
        String[] expectedSizes = {"[\"id\"]", "40"};
        int counter = 0;
        while (iterator.hasNext()) {
            JSONObject rule = (JSONObject) iterator.next();
            assertNotNull(rule);
            JSONObject symbolizer =
                    rule.getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS).getJSONObject(0);

            JSONObject pointSymb = symbolizer.getJSONObject(JSONLegendGraphicBuilder.POINT);
            assertEquals(
                    expectedSizes[counter++],
                    pointSymb.get(JSONLegendGraphicBuilder.SIZE).toString());
            assertEquals(
                    "circle",
                    pointSymb
                            .getJSONArray(JSONLegendGraphicBuilder.GRAPHICS)
                            .getJSONObject(0)
                            .get(JSONLegendGraphicBuilder.MARK));
        }
    }

    /** Tests that symbols relative sizes are proportional. */
    @Test
    public void testProportionalSymbolSize() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbols.sld"));
        req.setFormat(JSONFormat);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // System.out.println(result.toString(2));
        JSONArray rules = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
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
            assertEquals(
                    expectedSizes[counter++],
                    pointSymb.get(JSONLegendGraphicBuilder.SIZE).toString());
            assertEquals(
                    "circle",
                    pointSymb
                            .getJSONArray(JSONLegendGraphicBuilder.GRAPHICS)
                            .getJSONObject(0)
                            .get(JSONLegendGraphicBuilder.MARK));
        }
    }

    /** Tests that symbols relative sizes are proportional. */
    @Test
    public void testProportionalSymbolsLine() throws Exception {
        GetLegendGraphicRequest req = getRequest();

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(readSLD("ProportionalSymbolsLine.sld"));

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotEmpty(result);

        JSONArray rules = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                .getJSONObject(0)
                .getJSONArray(JSONLegendGraphicBuilder.RULES);
        Iterator<?> iterator = rules.iterator();
        String[] expectedSizes = {"30", "15"};
        int counter = 0;
        while (iterator.hasNext()) {
            JSONObject rule = (JSONObject) iterator.next();
            assertNotNull(rule);
            JSONObject symbolizer =
                    rule.getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS).getJSONObject(0);

            JSONObject pointSymb = symbolizer.getJSONObject(JSONLegendGraphicBuilder.LINE);

            assertEquals(
                    expectedSizes[counter++],
                    pointSymb
                            .getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_STROKE)
                            .getString(JSONLegendGraphicBuilder.SIZE));
        }
    }

    /** Tests that symbols relative sizes are proportional also if using uoms. */
    @Test
    public void testProportionalSymbolSizeUOM() throws Exception {
        GetLegendGraphicRequest req = getRequest();

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("ProportionalSymbolsUOM.sld");
        // printStyle(style);
        req.setStyle(style);
        req.setFormat(JSONFormat);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        JSONArray rules = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
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
            assertEquals(
                    expectedSizes[counter++],
                    pointSymb.get(JSONLegendGraphicBuilder.SIZE).toString());
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
     * Tests that symbols relative sizes are proportional also if using uoms in some Symbolizer and not using them in
     * others.
     */
    @Test
    public void testProportionalSymbolSizePartialUOM() throws Exception {
        GetLegendGraphicRequest req = getRequest();

        req.setScale(RendererUtilities.calculatePixelsPerMeterRatio(10, Collections.emptyMap()));

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("ProportionalSymbolsPartialUOM.sld");
        printStyle(style);
        req.setStyle(style);
        req.setFormat(JSONFormat);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotEmpty(result);

        JSONArray rules = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                .getJSONObject(0)
                .getJSONArray(JSONLegendGraphicBuilder.RULES);

        String[] expectedSizes = {"40.0", "40.0"};

        for (int i = 0; i < rules.size(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            assertNotEmpty(rule);

            JSONObject symbolizer =
                    rule.getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS).getJSONObject(0);

            JSONObject pointSymb = symbolizer.getJSONObject(JSONLegendGraphicBuilder.POINT);
            assertEquals(expectedSizes[i], pointSymb.get(JSONLegendGraphicBuilder.SIZE));

            assertEquals(
                    "circle",
                    pointSymb
                            .getJSONArray(JSONLegendGraphicBuilder.GRAPHICS)
                            .getJSONObject(0)
                            .get(JSONLegendGraphicBuilder.MARK));
        }
    }

    @Test
    public void testInternationalizedLabels() throws Exception {
        GetLegendGraphicRequest req = getRequest();

        Map<String, String> options = new HashMap<>();
        options.put("forceLabels", "on");
        req.setLegendOptions(options);
        req.setFormat(JSONFormat);
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("Internationalized.sld");
        // printStyle(style);
        req.setStyle(style);

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        // System.out.println(result.toString(2));
        assertEquals(
                "title",
                result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                        .getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES)
                        .getJSONObject(0)
                        .get(JSONLegendGraphicBuilder.TITLE));
        req.setLocale(Locale.ITALIAN);
        result = this.legendProducer.buildLegendGraphic(req);
        // System.out.println(result.toString(2));
        assertEquals(
                "titolomoltolungo",
                result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                        .getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.RULES)
                        .getJSONObject(0)
                        .get(JSONLegendGraphicBuilder.TITLE));
        req.setLocale(Locale.ENGLISH);
        result = this.legendProducer.buildLegendGraphic(req);
        // System.out.println(result.toString(2));
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
     * Test that the legend is not the same if there is a rendering transformation that converts the rendered layer from
     * raster to vector
     */
    @Test
    public void testRenderingTransformationRasterVector() throws Exception {

        Style transformStyle = readSLD("RenderingTransformRasterVector.sld");

        GetLegendGraphicRequest req = getRequest();
        CoverageInfo cInfo = getCatalog()
                .getCoverageByName(MockData.TASMANIA_DEM.getNamespaceURI(), MockData.TASMANIA_DEM.getLocalPart());
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        SimpleFeatureCollection feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.setLayer(feature.getSchema());
        req.setStyle(transformStyle);
        req.setLegendOptions(new HashMap<>());
        req.setFormat(JSONFormat);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        // was the legend painted?
        assertNotEmpty(result);

        // System.out.println(result.toString(2));
    }

    /**
     * Test that the legend is not the same if there is a rendering transformation that converts the rendered layer from
     * raster to vector
     */
    @Test
    public void testColorMapWithCql() throws Exception {

        Style style = readSLD("ColorMapWithCql.sld");
        // printStyle(style);
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

        GetLegendGraphicRequest req = getRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        SimpleFeatureCollection feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.setLayer(feature.getSchema());
        req.setStyle(style);
        req.setLegendOptions(new HashMap<>());

        JSONObject result = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotEmpty(result);
        JSONArray lx = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, lx.size());
        // rule 1 is a mark
        JSONObject rasterSymb = lx.getJSONObject(0)
                .getJSONArray(JSONLegendGraphicBuilder.RULES)
                .getJSONObject(0)
                .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                .getJSONObject(0)
                .getJSONObject(JSONLegendGraphicBuilder.RASTER);

        JSONArray colorMap = rasterSymb
                .getJSONObject(JSONLegendGraphicBuilder.COLORMAP)
                .getJSONArray(JSONLegendGraphicBuilder.ENTRIES);
        assertEquals(
                "['${strConcat(''#FF'',''0000'')}']", colorMap.getJSONObject(0).get(JSONLegendGraphicBuilder.COLOR));
        assertEquals("[\"${15+5}\"]", colorMap.getJSONObject(1).getString(JSONLegendGraphicBuilder.QUANTITY));
        assertEquals("[\"${0.25*2}\"]", colorMap.getJSONObject(2).getString(JSONLegendGraphicBuilder.OPACITY));
    }

    /**
     * Test that the legend is not the same if there is a rendering transformation that converts the rendered layer from
     * vector to raster
     */
    @Test
    public void testRenderingTransformationVectorRaster() throws Exception {

        Style transformStyle = readSLD("RenderingTransformVectorRaster.sld");

        GetLegendGraphicRequest req = getRequest();
        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.NAMED_PLACES.getNamespaceURI(), MockData.NAMED_PLACES.getLocalPart());
        assertNotNull(ftInfo);

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(transformStyle);
        // printStyle(transformStyle);
        req.setLegendOptions(new HashMap<>());
        req.setFormat(JSONFormat);

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotEmpty(result);
    }

    /** Tests that a legend containing an ExternalGraphic icon is rendered properly. */
    @Test
    public void testExternalGraphic() throws Exception {
        // load a style with 3 rules
        Style externalGraphicStyle = readSLD("ExternalGraphicDemo.sld");

        assertNotNull(externalGraphicStyle);

        GetLegendGraphicRequest req = getRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        printStyle(externalGraphicStyle);
        req.setStyle(externalGraphicStyle);

        req.setScale(1.0);

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotEmpty(result);
        print(result);
        JSONArray lx = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertEquals(1, lx.size());
        // rule 1 is a mark
        JSONObject pointSymb = lx.getJSONObject(0)
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

    /** Tests that symbols relative sizes are proportional. */
    @Test
    public void testThickPolygonBorder() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("ThickBorder.sld");
        req.setStyle(style);
        printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        print(result);
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        assertFalse(legend.isEmpty());
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject polySymb1 = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POLYGON);
        assertNotNull(polySymb1);
        assertEquals("#FF0000", polySymb1.get(JSONLegendGraphicBuilder.FILL));
        assertEquals("#000000", polySymb1.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("4", polySymb1.get(JSONLegendGraphicBuilder.STROKE_WIDTH));

        JSONObject polySymb2 = rules.getJSONObject(1)
                .getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS)
                .getJSONObject(0)
                .getJSONObject(JSONLegendGraphicBuilder.POLYGON);
        assertNotNull(polySymb2);
        assertEquals("#00FF00", polySymb2.get(JSONLegendGraphicBuilder.FILL));
        assertEquals("#000000", polySymb2.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("1", polySymb2.get(JSONLegendGraphicBuilder.STROKE_WIDTH));
    }

    @Test
    public void testSimplePoint() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("point.sld");
        req.setStyle(style);
        printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        print(result);
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject pointSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
    }

    @Test
    public void testHospitalPoint() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("hospital.sld");
        req.setStyle(style);
        printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        print(result);
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject pointSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
        assertEquals("http://local-test:8080/geoserver/kml/icon/Hospital?0.0.0=", pointSymb.getString("url"));
        pointSymb = symbolizers.getJSONObject(1).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
        assertEquals("http://local-test:8080/geoserver/kml/icon/Hospital?0.0.1=", pointSymb.getString("url"));
    }

    @Test
    public void testTrickyGraphic() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        org.geoserver.catalog.Catalog catalog = getCatalog();
        FeatureTypeInfo ftInfo =
                catalog.getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());

        // we need this to be in the "styles" directory to test the legend icon code!
        StyleInfo styleinfo = catalog.getStyleByName("tricky_point");
        Style style = styleinfo.getStyle();
        req.setStyle(style);
        printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        print(result);
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject pointSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
        assertEquals(
                "http://local-test:8080/geoserver/styles/img/landmarks/shop_supermarket.p.16.png",
                pointSymb.getString("url"));
        symbolizers = rules.getJSONObject(2).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        pointSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
        assertEquals("http://local-test:8080/geoserver/kml/icon/tricky_point?0.2.0=", pointSymb.getString("url"));
        pointSymb = symbolizers.getJSONObject(1).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
        assertEquals("http://local-test:8080/geoserver/kml/icon/tricky_point?0.2.1=", pointSymb.getString("url"));
    }

    @Test
    public void testGraphicFillLinks() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        org.geoserver.catalog.Catalog catalog = getCatalog();
        FeatureTypeInfo ftInfo =
                catalog.getFeatureTypeByName(MockData.MPOLYGONS.getNamespaceURI(), MockData.MPOLYGONS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());

        // we need this to be in the "styles" directory to test the legend icon code!
        StyleInfo styleinfo = catalog.getStyleByName("arealandmarks");
        Style style = styleinfo.getStyle();
        req.setStyle(style);
        printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        print(result);
        assertNotNull(result);
        // extract the basics
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertEquals(2, rules.size());

        // first rule, static image
        final JSONObject r1 = rules.getJSONObject(0);
        assertEquals("park", r1.getString("name"));
        assertEquals("[MTFCC = 'K2180']", r1.getString("filter"));
        final JSONArray symbolizers1 = r1.getJSONArray("symbolizers");
        assertEquals(1, symbolizers1.size());
        final JSONObject s1 = symbolizers1.getJSONObject(0);
        final JSONObject gf1 =
                s1.getJSONObject(JSONLegendGraphicBuilder.POLYGON).getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_FILL);
        assertEquals("http://local-test:8080/geoserver/styles/img/landmarks/area/forest.png", gf1.getString("url"));

        // second rule, mark, needs icon service and enabling non point graphics
        final JSONObject r2 = rules.getJSONObject(1);
        assertEquals("nationalpark", r2.getString("name"));
        assertEquals("[MTFCC = 'K2181']", r2.getString("filter"));
        final JSONArray symbolizers2 = r2.getJSONArray("symbolizers");
        assertEquals(1, symbolizers2.size());
        final JSONObject s2 = symbolizers2.getJSONObject(0);
        final JSONObject gf2 =
                s2.getJSONObject(JSONLegendGraphicBuilder.POLYGON).getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_FILL);
        assertEquals("http://local-test:8080/geoserver/kml/icon/arealandmarks?0.1.0=&npg=true", gf2.getString("url"));
    }

    @Test
    public void testGraphicFillLinksInWorkspace() throws Exception {
        org.geoserver.catalog.Catalog catalog = getCatalog();
        FeatureTypeInfo ftInfo =
                catalog.getFeatureTypeByName(MockData.MPOLYGONS.getNamespaceURI(), MockData.MPOLYGONS.getLocalPart());
        StyleInfo styleInfo = catalog.getStyleByName("foo:arealandmarks");
        GetLegendGraphicRequest req = getRequest(ftInfo.getFeatureType(), styleInfo.getStyle());
        req.setWidth(20);
        req.setHeight(20);

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
        JSONArray rules = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                .getJSONObject(0)
                .getJSONArray(JSONLegendGraphicBuilder.RULES);

        // test URL of static image (rule 1) for style inside a workspace
        String graphicUrl = rules.getJSONObject(0)
                .getJSONArray("symbolizers")
                .getJSONObject(0)
                .getJSONObject(JSONLegendGraphicBuilder.POLYGON)
                .getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_FILL)
                .getString("url");
        assertEquals("http://local-test:8080/geoserver/styles/foo/img/landmarks/area/forest.png", graphicUrl);

        // test URL of mark (rule 2) for style inside a workspace
        String markUrl = rules.getJSONObject(1)
                .getJSONArray("symbolizers")
                .getJSONObject(0)
                .getJSONObject(JSONLegendGraphicBuilder.POLYGON)
                .getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_FILL)
                .getString("url");
        assertEquals("http://local-test:8080/geoserver/kml/icon/foo/arealandmarks?0.1.0=&npg=true", markUrl);
    }

    @Test
    public void testTextSymbolizerGraphic() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        org.geoserver.catalog.Catalog catalog = getCatalog();
        FeatureTypeInfo ftInfo =
                catalog.getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());

        // we need this to be in the "styles" directory to test the legend icon code!
        StyleInfo styleinfo = catalog.getStyleByName("fixedArrows");
        Style style = styleinfo.getStyle();
        req.setStyle(style);
        printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        print(result);
        assertNotNull(result);
        // extract the basics
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertEquals(1, rules.size());

        // check the link
        final JSONObject r1 = rules.getJSONObject(0);
        final JSONArray symbolizers1 = r1.getJSONArray("symbolizers");
        assertEquals(1, symbolizers1.size());
        final JSONObject s1 = symbolizers1.getJSONObject(0);
        final JSONObject graphic =
                s1.getJSONObject(JSONLegendGraphicBuilder.TEXT).getJSONObject(JSONLegendGraphicBuilder.GRAPHIC);

        assertEquals("http://local-test:8080/geoserver/kml/icon/fixedArrows?0.0.0=&npg=true", graphic.getString("url"));
    }

    @Test
    public void testTextSymbolizerDynamicGraphic() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        org.geoserver.catalog.Catalog catalog = getCatalog();
        FeatureTypeInfo ftInfo =
                catalog.getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());

        // we need this to be in the "styles" directory to test the legend icon code!
        StyleInfo styleinfo = catalog.getStyleByName("dynamicArrows");
        Style style = styleinfo.getStyle();
        req.setStyle(style);
        printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        print(result);
        assertNotNull(result);
        // extract the basics
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertEquals(1, rules.size());

        // check the link
        final JSONObject r1 = rules.getJSONObject(0);
        final JSONArray symbolizers1 = r1.getJSONArray("symbolizers");
        assertEquals(1, symbolizers1.size());
        final JSONObject s1 = symbolizers1.getJSONObject(0);
        final JSONObject graphic =
                s1.getJSONObject(JSONLegendGraphicBuilder.TEXT).getJSONObject(JSONLegendGraphicBuilder.GRAPHIC);

        assertEquals(
                "http://local-test:8080/geoserver/kml/icon/dynamicArrows?0.0.0=&0.0.0.rotation=0.0&0.0.0.size=16.0&npg=true",
                graphic.getString("url"));
    }

    @Test
    public void testElseFilter() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("PopulationElse.sld");
        req.setStyle(style);
        // printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotEmpty(result);

        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject pointSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
        JSONObject rule = rules.getJSONObject(2);
        assertEquals("true", rule.get(JSONLegendGraphicBuilder.ELSE_FILTER));
    }

    @Test
    public void testFullPoint() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("full_point.sld");
        req.setStyle(style);
        printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotEmpty(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject pointSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POINT);
        assertNotNull(pointSymb);
        assertEquals("[centroid(the_geom)]", pointSymb.get(JSONLegendGraphicBuilder.GEOMETRY));
        assertEquals("6", pointSymb.get(JSONLegendGraphicBuilder.SIZE));
        assertEquals("[rotation * '-1']", pointSymb.get(JSONLegendGraphicBuilder.ROTATION));
        assertEquals("0.4", pointSymb.get(JSONLegendGraphicBuilder.OPACITY));
    }

    @Test
    public void testSimpleLine() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("line.sld");
        req.setStyle(style);
        // printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        // System.out.println(result.toString(2));
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject lineSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.LINE);
        assertNotNull(lineSymb);

        assertEquals("#0000FF", lineSymb.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("2", lineSymb.get(JSONLegendGraphicBuilder.STROKE_WIDTH));
    }

    @Test
    public void testFullLine() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("full_line.sld");
        req.setStyle(style);
        // printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        print(result);
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject lineSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.LINE);
        assertFalse(lineSymb.isNullObject());
        JSONObject lineSymb1 = symbolizers.getJSONObject(1).getJSONObject(JSONLegendGraphicBuilder.LINE);
        assertFalse(lineSymb1.isNullObject());
        assertEquals("10", lineSymb.get(JSONLegendGraphicBuilder.PERPENDICULAR_OFFSET));
        final JSONObject graphicStroke1 = lineSymb1.getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_STROKE);
        assertFalse(graphicStroke1.isNullObject());
        assertEquals(
                "http://local-test:8080/geoserver/kml/icon/Default%20Styler?0.0.0=&0.0.0.rotation=0.0&npg=true",
                graphicStroke1.getString("url"));

        JSONObject lineSymb2 = symbolizers.getJSONObject(2).getJSONObject(JSONLegendGraphicBuilder.LINE);
        assertFalse(lineSymb2.isNullObject());
        final JSONObject graphicFill2 = lineSymb2.getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_FILL);

        assertFalse(graphicFill2.isNullObject());
        assertEquals(
                "http://local-test:8080/geoserver/kml/icon/Default%20Styler?0.0.1=&0.0.1.rotation=0.0&npg=true",
                graphicFill2.getString("url"));
    }

    @Test
    public void testSimplePolygon() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("polygon.sld");
        req.setStyle(style);
        // printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        // System.out.println(result.toString(2));
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject polySymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POLYGON);
        assertNotNull(polySymb);
        assertEquals("#0099CC", polySymb.get(JSONLegendGraphicBuilder.FILL));
        assertEquals("#000000", polySymb.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("0.5", polySymb.get(JSONLegendGraphicBuilder.STROKE_WIDTH));
    }

    @Test
    public void testFullPolygon() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("full_polygon.sld");
        req.setStyle(style);
        // printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject polySymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.POLYGON);
        assertNotNull(polySymb);
        assertEquals("#0099CC", polySymb.get(JSONLegendGraphicBuilder.FILL));
        assertEquals("#000000", polySymb.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("0.5", polySymb.get(JSONLegendGraphicBuilder.STROKE_WIDTH));
        JSONObject polySymb2 = symbolizers.getJSONObject(1).getJSONObject(JSONLegendGraphicBuilder.POLYGON);
        assertFalse(polySymb2.isNullObject() && polySymb2.isEmpty());
        JSONObject stroke = polySymb2.getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_STROKE);

        assertFalse(stroke.isNullObject() && stroke.isEmpty());
        JSONObject fill = polySymb2.getJSONObject(JSONLegendGraphicBuilder.GRAPHIC_FILL);
        assertFalse(fill.isNullObject() && fill.isEmpty());
    }

    @Test
    public void testSimpleText() throws Exception {
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("text.sld");
        req.setStyle(style);
        // printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotNull(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject polySymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.LINE);
        assertNotNull(polySymb);

        assertEquals("#000000", polySymb.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("0.2", polySymb.get(JSONLegendGraphicBuilder.STROKE_WIDTH));

        JSONObject textSymb = symbolizers.getJSONObject(1).getJSONObject(JSONLegendGraphicBuilder.TEXT);
        assertFalse(textSymb.isNullObject());
        assertEquals("[STATE_ABBR]", textSymb.getString(JSONLegendGraphicBuilder.LABEL));
        JSONArray fonts = textSymb.getJSONArray(JSONLegendGraphicBuilder.FONTS);
        assertEquals(2, fonts.size());
        assertEquals(
                "[STATE_FONT]",
                fonts.getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.FONT_FAMILY)
                        .get(0));
        assertEquals(
                "Lobster",
                fonts.getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.FONT_FAMILY)
                        .get(1));
        assertEquals(
                "Times New Roman",
                fonts.getJSONObject(0)
                        .getJSONArray(JSONLegendGraphicBuilder.FONT_FAMILY)
                        .get(2));
        assertEquals("Normal", fonts.getJSONObject(0).get(JSONLegendGraphicBuilder.FONT_STYLE));
        assertEquals("normal", fonts.getJSONObject(0).get(JSONLegendGraphicBuilder.FONT_WEIGHT));
        assertEquals("14", fonts.getJSONObject(0).get(JSONLegendGraphicBuilder.FONT_SIZE));
        assertEquals(
                "Times New Roman",
                fonts.getJSONObject(1)
                        .getJSONArray(JSONLegendGraphicBuilder.FONT_FAMILY)
                        .get(0));
        assertEquals("Italic", fonts.getJSONObject(1).get(JSONLegendGraphicBuilder.FONT_STYLE));
        assertEquals("normal", fonts.getJSONObject(1).get(JSONLegendGraphicBuilder.FONT_WEIGHT));
        assertEquals("9", fonts.getJSONObject(1).get(JSONLegendGraphicBuilder.FONT_SIZE));
        assertFalse(
                textSymb.getJSONObject(JSONLegendGraphicBuilder.LABEL_PLACEMENT).isNullObject());
        assertFalse(textSymb.getJSONObject(JSONLegendGraphicBuilder.HALO).isNullObject());
        JSONObject vops = textSymb.getJSONObject(JSONLegendGraphicBuilder.VENDOR_OPTIONS);
        assertNotEmpty(vops);
        assertEquals("true", vops.get("followLine"));
    }

    @Test
    public void testComplexText() throws Exception {

        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(MockData.MPOINTS.getNamespaceURI(), MockData.MPOINTS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        Style style = readSLD("text_scaleSize.sld");
        req.setStyle(style);
        // printStyle(style);
        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        // System.out.println(result.toString(2));
        assertNotEmpty(result);
        // blue 2px wide line
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject polySymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.LINE);
        assertNotNull(polySymb);

        assertEquals("#000000", polySymb.get(JSONLegendGraphicBuilder.STROKE));
        assertEquals("0.2", polySymb.get(JSONLegendGraphicBuilder.STROKE_WIDTH));

        JSONObject textSymb = symbolizers.getJSONObject(1).getJSONObject(JSONLegendGraphicBuilder.TEXT);

        assertFalse(textSymb.isNullObject());

        assertEquals("[STATE_ABBR]", textSymb.getString(JSONLegendGraphicBuilder.LABEL));
        JSONArray fonts = textSymb.getJSONArray(JSONLegendGraphicBuilder.FONTS);
        assertEquals(2, fonts.size());
        assertEquals("9", fonts.getJSONObject(1).get(JSONLegendGraphicBuilder.FONT_SIZE));
    }

    @Test
    public void testContrastRaster() throws Exception {

        Style multipleRulesStyle = readSLD("raster_brightness.sld");
        // printStyle(multipleRulesStyle);
        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = getRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);

        SimpleFeatureCollection feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.setLayer(feature.getSchema());
        req.setStyle(multipleRulesStyle);
        req.setLegendOptions(new HashMap<>());

        // use default values for the rest of parameters

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotEmpty(result);
        // System.out.println(result.toString(2));
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject rasterSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.RASTER);

        assertNotEmpty(rasterSymb);
        JSONObject ce = rasterSymb.getJSONObject(JSONLegendGraphicBuilder.CONTRAST_ENHANCEMENT);
        assertNotEmpty(ce);
        assertEquals("0.5", ce.getString(JSONLegendGraphicBuilder.GAMMA_VALUE));
        assertEquals("true", ce.get(JSONLegendGraphicBuilder.NORMALIZE));
    }

    @Test
    public void testDescreteRaster() throws Exception {

        Style multipleRulesStyle = readSLD("raster_discretecolors.sld");
        // printStyle(multipleRulesStyle);
        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest(WMS.get());
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);

        SimpleFeatureCollection feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.setLayer(feature.getSchema());
        req.setStyle(multipleRulesStyle);
        req.setLegendOptions(new HashMap<>());
        req.setFormat(JSONFormat);

        JSONObject result = this.legendProducer.buildLegendGraphic(req);
        assertNotEmpty(result);
        JSONArray legend = result.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertNotNull(legend);
        JSONArray rules = legend.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.SYMBOLIZERS);
        assertNotNull(symbolizers);
        assertFalse(symbolizers.isEmpty());

        JSONObject rasterSymb = symbolizers.getJSONObject(0).getJSONObject(JSONLegendGraphicBuilder.RASTER);

        assertNotEmpty(rasterSymb);
        JSONObject ce = rasterSymb.getJSONObject(JSONLegendGraphicBuilder.CONTRAST_ENHANCEMENT);
        assertTrue(ce.isEmpty());
        JSONObject colormap = rasterSymb.getJSONObject(JSONLegendGraphicBuilder.COLORMAP);

        assertEquals("intervals", colormap.get(JSONLegendGraphicBuilder.COLORMAP_TYPE));
    }

    @Test
    public void testLegendSelectionInRule() throws Exception {
        // test that the <VendorOption name=renderingLegend>false</VendorOption> works with the Json
        // output
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.MPOLYGONS.getNamespaceURI(), MockData.MPOLYGONS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(getCatalog().getStyleByName("styleWithLegendSelection").getStyle());
        req.setFormat(JSONFormat);
        JSONObject jsonLegend = this.legendProducer.buildLegendGraphic(req);
        JSONArray legend = jsonLegend.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        JSONArray rules = legend.getJSONObject(0).getJSONArray("rules");
        // only one rule should be included in the output the other one has the renderingLegend
        // option set to false.
        assertEquals(1, rules.size());
        String ruleName = rules.getJSONObject(0).getString("name");
        assertEquals("nationalpark", ruleName);
    }

    @Test
    public void testLegendSelectionInSymbolizer() throws Exception {
        // test that the <VendorOption name=renderingLegend>false</VendorOption> option set in
        // symbolizer works
        // avoiding the inclusion in the final output of one of the two symbolizers
        GetLegendGraphicRequest req = getRequest();
        req.setWidth(20);
        req.setHeight(20);

        FeatureTypeInfo ftInfo = getCatalog()
                .getFeatureTypeByName(MockData.MPOLYGONS.getNamespaceURI(), MockData.MPOLYGONS.getLocalPart());

        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(getCatalog()
                .getStyleByName("styleWithLegendSelectionOnSymbolizer")
                .getStyle());
        req.setFormat(JSONFormat);
        JSONObject jsonLegend = this.legendProducer.buildLegendGraphic(req);
        JSONArray rules = jsonLegend
                .getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                .getJSONObject(0)
                .getJSONArray("rules");
        assertEquals(1, rules.size());
        JSONArray symbolizers = rules.getJSONObject(0).getJSONArray("symbolizers");
        // only one symbolizer of the two defined should be present in the final output
        // the other one has the renderingLabel option set to false
        assertEquals(1, symbolizers.size());
    }

    @Test
    public void testLanguageVector() throws Exception {
        // needs to go through the dispatcher for language() function to work
        String base = "wms?request=GetLegendGraphic&layer=MPolygons&style=multiLanguageVector&format"
                + "=application/json&legend_options=hideEmptyRules:true";
        checkLanguageRule("it", base);
        checkLanguageRule("de", base);
    }

    @Test
    public void testLanguageRaster() throws Exception {
        // needs to go through the dispatcher for language() function to work
        String base = "wms?request=GetLegendGraphic&layer=MPolygons&style=multiLanguageRaster&format"
                + "=application/json&legend_options=hideEmptyRules:true";
        checkLanguageRule(null, base);
        checkLanguageRule("", base);
        checkLanguageRule("it", base);
        checkLanguageRule("de", base);
    }

    @Test
    public void testGroupDefaultStyle() throws Exception {
        JSONObject json = (JSONObject) getAsJSON(
                "wms?request=GetLegendGraphic&layer=nature&style=default-style-nature&format" + "=application/json");
        JSONArray legend = json.getJSONArray("Legend");
        assertEquals(2, legend.size());
        assertEquals("Lakes", legend.getJSONObject(0).getString("layerName"));
        assertEquals("Forests", legend.getJSONObject(1).getString("layerName"));
    }

    @Test
    public void testGroupInvalidStyleName() throws Exception {
        Document dom =
                getAsDOM("wms?request=GetLegendGraphic&layer=nature&style=notAStyleName&format" + "=application/json");
        assertXpathEvaluatesTo("StyleNotDefined", "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String exception = xpath.evaluate("/ogc:ServiceExceptionReport/ogc:ServiceException", dom);
        assertEquals("No such style: notAStyleName", exception.trim());
    }

    private void checkLanguageRule(String lang, String base) throws Exception {
        String suffix = lang != null ? "&language=" + lang : "";
        JSONObject json = (JSONObject) getAsJSON(base + suffix, 200);
        print(json);

        JSONArray rules = json.getJSONArray(JSONLegendGraphicBuilder.LEGEND)
                .getJSONObject(0)
                .getJSONArray("rules");
        assertEquals(1, rules.size());
        String filter = rules.getJSONObject(0).getString("filter");
        if (lang != null) assertEquals("[language() = '" + lang + "']", filter);
        else assertEquals("[language() = '']", filter);
    }

    /** @param result */
    private void assertNotEmpty(JSONObject result) {
        assertNotNull(result);
        assertFalse(result.isNullObject());
        assertFalse(result.isEmpty());
    }

    /** */
    private Style readSLD(String sldName) throws IOException {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        SLDParser stylereader = new SLDParser(styleFactory, getClass().getResource(sldName));
        Style[] readStyles = stylereader.readXML();

        Style style = readStyles[0];
        return style;
    }

    @SuppressWarnings("PMD.SystemPrintln")
    private void printStyle(Style style) throws TransformerException {
        if (isQuietTests()) {
            return;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SLDTransformer transformer = new SLDTransformer();
        transformer.setIndentation(2);
        transformer.transform(style, bos);
        String styleStr = bos.toString();
        System.out.println(styleStr);
    }
    /** basic setup */
    private GetLegendGraphicRequest getRequest() {
        return getRequest(null, null);
    }

    private GetLegendGraphicRequest getRequest(FeatureType layer, Style style) {

        GetLegendGraphicRequest req = new GetLegendGraphicRequest(WMS.get());
        req.setBaseUrl("http://local-test:8080/geoserver");
        req.setLayer(layer);
        req.setStyle(style);
        req.setFormat(JSONFormat);
        return req;
    }

    @Test
    public void testLayerGroupStyleSingle() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            String lgStyleName = "nature-style";
            String lgName = "single_lake_and_places";
            LayerInfo forestL = getCatalog().getLayerByName("cite:Forests");
            LayerInfo lakesL = getCatalog().getLayerByName("cite:Lakes");
            group = lakesAndPlacesWithGroupStyle(
                    lgName,
                    LayerGroupInfo.Mode.SINGLE,
                    lgStyleName,
                    Arrays.asList(forestL, lakesL),
                    Arrays.asList(null, null));
            String url = "wms?LAYER="
                    + group.getName()
                    + "&STYLE=nature-style&FORMAT=application/json"
                    + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION=1.0.0&WIDTH=20&HEIGHT=20";
            JSONObject legend = (JSONObject) getAsJSON(url);
            JSONObject forest = legend.getJSONArray("Legend").getJSONObject(0);
            JSONObject lakes = legend.getJSONArray("Legend").getJSONObject(1);
            assertEquals("Forests", forest.get("layerName"));
            assertEquals("Lakes", lakes.get("layerName"));
        } finally {
            if (group != null) {
                catalog.remove(group);
            }
        }
    }

    @Test
    public void testLayerGroupStyleOpaque() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            String lgStyleName = "nature-style";
            String lgName = "opaque_lakes_and_places";
            LayerInfo forestL = getCatalog().getLayerByName("cite:Forests");
            LayerInfo lakesL = getCatalog().getLayerByName("cite:Lakes");
            group = lakesAndPlacesWithGroupStyle(
                    lgName,
                    LayerGroupInfo.Mode.OPAQUE_CONTAINER,
                    lgStyleName,
                    Arrays.asList(forestL, lakesL),
                    Arrays.asList(null, null));
            String url = "wms?LAYER="
                    + group.getName()
                    + "&STYLE=nature-style&FORMAT=application/json"
                    + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION=1.0.0&WIDTH=20&HEIGHT=20";
            JSONObject legend = (JSONObject) getAsJSON(url);
            JSONObject forest = legend.getJSONArray("Legend").getJSONObject(0);
            JSONObject lakes = legend.getJSONArray("Legend").getJSONObject(1);
            assertEquals("Forests", forest.get("layerName"));
            assertEquals("Lakes", lakes.get("layerName"));
        } finally {
            if (group != null) catalog.remove(group);
        }
    }

    @Test
    public void testNestedGroupWithStyle() throws Exception {
        LayerGroupInfo nested = null;
        LayerGroupInfo container = null;
        Catalog catalog = getCatalog();

        try {
            String lgName = "nested-lakes_and_places_group";
            LayerInfo forestL = getCatalog().getLayerByName("cite:Forests");
            List<StyleInfo> styles = new ArrayList<>();
            styles.add(null);
            nested = lakesAndPlacesWithGroupStyle(
                    lgName, LayerGroupInfo.Mode.SINGLE, "forest-style", Arrays.asList(forestL), styles);

            createLakesPlacesLayerGroup(catalog, "lakes-and-place", LayerGroupInfo.Mode.SINGLE, null);
            container = catalog.getLayerGroupByName("lakes-and-place");
            container.getLayers().add(0, nested);
            container.getStyles().add(0, nested.getLayerGroupStyles().get(0).getName());
            catalog.save(container);

            String url = "wms?LAYER="
                    + container.getName()
                    + "&STYLE=&FORMAT=application/json"
                    + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION=1.0.0&WIDTH=20&HEIGHT=20";
            JSONObject legend = (JSONObject) getAsJSON(url);
            JSONObject forest = legend.getJSONArray("Legend").getJSONObject(0);
            JSONObject lakes = legend.getJSONArray("Legend").getJSONObject(1);
            JSONObject places = legend.getJSONArray("Legend").getJSONObject(2);
            assertEquals("Forests", forest.get("layerName"));
            assertEquals("Lakes", lakes.get("layerName"));
            assertEquals("NamedPlaces", places.get("layerName"));
        } finally {
            if (container != null) catalog.remove(container);
            if (nested != null) catalog.remove(nested);
        }
    }

    @Test
    public void testLayerGroupStyleIngnoredIfTree() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            String lgName = "lakes_and_places_named";
            LayerInfo forestL = getCatalog().getLayerByName("cite:Forests");
            LayerInfo lakesL = getCatalog().getLayerByName("cite:Lakes");
            group = lakesAndPlacesWithGroupStyle(
                    lgName,
                    LayerGroupInfo.Mode.NAMED,
                    "nature-style",
                    Arrays.asList(forestL, lakesL),
                    Arrays.asList(null, null));
            String url = "wms?LAYER="
                    + group.getName()
                    + "&STYLE=nature-style&FORMAT=application/json"
                    + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION=1.0.0&WIDTH=20&HEIGHT=20";
            JSONObject legend = (JSONObject) getAsJSON(url);
            JSONObject lakes = legend.getJSONArray("Legend").getJSONObject(0);
            JSONObject places = legend.getJSONArray("Legend").getJSONObject(1);
            assertEquals("Lakes", lakes.get("layerName"));
            assertEquals("NamedPlaces", places.get("layerName"));
        } finally {
            if (group != null) catalog.remove(group);
        }
    }

    @Test
    public void testHideEmptyRules() throws Exception {
        String url = "wms?LAYER="
                + MockData.NAMED_PLACES.getLocalPart()
                + "&FORMAT=application/json"
                + "&SERVICE=WMS&REQUEST=GetLegendGraphic&VERSION=1.0.0&WIDTH=20&HEIGHT=20";
        JSONObject legend = (JSONObject) getAsJSON(url);
        JSONArray rules = legend.getJSONArray("Legend").getJSONObject(0).getJSONArray("rules");
        assertEquals(2, rules.size());
        assertEquals("ashton", rules.getJSONObject(0).getString("name"));
        assertEquals("goose_island", rules.getJSONObject(1).getString("name"));

        // goose_island is placed on negative Y ordinates
        url += "&LEGEND_OPTIONS=hideEmptyRules:true&BBOX=0,0,0.1,0.1";
        legend = (JSONObject) getAsJSON(url);
        rules = legend.getJSONArray("Legend").getJSONObject(0).getJSONArray("rules");
        assertEquals(1, rules.size());
        assertEquals("ashton", rules.getJSONObject(0).getString("name"));
    }
}
