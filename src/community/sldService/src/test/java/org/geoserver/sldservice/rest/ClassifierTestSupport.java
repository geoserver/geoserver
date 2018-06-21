/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import javax.xml.namespace.QName;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.rest.RestBaseController;
import org.geotools.filter.function.FilterFunction_parseDouble;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ClassifierTestSupport extends SLDServiceBaseTest {

    private static final int DEFAULT_INTERVALS = 2;

    private static final String sldPrefix =
            "<StyledLayerDescriptor><NamedLayer><Name>feature</Name><UserStyle><FeatureTypeStyle>";
    private static final String sldPostfix =
            "</FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";

    @Before
    public void setUp() throws Exception {

        QName CLASSIFICATION_POINTS =
                new QName(
                        SystemTestData.CITE_URI,
                        "ClassificationPoints",
                        SystemTestData.CITE_PREFIX);
        Map<LayerProperty, Object> props = new HashMap<LayerProperty, Object>();
        getTestData()
                .addVectorLayer(
                        CLASSIFICATION_POINTS,
                        props,
                        "ClassificationPoints.properties",
                        this.getClass(),
                        getCatalog());
        // getTestData().addVectorLayer(qName, props, catalog);
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
        assertTrue(rules[1].getTitle().contains("61.0"));
        assertTrue(rules[2].getTitle().contains("61.0"));
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

    @Test
    public void testClassifyForCoverageIsEmpty() throws Exception {

        final String restPath =
                RestBaseController.ROOT_PATH + "/sldservice/wcs:World/" + getServiceUrl() + ".xml";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        assertTrue(resultXml.indexOf("<Rules/>") != -1);
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

    @Override
    protected String getServiceUrl() {
        return "classify";
    }
}
