/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.geoserver.sldservice.utils.classifier.RasterSymbolizerBuilder.DEFAULT_MAX_PIXELS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.media.jai.PlanarImage;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.rest.RestBaseController;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.FilterFunction_parseDouble;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.xml.styling.SLDParser;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
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
    public static final String MULTIBAND_VIEW = "multiband_select";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no need for built-in layers
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        Map<LayerProperty, Object> props = new HashMap<>();
        Catalog catalog = getCatalog();
        testData.addVectorLayer(
                CLASSIFICATION_POINTS,
                props,
                "ClassificationPoints.properties",
                this.getClass(),
                catalog);

        testData.addVectorLayer(
                CLASSIFICATION_POLYGONS,
                props,
                "ClassificationPolygons.properties",
                this.getClass(),
                catalog);

        testData.addRasterLayer(MILANOGEO, "milanogeo.tif", "tif", null, this.getClass(), catalog);

        testData.addRasterLayer(
                TAZBYTE, "tazbyte.tiff", "tif", null, SystemTestData.class, catalog);

        testData.addRasterLayer(DEM_FLOAT, "dem_float.tif", "tif", null, this.getClass(), catalog);

        testData.addRasterLayer(SRTM, "srtm.tif", "tif", null, this.getClass(), catalog);

        // for coverage view band selection testing
        testData.addDefaultRasterLayer(SystemTestData.MULTIBAND, catalog);

        // setup the coverage view
        final InputCoverageBand ib0 = new InputCoverageBand("multiband", "2");
        final CoverageBand b0 =
                new CoverageBand(
                        Collections.singletonList(ib0),
                        "multiband@2",
                        0,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand ib1 = new InputCoverageBand("multiband", "1");
        final CoverageBand b1 =
                new CoverageBand(
                        Collections.singletonList(ib1),
                        "multiband@1",
                        1,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand ib2 = new InputCoverageBand("multiband", "0");
        final CoverageBand b2 =
                new CoverageBand(
                        Collections.singletonList(ib2),
                        "multiband@0",
                        2,
                        CompositionType.BAND_SELECT);

        final List<CoverageBand> coverageBands = new ArrayList<>();
        coverageBands.add(b0);
        coverageBands.add(b1);
        coverageBands.add(b2);

        CoverageView multiBandCoverageView = new CoverageView(MULTIBAND_VIEW, coverageBands);

        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName("multiband");
        CatalogBuilder builder = new CatalogBuilder(catalog);

        // Reordered bands coverage
        CoverageInfo coverageInfo =
                multiBandCoverageView.createCoverageInfo(MULTIBAND_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        catalog.add(coverageInfo);
        final LayerInfo layerInfoView = builder.buildLayer(coverageInfo);
        catalog.add(layerInfoView);
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
        checkRule(rules[0], "#8E0000", org.opengis.filter.And.class);
        checkRule(rules[1], "#FF0000", org.opengis.filter.And.class);

        assertFalse(resultXml.indexOf("StyledLayerDescriptor") != -1);
    }

    @Test
    public void testClassifyWrongAttribute() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foobar";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertEquals(400, response.getStatus());
        final String xml = response.getContentAsString();
        assertThat(
                xml,
                containsString(
                        "Could not find property foobar, available attributes are: id, name, foo, bar, geom, group"));
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
        checkRule(rules[0], "#8E0000", org.opengis.filter.And.class);
        checkRule(rules[1], "#FF0000", org.opengis.filter.And.class);
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
        checkRule(rules[0], "#690000", org.opengis.filter.PropertyIsLessThan.class);
        checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        checkRule(rules[2], "#FF0000", org.opengis.filter.PropertyIsGreaterThanOrEqualTo.class);
    }

    @Test
    public void testInvalidStdDev() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&open=false&stddevs=-1&fullSLD=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 400);
        assertThat(
                response.getContentAsString(),
                containsString("stddevs must be a positive floating point number"));
    }

    @Test
    public void testClassifyEqualIntervalsStdDevSmaller() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&open=false&stddevs=1&fullSLD=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);

        // stddev filter cuts 4 and 90 away leaving 8 and 61 as the extremes
        // System.out.println(response.getContentAsString());

        Rule[] rules = checkRules(response.getContentAsString(), 3);
        Filter f1 = checkRule(rules[0], "#690000", org.opengis.filter.And.class);
        assertFilter("foo >= 8 and foo < 25.667", f1);
        Filter f2 = checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        assertFilter("foo >= 25.667 and foo < 43.333", f2);
        Filter f3 = checkRule(rules[2], "#FF0000", org.opengis.filter.And.class);
        assertFilter("foo >= 43.333 and foo <= 61", f3);
    }

    @Test
    public void testClassifyEqualIntervalsBBoxStdDev() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&open=false&stddevs=1&fullSLD=true&bbox=6,5,50,45";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);

        // bbox leaves 8,12,20,29,43, stddev filter leaves 12,20,29
        // System.out.println(response.getContentAsString());

        Rule[] rules = checkRules(response.getContentAsString(), 3);
        Filter f1 = checkRule(rules[0], "#690000", org.opengis.filter.And.class);
        assertFilter("foo >= 12 and foo < 17.6667", f1);
        Filter f2 = checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        assertFilter("foo >= 17.6667 and foo < 23.3333", f2);
        Filter f3 = checkRule(rules[2], "#FF0000", org.opengis.filter.And.class);
        assertFilter("foo >= 23.3333 and foo <= 29", f3);
    }

    @Test
    public void testClassifyEqualIntervalsStdDevAll() throws Exception {
        // the stddev range will cover the entire data set
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&open=false&stddevs=3&fullSLD=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);

        // stddev filter cuts 4 and 90 away leaving 8 and 61 as the extremes
        // System.out.println(response.getContentAsString());

        Rule[] rules = checkRules(response.getContentAsString(), 3);
        Filter f1 = checkRule(rules[0], "#690000", org.opengis.filter.And.class);
        assertFilter("foo >= 4 and foo < 32.667", f1);
        Filter f2 = checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        assertFilter("foo >= 32.667 and foo < 61.333", f2);
        Filter f3 = checkRule(rules[2], "#FF0000", org.opengis.filter.And.class);
        assertFilter("foo >= 61.333 and foo <= 90", f3);
    }

    private void assertFilter(String expectedCQL, Filter actual) throws CQLException {
        final Filter expected = ECQL.toFilter(expectedCQL);
        assertEquals(expected, actual);
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
    public void testClassifyQuantileStdDev() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=3&open=false&stddevs=1&fullSLD=true&method=quantile";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);

        // stddev filter cuts 4 and 90 away
        // System.out.println(response.getContentAsString());

        Rule[] rules = checkRules(response.getContentAsString(), 3);
        Filter f1 = checkRule(rules[0], "#690000", org.opengis.filter.And.class);
        assertFilter("foo >= 8 and foo < 20", f1);
        Filter f2 = checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        assertFilter("foo >= 20 and foo < 43", f2);
        Filter f3 = checkRule(rules[2], "#FF0000", org.opengis.filter.And.class);
        assertFilter("foo >= 43 and foo <= 61", f3);
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

        // not enough polygons to make 5 rules, only 4
        assertEquals(" < 43.0", rules[0].getDescription().getTitle().toString());
        assertEquals(" >= 43.0 AND < 61.0", rules[1].getDescription().getTitle().toString());
        assertEquals(" >= 61.0 AND < 90.0", rules[2].getDescription().getTitle().toString());
        assertEquals(" >= 90.0", rules[3].getDescription().getTitle().toString());
    }

    @Test
    public void testEqualAreaStdDevs() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPolygons/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=5&open=true&method=equalArea&stddevs=1";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertEquals(200, response.getStatus());
        String resultXml = response.getContentAsString();
        // System.out.println(resultXml);
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        // not enough polygons to make 5 rules, only 3 (due also to stddev cut)
        assertEquals(" < 29.0", rules[0].getDescription().getTitle().toString());
        assertEquals(" >= 29.0 AND < 61.0", rules[1].getDescription().getTitle().toString());
        assertEquals(" >= 61.0", rules[2].getDescription().getTitle().toString());
    }

    @Test
    public void testEqualAreaWithinBounds() throws Exception {
        // restrict the area used for the classification
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPolygons/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&intervals=5&open=true&method=equalArea&bbox=20,20,150,150";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkRules(
                        resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix), 3);

        // also due to bbox restriction, not enough polygons to make 5 rules, only 3
        assertEquals(" < 43.0", rules[0].getDescription().getTitle().toString());
        assertEquals(" >= 43.0 AND < 90.0", rules[1].getDescription().getTitle().toString());
        assertEquals(" >= 90.0", rules[2].getDescription().getTitle().toString());
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
        checkRule(rules[0], "#690000", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#B40000", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#FF0000", org.opengis.filter.PropertyIsEqualTo.class);
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

        checkRule(rules[0], "#000069", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#0000B4", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#0000FF", org.opengis.filter.PropertyIsEqualTo.class);
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

        checkRule(rules[0], "#0000FF", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#0000B4", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#000069", org.opengis.filter.PropertyIsEqualTo.class);
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

        checkRule(rules[0], "#690000", org.opengis.filter.PropertyIsLessThan.class);
        PropertyIsLessThan filter = (PropertyIsLessThan) rules[0].getFilter();
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

    private Filter checkRule(Rule rule, String color, Class<?> filterType) {
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
        return rule.getFilter();
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
        assertEquals(CQL.toExpression("'#00008E'"), entries[0].getColor());
        assertEquals(CQL.toExpression("1.0"), entries[1].getQuantity());
        assertEquals(CQL.toExpression("'#0000FF'"), entries[1].getColor());
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
        assertEquals(CQL.toExpression("'#00001F'"), entries[0].getColor());
        assertEquals(CQL.toExpression("178.0"), entries[166].getQuantity());
        assertEquals(CQL.toExpression("'#0000FF'"), entries[166].getColor());
    }

    @Test
    public void testRasterUniqueByteStddev() throws Exception {
        // filter the list of values to those within 2 stddevs from average
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:tazbyte/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=uniqueInterval&ramp=blue&fullSLD=true&stddevs=2";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(106, entries.length);
        assertEquals(CQL.toExpression("1.0"), entries[0].getQuantity());
        assertEquals(CQL.toExpression("'#000020'"), entries[0].getColor());
        assertEquals(CQL.toExpression("106.0"), entries[105].getQuantity());
        assertEquals(CQL.toExpression("'#0000FF'"), entries[105].getColor());
    }

    @Test
    public void testEqualIntervalByteStddev() throws Exception {
        // filter the list of values to those within 2 stddevs from average
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:tazbyte/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=equalInterval&ramp=blue&fullSLD=true&stddevs=2&intervals=5";
        Document dom = getAsDOM(restPath, 200);
        // print(dom);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(6, entries.length); // 6 values to define the bounds of 5 intervals
        // values reduced from 1 to 107 due to the stddev filter
        assertEquals(CQL.toExpression("1.0"), entries[0].getQuantity());
        assertEquals(CQL.toExpression("'#000000'"), entries[0].getColor());
        assertEquals(CQL.toExpression("107.00000000000001"), entries[5].getQuantity());
        assertEquals(CQL.toExpression("'#0000FF'"), entries[5].getColor());
    }

    @Test
    public void testQuantileByteStddev() throws Exception {
        // filter the list of values to those within 2 stddevs from average
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:tazbyte/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&ramp=blue&fullSLD=true&stddevs=2&intervals=5";
        Document dom = getAsDOM(restPath, 200);
        // print(dom);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(6, entries.length); // 6 values to define the bounds of 5 intervals
        // values reduced from 1 to 107 due to the stddev filter
        assertEquals(CQL.toExpression("1.0"), entries[0].getQuantity());
        assertEquals(CQL.toExpression("'#000000'"), entries[0].getColor());
        assertEquals(CQL.toExpression("107.00000000000001"), entries[5].getQuantity());
        assertEquals(CQL.toExpression("'#0000FF'"), entries[5].getColor());
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
    public void testEqualIntervalDemBBOX() throws Exception {
        // get a smaller subset, this should alter min and max accordingly
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:dem/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=equalInterval&intervals=5&ramp=jet&fullSLD=true&bbox=10,10,15,15";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(6, entries.length);
        assertEntry(entries[0], 392, null, "#000000", 0); // transparent entry
        assertEntry(entries[1], 404.6, ">= 392 AND < 404.6", "#0000FF", 1);
        assertEntry(entries[2], 417.2, ">= 404.6 AND < 417.2", "#FFFF00", 1);
        assertEntry(entries[3], 429.8, ">= 417.2 AND < 429.8", "#FFAA00", 1);
        assertEntry(entries[4], 442.4, ">= 429.8 AND < 442.4", "#FF5500", 1);
        assertEntry(entries[5], 455, ">= 442.4 AND <= 455", "#FF0000", 1);
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
        // the expected values are from the pixel perfect quantile classification,
        // the tolerance is added to allow the histogram based classification to
        // pass the test, while ensuring it's not too far away
        assertEntry(entries[0], -2, null, "#000000", 0); // transparent entry
        assertEntry(entries[1], 237, ">= -2 AND < 243.820312", "#0000FF", 1, 10);
        assertEntry(entries[2], 441, ">= 243.820312 AND < 447.5", "#FFFF00", 1, 10);
        assertEntry(entries[3], 640, ">= 447.5 AND < 644.15625", "#FFAA00", 1, 10);
        assertEntry(entries[4], 894, ">= 644.15625 AND < 897", "#FF5500", 1, 10);
        assertEntry(entries[5], 1796, ">= 897 AND <= 1796", "#FF0000", 1);
    }

    @Test
    public void testQuantileOpenIntervalsSrtm() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5&ramp=jet&fullSLD=true&open=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        // the expected values are from the pixel perfect quantile classification,
        // the tolerance is added to allow the histogram based classification to
        // pass the test, while ensuring it's not too far away
        assertEntry(entries[0], 237, "< 243.820312", "#0000FF", 1, 10);
        assertEntry(entries[1], 441, ">= 243.820312 AND < 447.5", "#FFFF00", 1, 10);
        assertEntry(entries[2], 640, ">= 447.5 AND < 644.15625", "#FFAA00", 1, 10);
        assertEntry(entries[3], 894, ">= 644.15625 AND < 897", "#FF5500", 1, 10);
        assertEntry(entries[4], Double.MAX_VALUE, ">= 897", "#FF0000", 1);
    }

    @Test
    public void testQuantileStdDevOpenIntervalsSrtm() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5&ramp=jet&fullSLD=true&open=true&stddevs=1";
        Document dom = getAsDOM(restPath, 200);
        // print(dom);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        // same as in
        assertEntry(entries[0], 360, "< 360.640794", "#0000FF", 1, 10);
        assertEntry(entries[1], 481, ">= 360.640794 AND < 481.343203", "#FFFF00", 1, 10);
        assertEntry(entries[2], 610, ">= 481.343203 AND < 610.275321", "#FFAA00", 1, 10);
        assertEntry(entries[3], 756, ">= 610.275321 AND < 755.666859", "#FF5500", 1, 10);
        assertEntry(entries[4], Double.MAX_VALUE, ">= 755.666859", "#FF0000", 1);
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
        // the expected values are from the pixel perfect quantile classification,
        // the tolerance is added to allow the histogram based classification to
        // pass the test, while ensuring it's not too far away
        assertEntry(entries[0], -2, "-2", "#0000FF", 1);
        assertEntry(entries[1], 292, "292.984375", "#FFFF00", 1, 10);
        assertEntry(entries[2], 536, "538.804688", "#FFAA00", 1, 10);
        assertEntry(entries[3], 825, "826.765625", "#FF5500", 1, 10);
        assertEntry(entries[4], 1796, "1796", "#FF0000", 1);
    }

    /**
     * Same as testQuantileContinuousSrtm, but with reversed colormap
     *
     * @throws Exception
     */
    @Test
    public void testQuantileContinuousSrtmReverse() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5&ramp=jet&fullSLD=true&continuous=true&reverse=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertEntry(entries[0], -2, "-2", "#FF0000", 1);
        assertEntry(entries[1], 292, "292.984375", "#FF5500", 1, 10);
        assertEntry(entries[2], 536, "538.804688", "#FFAA00", 1, 10);
        assertEntry(entries[3], 825, "826.765625", "#FFFF00", 1, 10);
        assertEntry(entries[4], 1796, "1796", "#0000FF", 1);
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
        // the expected values are from the pixel perfect jenks classification,
        // the tolerance is added to allow the histogram based classification to
        // pass the test, while ensuring it's not too far away
        assertEntry(entries[0], -2, "-2", "#0000FF", 1);
        assertEntry(entries[1], 336, "332.011905", "#FFFF00", 1, 10);
        assertEntry(entries[2], 660, "654.707317", "#FFAA00", 1, 10);
        assertEntry(entries[3], 1011, "1005.6", "#FF5500", 1, 10);
        assertEntry(entries[4], 1796, "1796", "#FF0000", 1);
    }

    @Test
    public void testJenksIntervalsSrtmStddev() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=jenks&intervals=5&ramp=jet&fullSLD=true&continuous=true&stddevs=1";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        // same as above, but the stddev filtering limits the range
        assertEntry(entries[0], 223, "223.478966", "#0000FF", 1, 1);
        assertEntry(entries[1], 394, "394.911765", "#FFFF00", 1, 1);
        assertEntry(entries[2], 561, "561.92", "#FFAA00", 1, 1);
        assertEntry(entries[3], 738, "738.037037", "#FF5500", 1, 1);
        assertEntry(entries[4], 926, "925.747526", "#FF0000", 1, 1);
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

    @Test
    public void testCoverageViewDefaultBand() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/wcs:multiband_select/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5&ramp=jet&fullSLD=true&continuous=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ChannelSelection channelSelection = rs.getChannelSelection();
        assertNotNull(channelSelection);
        SelectedChannelType gray = channelSelection.getGrayChannel();
        assertNotNull(gray);
        assertEquals("1", gray.getChannelName().evaluate(null, String.class));
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertEntry(entries[0], 0, "0", "#0000FF", 1);
        assertEntry(entries[1], 6, "6", "#FFFF00", 1);
        assertEntry(entries[2], 51, "51", "#FFAA00", 1);
        assertEntry(entries[3], 93, "93", "#FF5500", 1);
        assertEntry(entries[4], 194, "194", "#FF0000", 1);
    }

    @Test
    public void testMultibandSelection() throws Exception {
        // same as the above, but going against the native TIFF file. Need to read band 3 to
        // match the first band of the coverage view
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/wcs:multiband/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5&ramp=jet&fullSLD=true&continuous=true&attribute=3";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ChannelSelection channelSelection = rs.getChannelSelection();
        assertNotNull(channelSelection);
        SelectedChannelType gray = channelSelection.getGrayChannel();
        assertNotNull(gray);
        assertEquals("3", gray.getChannelName().evaluate(null, String.class));
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertEntry(entries[0], 0, "0", "#0000FF", 1);
        assertEntry(entries[1], 6, "6", "#FFFF00", 1);
        assertEntry(entries[2], 51, "51", "#FFAA00", 1);
        assertEntry(entries[3], 93, "93", "#FF5500", 1);
        assertEntry(entries[4], 194, "194", "#FF0000", 1);
    }

    @Test
    public void testCoverageViewSecondBand() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/wcs:multiband_select/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5&ramp=jet&fullSLD=true&continuous=true&attribute=2";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ChannelSelection channelSelection = rs.getChannelSelection();
        assertNotNull(channelSelection);
        SelectedChannelType gray = channelSelection.getGrayChannel();
        assertNotNull(gray);
        assertEquals("2", gray.getChannelName().evaluate(null, String.class));
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertEntry(entries[0], 0, "0", "#0000FF", 1);
        assertEntry(entries[1], 6, "6", "#FFFF00", 1);
        assertEntry(entries[2], 48, "48", "#FFAA00", 1);
        assertEntry(entries[3], 77, "77", "#FF5500", 1);
        assertEntry(entries[4], 160, "160", "#FF0000", 1);
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

    private void assertEntry(
            ColorMapEntry entry,
            double value,
            String label,
            String color,
            double opacity,
            double valueTolerance) {
        assertEquals(value, entry.getQuantity().evaluate(null, Double.class), valueTolerance);
        assertEquals(label, entry.getLabel());
        assertEquals(color, entry.getColor().evaluate(null, String.class));
        double actualOpacity =
                Optional.ofNullable(entry.getOpacity())
                        .map(o -> o.evaluate(null, Double.class))
                        .orElse((double) 1);
        assertEquals(opacity, actualOpacity, EPS);
    }

    @Test
    public void testReaderBandSelection() throws Exception {
        // the backing reader supports native selection
        CoverageInfo coverage = getCatalog().getCoverageByName(MULTIBAND_VIEW);
        ImageReader reader = new ImageReader(coverage, 1, DEFAULT_MAX_PIXELS, null).invoke();

        Map<GeneralParameterDescriptor, Object> parameters =
                getParametersMap(reader.getReadParameters());

        // expect the bands selection and deferred loading
        assertThat(
                parameters.keySet(),
                Matchers.containsInAnyOrder(
                        AbstractGridFormat.BANDS, AbstractGridFormat.USE_JAI_IMAGEREAD));
        int[] bands = (int[]) parameters.get(AbstractGridFormat.BANDS);
        assertArrayEquals(new int[] {0}, bands);

        RenderedImage image = reader.getImage();
        assertEquals(1, image.getSampleModel().getNumBands());
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        }
    }

    @Test
    public void testJAIBandSelection() throws Exception {
        // the backing reader does not support native selection
        CoverageInfo coverage =
                getCatalog().getCoverageByName(SystemTestData.MULTIBAND.getLocalPart());
        ImageReader reader = new ImageReader(coverage, 1, DEFAULT_MAX_PIXELS, null).invoke();

        Map<GeneralParameterDescriptor, Object> parameters =
                getParametersMap(reader.getReadParameters());

        // expect only deferred loading
        assertThat(parameters.keySet(), Matchers.contains(AbstractGridFormat.USE_JAI_IMAGEREAD));

        // yet the image just has one band
        RenderedImage image = reader.getImage();
        assertEquals(1, image.getSampleModel().getNumBands());
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        }
    }

    @Test
    public void testSubsampling() throws Exception {
        CoverageInfo coverage =
                getCatalog().getCoverageByName(SystemTestData.MULTIBAND.getLocalPart());
        // the image is 68*56=3808, force subsampling by giving a low limit
        ImageReader reader = new ImageReader(coverage, 1, 1000, null).invoke();

        Map<GeneralParameterDescriptor, Object> parameters =
                getParametersMap(reader.getReadParameters());

        // expect deferred loading and restricted grid geometry
        assertThat(
                parameters.keySet(),
                Matchers.containsInAnyOrder(
                        AbstractGridFormat.USE_JAI_IMAGEREAD,
                        AbstractGridFormat.READ_GRIDGEOMETRY2D));
        // reduced pixels
        GridGeometry2D gg = (GridGeometry2D) parameters.get(AbstractGridFormat.READ_GRIDGEOMETRY2D);
        assertEquals(35, gg.getGridRange2D().width);
        assertEquals(29, gg.getGridRange2D().height);
        // but full envelope
        assertEquals(
                coverage.getNativeBoundingBox(), ReferencedEnvelope.reference(gg.getEnvelope2D()));

        // the image just has one band
        RenderedImage image = reader.getImage();
        assertEquals(1, image.getSampleModel().getNumBands());
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        }
    }

    @Test
    public void testBoundingBox() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(SystemTestData.MULTIBAND.getLocalPart());
        ReferencedEnvelope readEnvelope =
                new ReferencedEnvelope(
                        520000, 540000, 3600000, 3700000, CRS.decode("EPSG:32611", true));
        ImageReader reader = new ImageReader(ci, 1, DEFAULT_MAX_PIXELS, readEnvelope).invoke();

        // expect deferred loading and restricted grid geometry
        Map<GeneralParameterDescriptor, Object> parameters =
                getParametersMap(reader.getReadParameters());
        assertThat(
                parameters.keySet(),
                Matchers.containsInAnyOrder(
                        AbstractGridFormat.USE_JAI_IMAGEREAD,
                        AbstractGridFormat.READ_GRIDGEOMETRY2D));
        GridGeometry2D gg = (GridGeometry2D) parameters.get(AbstractGridFormat.READ_GRIDGEOMETRY2D);
        // check the grid geometry is restricted in space, but has the same scale factors as the
        // original one (from a gdalinfo output)
        AffineTransform2D at = (AffineTransform2D) gg.getGridToCRS2D();
        double xPixelSize = 3530;
        double yPixelSize = 3547;
        assertEquals(at.getScaleX(), xPixelSize, 1);
        assertEquals(at.getScaleY(), -yPixelSize, 1);
        // read bounds are the requested ones, allow up to a pixel worth of difference
        assertBoundsEquals2D(readEnvelope, gg.getEnvelope2D(), Math.max(xPixelSize, yPixelSize));

        // if the reader does not do cropping, make sure it's done in post processing if needed
        GridCoverage2D coverage = reader.getCoverage();
        assertBoundsEquals2D(
                coverage.getEnvelope2D(), readEnvelope, Math.max(xPixelSize, yPixelSize));

        // the image just has one band
        RenderedImage image = reader.getImage();
        assertEquals(1, image.getSampleModel().getNumBands());
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        }
    }

    @Test
    public void testBoundingBoxPartiallyOutside() throws Exception {
        CoverageInfo coverage =
                getCatalog().getCoverageByName(SystemTestData.MULTIBAND.getLocalPart());
        ReferencedEnvelope readEnvelope =
                new ReferencedEnvelope(
                        500000, 540000, 3000000, 3600000, CRS.decode("EPSG:32611", true));
        ImageReader reader =
                new ImageReader(coverage, 1, DEFAULT_MAX_PIXELS, readEnvelope).invoke();

        // expect deferred loading and restricted grid geometry
        Map<GeneralParameterDescriptor, Object> parameters =
                getParametersMap(reader.getReadParameters());
        assertThat(
                parameters.keySet(),
                Matchers.containsInAnyOrder(
                        AbstractGridFormat.USE_JAI_IMAGEREAD,
                        AbstractGridFormat.READ_GRIDGEOMETRY2D));
        GridGeometry2D gg = (GridGeometry2D) parameters.get(AbstractGridFormat.READ_GRIDGEOMETRY2D);
        // check the grid geometry is restricted in space, but has the same scale factors as the
        // original one (from a gdalinfo output)
        AffineTransform2D at = (AffineTransform2D) gg.getGridToCRS2D();
        double xPixelSize = 3530;
        double yPixelSize = 3547;
        assertEquals(at.getScaleX(), xPixelSize, 1);
        assertEquals(at.getScaleY(), -yPixelSize, 1);
        // read bounds are the requested ones intersected with the coverage envelope, allow up to a
        // pixel worth of difference
        ReferencedEnvelope expectedEnvelope =
                readEnvelope.intersection(coverage.getNativeBoundingBox());
        assertBoundsEquals2D(
                expectedEnvelope, gg.getEnvelope2D(), Math.max(xPixelSize, yPixelSize));

        // the image just has one band
        RenderedImage image = reader.getImage();
        assertEquals(1, image.getSampleModel().getNumBands());
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        }
    }

    @Test
    public void testBoundingBoxAndRescale() throws Exception {
        CoverageInfo coverage =
                getCatalog().getCoverageByName(SystemTestData.MULTIBAND.getLocalPart());
        ReferencedEnvelope readEnvelope =
                new ReferencedEnvelope(
                        520000, 748000, 3600000, 3700000, CRS.decode("EPSG:32611", true));
        ImageReader reader = new ImageReader(coverage, 1, 1000, readEnvelope).invoke();

        // expect deferred loading and restricted grid geometry
        Map<GeneralParameterDescriptor, Object> parameters =
                getParametersMap(reader.getReadParameters());
        assertThat(
                parameters.keySet(),
                Matchers.containsInAnyOrder(
                        AbstractGridFormat.USE_JAI_IMAGEREAD,
                        AbstractGridFormat.READ_GRIDGEOMETRY2D));
        GridGeometry2D gg = (GridGeometry2D) parameters.get(AbstractGridFormat.READ_GRIDGEOMETRY2D);
        // check the grid geometry is restricted in space and also scaled down to match the max
        // pixels
        AffineTransform2D at = (AffineTransform2D) gg.getGridToCRS2D();
        double xPixelSize = 4882;
        double yPixelSize = 4898;
        assertEquals(xPixelSize, at.getScaleX(), 1);
        assertEquals(-yPixelSize, at.getScaleY(), 1);
        // read bounds are the requested ones, allow up to a pixel worth of difference
        assertBoundsEquals2D(readEnvelope, gg.getEnvelope2D(), Math.max(xPixelSize, yPixelSize));

        // the image just has one band
        RenderedImage image = reader.getImage();
        assertEquals(1, image.getSampleModel().getNumBands());
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        }
    }

    private Map<GeneralParameterDescriptor, Object> getParametersMap(
            ArrayList<GeneralParameterValue> readParameters) {
        return readParameters
                .stream()
                .collect(
                        Collectors.toMap(
                                pv -> pv.getDescriptor(), pv -> ((ParameterValue) pv).getValue()));
    }

    private void assertBoundsEquals2D(Envelope env1, Envelope env2, double eps) {
        double[] delta = new double[4];
        delta[0] = env1.getMinimum(0) - env2.getMinimum(0);
        delta[1] = env1.getMaximum(0) - env2.getMaximum(0);
        delta[2] = env1.getMinimum(1) - env2.getMinimum(1);
        delta[3] = env1.getMaximum(1) - env2.getMaximum(1);

        for (int i = 0; i < delta.length; i++) {
            /*
             * As per Envelope2D#boundsEquals we use ! here to
             * catch any NaN values
             */
            if (!(Math.abs(delta[i]) <= eps)) {
                fail("Envelopes have not same 2D bounds: " + env1 + ", " + env2);
            }
        }
    }
}
