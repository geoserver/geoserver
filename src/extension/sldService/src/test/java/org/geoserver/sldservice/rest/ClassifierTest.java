/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import it.geosolutions.jaiext.JAIExt;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import javax.xml.namespace.QName;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.rest.RestBaseController;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.FilterFunction_parseDouble;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.xml.styling.SLDParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ClassifierTest extends SLDServiceBaseTest {

    private static final int DEFAULT_INTERVALS = 2;

    static final QName CLASSIFICATION_POINTS =
            new QName(SystemTestData.CITE_URI, "ClassificationPoints", SystemTestData.CITE_PREFIX);

    static final QName CLASSIFICATION_POLYGONS =
            new QName(
                    SystemTestData.CITE_URI, "ClassificationPolygons", SystemTestData.CITE_PREFIX);

    static final QName MILANOGEO =
            new QName(SystemTestData.CITE_URI, "milanogeo", SystemTestData.CITE_PREFIX);

    static final QName TAZBYTE =
            new QName(SystemTestData.CITE_URI, "tazbyte", SystemTestData.CITE_PREFIX);

    static final QName DEM_FLOAT =
            new QName(SystemTestData.CITE_URI, "dem", SystemTestData.CITE_PREFIX);

    static final QName SRTM =
            new QName(SystemTestData.CITE_URI, "srtm", SystemTestData.CITE_PREFIX);

    private static final String sldPrefix =
            "<StyledLayerDescriptor><NamedLayer><Name>feature</Name><UserStyle><FeatureTypeStyle>";
    private static final String sldPostfix =
            "</FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";

    private static final double EPS = 1e-6;

    @BeforeClass
    public static void setupJaiExt() {
        JAIExt.initJAIEXT(true, true);
    }

    @AfterClass
    public static void cleanupJaiExt() {
        JAIExt.initJAIEXT(false, true);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        Map<LayerProperty, Object> props = new HashMap<>();
        testData.addVectorLayer(
                CLASSIFICATION_POINTS,
                props,
                "ClassificationPoints.properties",
                this.getClass(),
                getCatalog());

        testData.addVectorLayer(
                CLASSIFICATION_POLYGONS,
                props,
                "ClassificationPolygons.properties",
                this.getClass(),
                getCatalog());

        testData.addRasterLayer(
                MILANOGEO, "milanogeo.tif", "tif", null, this.getClass(), getCatalog());

        testData.addRasterLayer(
                TAZBYTE, "tazbyte.tiff", "tif", null, SystemTestData.class, getCatalog());

        testData.addRasterLayer(
                DEM_FLOAT, "dem_float.tif", "tif", null, this.getClass(), getCatalog());

        testData.addRasterLayer(SRTM, "srtm.tif", "tif", null, this.getClass(), getCatalog());
    }

    @Test
    public void testClassifyForFeatureDefault() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                        DEFAULT_INTERVALS);
        checkRule(rules[0], "#680000", org.opengis.filter.And.class);
        checkRule(rules[1], "#B20000", org.opengis.filter.And.class);

        assertFalse(resultXml.indexOf("StyledLayerDescriptor") != -1);
    }

    @Test
    public void testCustomStroke() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&strokeColor=0xFF0000&strokeWeight=5";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                        DEFAULT_INTERVALS);
        checkRule(rules[0], "#680000", org.opengis.filter.And.class);
        checkRule(rules[1], "#B20000", org.opengis.filter.And.class);
        checkStroke(rules[0], "#FF0000", "5.0");
        checkStroke(rules[1], "#FF0000", "5.0");
    }

    @Test
    public void testCustomClasses() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&customClasses=1,10,#FF0000;10,20,#00FF00;20,30,#0000FF";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
        checkRule(rules[0], "#FF0000", org.opengis.filter.And.class);
        checkRule(rules[1], "#00FF00", org.opengis.filter.And.class);
        checkRule(rules[2], "#0000FF", org.opengis.filter.And.class);
    }

    @Test
    public void testCustomColors1() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&ramp=custom&colors=#FF0000,#00FF00,#0000FF";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
        checkRule(rules[0], "#FF0000", org.opengis.filter.And.class);
        checkRule(rules[1], "#00FF00", org.opengis.filter.And.class);
        checkRule(rules[2], "#0000FF", org.opengis.filter.And.class);
    }

    @Test
    public void testCustomColors2() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=2&ramp=custom&colors=#FF0000,#00FF00,#0000FF";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 2);
        checkRule(rules[0], "#FF0000", org.opengis.filter.And.class);
        checkRule(rules[1], "#00FF00", org.opengis.filter.And.class);
    }

    @Test
    public void testCustomColors3() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=15&ramp=custom&colors=#FF0000,#00FF00,#0000FF";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                        15);
        checkRule(rules[0], "#FF0000", org.opengis.filter.And.class);
        checkRule(rules[1], "#D42A00", org.opengis.filter.And.class);
        checkRule(rules[2], "#AA5500", org.opengis.filter.And.class);
        checkRule(rules[3], "#7F7F00", org.opengis.filter.And.class);
        checkRule(rules[4], "#55AA00", org.opengis.filter.And.class);
        checkRule(rules[5], "#2AD400", org.opengis.filter.And.class);
        checkRule(rules[6], "#00FF00", org.opengis.filter.And.class);
        checkRule(rules[7], "#00E21C", org.opengis.filter.And.class);
        checkRule(rules[8], "#00C538", org.opengis.filter.And.class);
        checkRule(rules[9], "#00A954", org.opengis.filter.And.class);
        checkRule(rules[10], "#008D71", org.opengis.filter.And.class);
        checkRule(rules[11], "#00718D", org.opengis.filter.And.class);
        checkRule(rules[12], "#0054A9", org.opengis.filter.And.class);
        checkRule(rules[13], "#0038C6", org.opengis.filter.And.class);
        checkRule(rules[14], "#001CE2", org.opengis.filter.And.class);
    }

    @Test
    public void testFullSLD() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&fullSLD=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        assertTrue(resultXml.indexOf("StyledLayerDescriptor") != -1);
    }

    @Test
    public void testClassifyOpenRange() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=id&intervals=3&open=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
        checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsLessThanOrEqualTo.class);
        checkRule(rules[1], "#8C0000", org.opengis.filter.And.class);
        checkRule(rules[2], "#C30000", org.opengis.filter.PropertyIsGreaterThan.class);
    }

    @Test
    public void testQuantile() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&open=true&method=quantile";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        assertTrue(rules[0].getTitle().contains("20.0"));
        assertTrue(rules[1].getTitle().contains("20.0"));
        assertTrue(rules[2].getTitle().contains("61.0"));
    }

    @Test
    public void testEqualArea() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPolygons/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=5&open=true&method=equalArea";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 4);

        assertEquals(" <= 43.0", rules[0].getDescription().getTitle().toString());
        assertEquals(" > 43.0 AND <= 61.0", rules[1].getDescription().getTitle().toString());
        assertEquals(" > 61.0 AND <= 90.0", rules[2].getDescription().getTitle().toString());
        assertEquals(" > 90.0", rules[3].getDescription().getTitle().toString());
    }

    @Test
    public void testJenks() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&open=true&method=jenks";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        assertTrue(rules[0].getTitle().contains("12.0"));
        assertTrue(rules[1].getTitle().contains("12.0"));
        assertTrue(rules[1].getTitle().contains("29.0"));
        assertTrue(rules[2].getTitle().contains("29.0"));
    }

    @Test
    public void testEqualInterval() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&open=true&method=equalInterval";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        assertTrue(rules[0].getTitle().contains("32.6"));
        assertTrue(rules[1].getTitle().contains("32.6"));
        assertTrue(rules[1].getTitle().contains("61.3"));
        assertTrue(rules[2].getTitle().contains("61.3"));
    }

    @Test
    public void testUnique() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=name&intervals=3&method=uniqueInterval";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);
        checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#8C0000", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#C30000", org.opengis.filter.PropertyIsEqualTo.class);
        TreeSet<String> orderedRules = new TreeSet<String>();
        orderedRules.add(rules[0].getTitle());
        orderedRules.add(rules[1].getTitle());
        orderedRules.add(rules[2].getTitle());
        Iterator iter = orderedRules.iterator();
        assertEquals("bar", iter.next());
        assertEquals("foo", iter.next());
        assertEquals("foobar", iter.next());
    }

    @Test
    public void testBlueRamp() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=name&intervals=3&method=uniqueInterval&ramp=blue";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        checkRule(rules[0], "#000055", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#00008C", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#0000C3", org.opengis.filter.PropertyIsEqualTo.class);
    }

    @Test
    public void testReverse() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=name&intervals=3&method=uniqueInterval&ramp=blue&reverse=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        checkRule(rules[0], "#0000C3", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#00008C", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#000055", org.opengis.filter.PropertyIsEqualTo.class);
    }

    @Test
    public void testNormalize() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=id&intervals=3&open=true&normalize=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsLessThanOrEqualTo.class);
        org.opengis.filter.PropertyIsLessThanOrEqualTo filter =
                (org.opengis.filter.PropertyIsLessThanOrEqualTo) rules[0].getFilter();
        assertTrue(filter.getExpression1() instanceof FilterFunction_parseDouble);
    }

    @Test
    public void testCustomRamp() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=name&intervals=3&method=uniqueInterval&ramp=custom&startColor=0xFF0000&endColor=0x0000FF";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        checkRule(rules[0], "#FF0000", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#7F007F", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#0000FF", org.opengis.filter.PropertyIsEqualTo.class);
    }

    private Rule[] checkRules(String resultXml, int classes) {
        Rule[] rules = checkSLD(resultXml);
        assertEquals(classes, rules.length);
        return rules;
    }

    private void checkStroke(Rule rule, String color, String weight) {
        assertNotNull(rule.getSymbolizers());
        assertEquals(1, rule.getSymbolizers().length);
        assertTrue(rule.getSymbolizers()[0] instanceof PointSymbolizer);
        PointSymbolizer symbolizer = (PointSymbolizer) rule.getSymbolizers()[0];
        assertNotNull(symbolizer.getGraphic());
        assertEquals(1, symbolizer.getGraphic().getMarks().length);
        assertNotNull(symbolizer.getGraphic().getMarks()[0].getStroke());
        assertEquals(
                color, symbolizer.getGraphic().getMarks()[0].getStroke().getColor().toString());
        assertEquals(
                weight, symbolizer.getGraphic().getMarks()[0].getStroke().getWidth().toString());
    }

    private void checkRule(Rule rule, String color, Class<?> filterType) {
        assertNotNull(rule.getFilter());
        assertTrue(filterType.isAssignableFrom(rule.getFilter().getClass()));
        assertNotNull(rule.getSymbolizers());
        assertEquals(1, rule.getSymbolizers().length);
        assertTrue(rule.getSymbolizers()[0] instanceof PointSymbolizer);
        PointSymbolizer symbolizer = (PointSymbolizer) rule.getSymbolizers()[0];
        assertNotNull(symbolizer.getGraphic());
        assertEquals(1, symbolizer.getGraphic().getMarks().length);
        assertNotNull(symbolizer.getGraphic().getMarks()[0].getFill());
        assertEquals(color, symbolizer.getGraphic().getMarks()[0].getFill().getColor().toString());
    }

    @Test
    public void testRasterUniqueBinary() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:milanogeo/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=uniqueInterval&ramp=blue&fullSLD=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(2, entries.length);
        assertEquals(CQL.toExpression("0.0"), entries[0].getQuantity());
        assertEquals(CQL.toExpression("'#000068'"), entries[0].getColor());
        assertEquals(CQL.toExpression("1.0"), entries[1].getQuantity());
        assertEquals(CQL.toExpression("'#0000B2'"), entries[1].getColor());
    }

    @Test
    public void testRasterUniqueByte() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:tazbyte/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=uniqueInterval&ramp=blue&fullSLD=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(167, entries.length);
        assertEquals(CQL.toExpression("1.0"), entries[0].getQuantity());
        assertEquals(CQL.toExpression("'#00001E'"), entries[0].getColor());
        assertEquals(CQL.toExpression("178.0"), entries[166].getQuantity());
        // this color is too dark, believe there is a bug in the ramps
        assertEquals(CQL.toExpression("'#000056'"), entries[166].getColor());
    }

    @Test
    public void testEqualIntervalDem() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:dem/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=equalInterval&intervals=5&ramp=jet&fullSLD=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(6, entries.length);
        assertEntry(entries[0], -1, null, "#000000", 0); // transparent entry
        assertEntry(entries[1], 249.2, ">= -1 AND < 249.2", "#0000FF", 1);
        assertEntry(entries[2], 499.4, ">= 249.2 AND < 499.4", "#FFFF00", 1);
        assertEntry(entries[3], 749.6, ">= 499.4 AND < 749.6", "#FFAA00", 1);
        assertEntry(entries[4], 999.8, ">= 749.6 AND < 999.8", "#FF5500", 1);
        assertEntry(entries[5], 1250, ">= 999.8 AND <= 1250", "#FF0000", 1);
    }

    @Test
    public void testEqualIntervalContinousDem() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:dem/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=equalInterval&intervals=5&ramp=jet&fullSLD=true&continuous=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertEntry(entries[0], -1, "-1", "#0000FF", 1);
        assertEntry(entries[1], 311.75, "311.75", "#FFFF00", 1);
        assertEntry(entries[2], 624.5, "624.5", "#FFAA00", 1);
        assertEntry(entries[3], 937.25, "937.25", "#FF5500", 1);
        assertEntry(entries[4], 1250, "1250", "#FF0000", 1);
    }

    @Test
    public void testQuantileIntervalsSrtm() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5&ramp=jet&fullSLD=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(6, entries.length);
        assertEntry(entries[0], -2, null, "#000000", 0); // transparent entry
        assertEntry(entries[1], 237, ">= -2 AND < 237", "#0000FF", 1);
        assertEntry(entries[2], 441, ">= 237 AND < 441", "#FFFF00", 1);
        assertEntry(entries[3], 640, ">= 441 AND < 640", "#FFAA00", 1);
        assertEntry(entries[4], 894, ">= 640 AND < 894", "#FF5500", 1);
        assertEntry(entries[5], 1796, ">= 894 AND <= 1796", "#FF0000", 1);
    }

    @Test
    public void testQuantileContinuousSrtm() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5&ramp=jet&fullSLD=true&continuous=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertEntry(entries[0], -2, "-2", "#0000FF", 1);
        assertEntry(entries[1], 292, "292", "#FFFF00", 1);
        assertEntry(entries[2], 536, "536", "#FFAA00", 1);
        assertEntry(entries[3], 825, "825", "#FF5500", 1);
        assertEntry(entries[4], 1796, "1796", "#FF0000", 1);
    }

    @Test
    public void testJenksIntervalsSrtm() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=jenks&intervals=5&ramp=jet&fullSLD=true&continuous=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertEntry(entries[0], -2, "-2", "#0000FF", 1);
        assertEntry(entries[1], 336, "336", "#FFFF00", 1);
        assertEntry(entries[2], 660, "660", "#FFAA00", 1);
        assertEntry(entries[3], 1011, "1011", "#FF5500", 1);
        assertEntry(entries[4], 1796, "1796", "#FF0000", 1);
    }

    @Test
    public void testRasterCustomClassesInterval() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "customClasses=1,10,#FF0000;10,20,#00FF00;20,30,#0000FF&fullSLD=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(4, entries.length);
        assertEntry(entries[0], 1, null, "#000000", 0); // transparent entry
        assertEntry(entries[1], 10, ">= 1 AND < 10", "#FF0000", 1);
        assertEntry(entries[2], 20, ">= 10 AND < 20", "#00FF00", 1);
        assertEntry(entries[3], 30, ">= 20 AND <= 30", "#0000FF", 1);
    }

    @Test
    public void testRasterCustomClassesContinuous() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "customClasses=1,10,#FF0000;10,20,#00FF00;20,30,#0000FF&fullSLD=true&continuous=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(3, entries.length);
        assertEntry(entries[0], 1, "1", "#FF0000", 1);
        assertEntry(entries[1], 10, "10", "#00FF00", 1);
        assertEntry(entries[2], 20, "20", "#0000FF", 1);
    }

    /**
     * Parses the DOM, check there is just one feature type style and one rule, and a single
     * symbolizer of type RasterSymbolizer, and returns it
     */
    private RasterSymbolizer getRasterSymbolizer(Document dom) {
        List<Rule> rules = getRules(dom);
        assertEquals(1, rules.size());
        List<Symbolizer> symbolizers = rules.get(0).symbolizers();
        assertEquals(1, symbolizers.size());
        assertThat(symbolizers.get(0), instanceOf(RasterSymbolizer.class));
        return (RasterSymbolizer) symbolizers.get(0);
    }

    /** Parses the DOM, check there is just one feature type style and returns all rules in it */
    private List<Rule> getRules(Document dom) {
        SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory());
        StyledLayerDescriptor sld = parser.parseDescriptor(dom.getDocumentElement());
        NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
        Style style = layer.getStyles()[0];
        List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
        assertEquals(1, featureTypeStyles.size());
        return featureTypeStyles.get(0).rules();
    }

    @Override
    protected String getServiceUrl() {
        return "classify";
    }

    private void assertEntry(
            ColorMapEntry entry, double value, String label, String color, double opacity) {
        assertEquals(value, entry.getQuantity().evaluate(null, Double.class), EPS);
        assertEquals(label, entry.getLabel());
        assertEquals(color, entry.getColor().evaluate(null, String.class));
        double actualOpacity =
                Optional.ofNullable(entry.getOpacity())
                        .map(o -> o.evaluate(null, Double.class))
                        .orElse((double) 1);
        assertEquals(opacity, actualOpacity, EPS);
    }
}
