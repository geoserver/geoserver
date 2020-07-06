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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.rest.RestBaseController;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.EnvFunction;
import org.geotools.filter.function.FilterFunction_parseDouble;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.styling.*;
import org.geotools.xml.styling.SLDParser;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.*;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
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

    static final QName CLASSIFICATION_POINTS2 =
            new QName(SystemTestData.CITE_URI, "ClassificationPoints2", SystemTestData.CITE_PREFIX);

    static final QName CLASSIFICATION_POINTS3 =
            new QName(SystemTestData.CITE_URI, "ClassificationPoints3", SystemTestData.CITE_PREFIX);

    static final QName CLASSIFICATION_POLYGONS =
            new QName(
                    SystemTestData.CITE_URI, "ClassificationPolygons", SystemTestData.CITE_PREFIX);

    static final QName CLASSIFICATION_LINES =
            new QName(SystemTestData.CITE_URI, "ClassificationLines", SystemTestData.CITE_PREFIX);

    static final QName FILTERED_POINTS =
            new QName(SystemTestData.CITE_URI, "FilteredPoints", SystemTestData.CITE_PREFIX);

    static final QName MILANOGEO =
            new QName(SystemTestData.CITE_URI, "milanogeo", SystemTestData.CITE_PREFIX);

    static final QName TAZBYTE =
            new QName(SystemTestData.CITE_URI, "tazbyte", SystemTestData.CITE_PREFIX);

    static final QName DEM_FLOAT =
            new QName(SystemTestData.CITE_URI, "dem", SystemTestData.CITE_PREFIX);

    static final QName SRTM =
            new QName(SystemTestData.CITE_URI, "srtm", SystemTestData.CITE_PREFIX);

    static final QName SFDEM_MOSAIC =
            new QName(SystemTestData.CITE_URI, "sfdem_mosaic", SystemTestData.CITE_PREFIX);

    static final QName SINGLE_FLOAT =
            new QName(SystemTestData.CITE_URI, "singleFloatNoData", SystemTestData.CITE_PREFIX);

    static final QName SINGLE_BYTE =
            new QName(SystemTestData.CITE_URI, "singleByteNoData", SystemTestData.CITE_PREFIX);

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
                CLASSIFICATION_POINTS2,
                props,
                "ClassificationPoints2.properties",
                this.getClass(),
                catalog);

        testData.addVectorLayer(
                CLASSIFICATION_POINTS3,
                props,
                "ClassificationPoints3.properties",
                this.getClass(),
                catalog);

        testData.addVectorLayer(
                CLASSIFICATION_LINES,
                props,
                "ClassificationLines.properties",
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

        testData.addRasterLayer(
                SINGLE_FLOAT, "singleFloatNoData.tif", "tif", null, this.getClass(), catalog);

        testData.addRasterLayer(
                SINGLE_BYTE, "singleByteNoData.tif", "tif", null, this.getClass(), catalog);

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

        // add a filtered view with env vars for vector env parameter testing
        testData.addVectorLayer(
                FILTERED_POINTS,
                props,
                "ClassificationPoints.properties",
                this.getClass(),
                catalog);
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(FILTERED_POINTS.getLocalPart());
        ft.setCqlFilter("group = env('group', 'Group0')");
        catalog.save(ft);

        // add a filtered mosaic with a "direction" column
        testData.addRasterLayer(
                SFDEM_MOSAIC, "sfdem-tiles.zip", null, null, ClassifierTest.class, catalog);
        CoverageInfo sfdem = catalog.getCoverageByName(getLayerId(SFDEM_MOSAIC));
        sfdem.getParameters().put("Filter", "direction = env('direction','NE')");
        catalog.save(sfdem);
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
        assertFilter("foo >= 8.0 and foo < 25.667", f1);
        Filter f2 = checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        assertFilter("foo >= 25.667 and foo < 43.333", f2);
        Filter f3 = checkRule(rules[2], "#FF0000", org.opengis.filter.And.class);
        assertFilter("foo >= 43.333 and foo <= 61.0", f3);
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
        assertFilter("foo >= 12.0 and foo < 17.6667", f1);
        Filter f2 = checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        assertFilter("foo >= 17.6667 and foo < 23.3333", f2);
        Filter f3 = checkRule(rules[2], "#FF0000", org.opengis.filter.And.class);
        assertFilter("foo >= 23.3333 and foo <= 29.0", f3);
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
        assertFilter("foo >= 4.0 and foo < 32.667", f1);
        Filter f2 = checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        assertFilter("foo >= 32.667 and foo < 61.333", f2);
        Filter f3 = checkRule(rules[2], "#FF0000", org.opengis.filter.And.class);
        assertFilter("foo >= 61.333 and foo <= 90.0", f3);
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

        assertTrue(rules[0].getDescription().getTitle().toString().contains("20.0"));
        assertTrue(rules[1].getDescription().getTitle().toString().contains("20.0"));
        assertTrue(rules[2].getDescription().getTitle().toString().contains("61.0"));
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
        assertFilter("foo >= 8.0 and foo < 20.0", f1);
        Filter f2 = checkRule(rules[1], "#B40000", org.opengis.filter.And.class);
        assertFilter("foo >= 20.0 and foo < 43.0", f2);
        Filter f3 = checkRule(rules[2], "#FF0000", org.opengis.filter.And.class);
        assertFilter("foo >= 43.0 and foo <= 61.0", f3);
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

        assertTrue(rules[0].getDescription().getTitle().toString().contains("12.0"));
        assertTrue(rules[1].getDescription().getTitle().toString().contains("12.0"));
        assertTrue(rules[1].getDescription().getTitle().toString().contains("29.0"));
        assertTrue(rules[2].getDescription().getTitle().toString().contains("29.0"));
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

        assertTrue(rules[0].getDescription().getTitle().toString().contains("32.6"));
        assertTrue(rules[1].getDescription().getTitle().toString().contains("32.6"));
        assertTrue(rules[1].getDescription().getTitle().toString().contains("61.3"));
        assertTrue(rules[2].getDescription().getTitle().toString().contains("61.3"));
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
        checkRule(rules[0], "#690000", PropertyIsEqualTo.class);
        checkRule(rules[1], "#B40000", PropertyIsEqualTo.class);
        checkRule(rules[2], "#FF0000", PropertyIsEqualTo.class);
        TreeSet<String> orderedRules = new TreeSet<String>();
        orderedRules.add(rules[0].getDescription().getTitle().toString());
        orderedRules.add(rules[1].getDescription().getTitle().toString());
        orderedRules.add(rules[2].getDescription().getTitle().toString());
        Iterator iter = orderedRules.iterator();
        assertEquals("bar", iter.next());
        assertEquals("foo", iter.next());
        assertEquals("foobar", iter.next());
    }

    @Test
    public void testEnvVectorGroup2() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:FilteredPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=name&method=uniqueInterval&fullSLD=true&env=group:Group2";
        Document dom = getAsDOM(restPath, 200);
        List<Rule> rules = getRules(dom);
        assertEquals(2, rules.size());
        checkRule(rules.get(0), "#8E0000", PropertyIsEqualTo.class);
        checkRule(rules.get(1), "#FF0000", PropertyIsEqualTo.class);
        List<String> sortedRules =
                rules.stream()
                        .map(r -> r.getDescription().getTitle().toString())
                        .sorted()
                        .collect(Collectors.toList());
        assertThat(sortedRules, contains("bar", "foo"));

        // also make sure the env vars have been cleared, this thread is the same that run the
        // request
        assertNull(CQL.toExpression("env('group')").evaluate(null));
    }

    @Test
    public void testEnvVectorGroup0() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:FilteredPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=name&method=uniqueInterval&fullSLD=true&env=group:Group0";
        Document dom = getAsDOM(restPath, 200);
        List<Rule> rules = getRules(dom);
        assertEquals(2, rules.size());
        checkRule(rules.get(0), "#8E0000", PropertyIsEqualTo.class);
        checkRule(rules.get(1), "#FF0000", PropertyIsEqualTo.class);
        List<String> sortedRules =
                rules.stream()
                        .map(r -> r.getDescription().getTitle().toString())
                        .sorted()
                        .collect(Collectors.toList());
        assertThat(sortedRules, contains("foo", "foobar"));

        // also make sure the env vars have been cleared, this thread is the same that run the
        // request
        assertNull(CQL.toExpression("env('group')").evaluate(null));
    }

    @Test
    public void testEnvVectorNotThere() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:FilteredPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=name&method=uniqueInterval&fullSLD=true&env=group:NotAGroup";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertEquals(404, response.getStatus());
        assertNull(CQL.toExpression("env('group')").evaluate(null));
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

        checkRule(rules[0], "#000069", PropertyIsEqualTo.class);
        checkRule(rules[1], "#0000B4", PropertyIsEqualTo.class);
        checkRule(rules[2], "#0000FF", PropertyIsEqualTo.class);
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

        checkRule(rules[0], "#0000FF", PropertyIsEqualTo.class);
        checkRule(rules[1], "#0000B4", PropertyIsEqualTo.class);
        checkRule(rules[2], "#000069", PropertyIsEqualTo.class);
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

        checkRule(rules[0], "#FF0000", PropertyIsEqualTo.class);
        checkRule(rules[1], "#7F007F", PropertyIsEqualTo.class);
        checkRule(rules[2], "#0000FF", PropertyIsEqualTo.class);
    }

    private Rule[] checkRules(String resultXml, int classes) {
        Rule[] rules = checkSLD(resultXml);
        assertEquals(classes, rules.length);
        return rules;
    }

    private void checkStroke(Rule rule, String color, String weight) {
        assertNotNull(rule.symbolizers());
        assertEquals(1, rule.symbolizers().size());
        assertTrue(rule.symbolizers().get(0) instanceof PointSymbolizer);
        PointSymbolizer symbolizer = (PointSymbolizer) rule.symbolizers().get(0);
        assertNotNull(symbolizer.getGraphic());
        assertEquals(1, symbolizer.getGraphic().graphicalSymbols().size());
        Mark mark = (Mark) symbolizer.getGraphic().graphicalSymbols().get(0);
        assertNotNull(mark.getStroke());
        assertEquals(color, mark.getStroke().getColor().toString());
        assertEquals(weight, mark.getStroke().getWidth().toString());
    }

    private Filter checkRule(Rule rule, String color, Class<?> filterType) {
        assertNotNull(rule.getFilter());
        assertTrue(filterType.isAssignableFrom(rule.getFilter().getClass()));
        assertNotNull(rule.symbolizers());
        assertEquals(1, rule.symbolizers().size());
        assertTrue(rule.symbolizers().get(0) instanceof PointSymbolizer);
        PointSymbolizer symbolizer = (PointSymbolizer) rule.symbolizers().get(0);
        assertNotNull(symbolizer.getGraphic());
        assertEquals(1, symbolizer.getGraphic().graphicalSymbols().size());
        Mark mark = (Mark) symbolizer.getGraphic().graphicalSymbols().get(0);
        assertNotNull(mark.getFill());
        assertEquals(color, mark.getFill().getColor().toString());
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

    /** Same as testQuantileContinuousSrtm, but with reversed colormap */
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

        // expect the bands selection
        assertThat(parameters.keySet(), Matchers.hasItem(AbstractGridFormat.BANDS));
        int[] bands = (int[]) parameters.get(AbstractGridFormat.BANDS);
        assertArrayEquals(new int[] {0}, bands);

        RenderedImage image = reader.getImage();
        assertEquals(1, image.getSampleModel().getNumBands());
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        }
    }

    @Test
    public void testDeferredLoadMosaic() throws Exception {
        // the backing reader supports deferred loading
        CoverageInfo coverage = getCatalog().getCoverageByName(getLayerId(SFDEM_MOSAIC));
        ImageReader reader = new ImageReader(coverage, 1, DEFAULT_MAX_PIXELS, null).invoke();

        Map<GeneralParameterDescriptor, Object> parameters =
                getParametersMap(reader.getReadParameters());

        // expect the bands selection
        assertThat(parameters.keySet(), Matchers.hasItem(AbstractGridFormat.USE_JAI_IMAGEREAD));
        Boolean imageRead = (Boolean) parameters.get(AbstractGridFormat.USE_JAI_IMAGEREAD);
        assertTrue(imageRead);

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
        assertThat(parameters.keySet(), not(contains(AbstractGridFormat.BANDS)));

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
        assertThat(parameters.keySet(), Matchers.hasItem(AbstractGridFormat.READ_GRIDGEOMETRY2D));
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
        assertThat(parameters.keySet(), Matchers.hasItem(AbstractGridFormat.READ_GRIDGEOMETRY2D));
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
        assertThat(parameters.keySet(), Matchers.hasItem(AbstractGridFormat.READ_GRIDGEOMETRY2D));
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
        assertThat(parameters.keySet(), Matchers.hasItem(AbstractGridFormat.READ_GRIDGEOMETRY2D));
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

    @Test
    public void testRasterEnv() throws Exception {
        // checking the extrema of the raster in each granule, stats coming from QGIS
        checkRasterEnv("NW", 1080, 1767);
        checkRasterEnv("SW", 1379, 1840);
        checkRasterEnv("NE", 1066, 1626);
        checkRasterEnv("SE", 1214, 1735);
    }

    private void checkRasterEnv(String direction, double low, double high) throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:sfdem_mosaic/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=equalInterval&intervals=1&ramp=jet&fullSLD=true&env=direction:"
                        + direction;
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(2, entries.length);
        assertEquals(low, entries[0].getQuantity().evaluate(null, Double.class), 0.1);
        assertEquals(high, entries[1].getQuantity().evaluate(null, Double.class), 0.1);
    }

    @Test
    public void testRasterEnvNotFound() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:sfdem_mosaic/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=equalInterval&intervals=1&ramp=jet&fullSLD=true&env=direction:IDontExist";
        MockHttpServletResponse servletResponse = getAsServletResponse(restPath);
        assertEquals(404, servletResponse.getStatus());
    }

    private Map<GeneralParameterDescriptor, Object> getParametersMap(
            List<GeneralParameterValue> readParameters) {
        return readParameters
                .stream()
                .filter(pv -> ((ParameterValue) pv).getValue() != null)
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

    @Test
    public void testImageReaderEnv() throws Exception {
        // the backing reader supports native selection
        CoverageInfo coverage = getCatalog().getCoverageByName(getLayerId(SFDEM_MOSAIC));
        try {
            EnvFunction.setLocalValue("direction", "NE");
            ImageReader reader = new ImageReader(coverage, 1, DEFAULT_MAX_PIXELS, null).invoke();

            List<GeneralParameterValue> readParameters = reader.getReadParameters();
            Map<GeneralParameterDescriptor, Object> parameterValues =
                    getParametersMap(readParameters);

            // check no duplicates
            Set<String> parameterCodes =
                    readParameters
                            .stream()
                            .map(rp -> rp.getDescriptor().getName().getCode())
                            .collect(Collectors.toSet());
            assertEquals(readParameters.size(), parameterCodes.size());

            // the filter has been set with env vars expanded
            Filter filter = (Filter) parameterValues.get(ImageMosaicFormat.FILTER);
            assertEquals(ECQL.toFilter("direction = 'NE'"), filter);
        } finally {
            EnvFunction.clearLocalValues();
        }
    }

    @Test
    public void testClassifyRasterSingleFloat() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:singleFloatNoData/"
                        + getServiceUrl()
                        + ".xml?continuous=false&fullSLD=true&method=quantile"
                        + "&colors=0xFF071C,0xFFA92E&ramp=custom";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        assertEquals(ColorMap.TYPE_INTERVALS, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(2, entries.length);
        ColorMapEntry cm0 = cm.getColorMapEntry(0);
        assertThat(cm0.getQuantity().evaluate(null, Float.class), Matchers.lessThanOrEqualTo(10f));
        assertEquals("#FF071C", cm0.getColor().evaluate(null, String.class));
        assertEquals(1, cm0.getOpacity().evaluate(null, Double.class), 0);
        ColorMapEntry cm1 = cm.getColorMapEntry(1);
        assertThat(
                cm1.getQuantity().evaluate(null, Float.class), Matchers.greaterThanOrEqualTo(10f));
        assertEquals("#FF071C", cm1.getColor().evaluate(null, String.class));
        assertEquals(1, cm1.getOpacity().evaluate(null, Double.class), 0);
    }

    /**
     * Was hoping for a simpler solution for integer data (single entry, type "values"), but the
     * output does not really render when tested, unsure why... keeping the code to cover both types
     * of data anyways
     */
    @Test
    public void testClassifyRasterSingleByte() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:singleByteNoData/"
                        + getServiceUrl()
                        + ".xml?continuous=false&fullSLD=true&method=quantile"
                        + "&colors=0xFF071C,0xFFA92E&ramp=custom";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        assertEquals(ColorMap.TYPE_INTERVALS, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(2, entries.length);
        ColorMapEntry cm0 = cm.getColorMapEntry(0);
        assertThat(cm0.getQuantity().evaluate(null, Float.class), Matchers.lessThanOrEqualTo(10f));
        assertEquals("#FF071C", cm0.getColor().evaluate(null, String.class));
        assertEquals(1, cm0.getOpacity().evaluate(null, Double.class), 0);
        ColorMapEntry cm1 = cm.getColorMapEntry(1);
        assertThat(
                cm1.getQuantity().evaluate(null, Float.class), Matchers.greaterThanOrEqualTo(10f));
        assertEquals("#FF071C", cm1.getColor().evaluate(null, String.class));
        assertEquals(1, cm1.getOpacity().evaluate(null, Double.class), 0);
    }

    @Test
    public void testRasterNoDuplicatedClasses() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:milanogeo/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&ramp=custom&intervals=7"
                        + "&colors=#FF071C,#CC0616,#82040E,#68030B,#530209,#420207,#350206&fullSLD=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(2, entries.length);
        // first color map entry got skipped when applying color ramp, taking the second
        ColorMapEntry cm1 = cm.getColorMapEntry(1);
        assertEquals("#FF071C", cm1.getColor().evaluate(null, String.class));
        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:milanogeo/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=jenks&ramp=custom&intervals=7&open=true"
                        + "&colors=#FF071C,#CC0616,#82040E,#68030B,#530209,#420207,#350206&fullSLD=true";
        Document domJenks = getAsDOM(restPathJenks, 200);
        RasterSymbolizer rsJenks = getRasterSymbolizer(domJenks);
        ColorMap cmJenks = rsJenks.getColorMap();
        ColorMapEntry[] entriesJenks = cmJenks.getColorMapEntries();
        assertEquals(2, entriesJenks.length);
        ColorMapEntry cm0Jenks = cmJenks.getColorMapEntry(0);
        assertEquals("#FF071C", cm0Jenks.getColor().evaluate(null, String.class));
        ColorMapEntry cm1Jenks = cmJenks.getColorMapEntry(1);
        assertEquals("#CC0616", cm1Jenks.getColor().evaluate(null, String.class));
    }

    @Test
    public void testNoDuplicatedClosedRulesVectors() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationLines/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=cat2&ramp=CUSTOM&method=quantile&intervals=7&"
                        + "colors=#FF071C,#CC0616,#82040E,#68030B,#530209,#420207,#350206";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 4);
        checkRuleLineSymbolizer(rules[0], "#FF071C");
        checkRuleLineSymbolizer(rules[1], "#CC0616");
        checkRuleLineSymbolizer(rules[2], "#82040E");
        checkRuleLineSymbolizer(rules[3], "#68030B");

        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationLines/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=cat2&ramp=CUSTOM&method=jenks&intervals=7&"
                        + "colors=#FF071C,#CC0616,#82040E,#68030B,#530209,#420207,#350206";
        Document domJenks = getAsDOM(restPathJenks, 200);
        print(domJenks);
        ByteArrayOutputStream baosJenks = new ByteArrayOutputStream();
        print(domJenks, baosJenks);
        String resultJenks = baosJenks.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesJenks =
                checkSLD(resultJenks.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rulesJenks.length == 3);
        checkRuleLineSymbolizer(rulesJenks[0], "#FF071C");
        checkRuleLineSymbolizer(rulesJenks[1], "#CC0616");
        checkRuleLineSymbolizer(rulesJenks[2], "#82040E");
    }

    @Test
    public void testNoDuplicatedOpenRulesVectors() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationLines/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=cat2&ramp=CUSTOM&method=quantile&intervals=7&open=true&"
                        + "colors=#FF071C,#CC0616,#82040E,#68030B,#530209,#420207,#350206";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 5);
        checkRuleLineSymbolizer(rules[0], "#FF071C");
        checkRuleLineSymbolizer(rules[1], "#CC0616");
        checkRuleLineSymbolizer(rules[2], "#82040E");
        checkRuleLineSymbolizer(rules[3], "#68030B");
        checkRuleLineSymbolizer(rules[4], "#530209");
        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationLines/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=cat2&ramp=CUSTOM&method=jenks&intervals=7&open=true&"
                        + "colors=#FF071C,#CC0616,#82040E,#68030B,#530209,#420207,#350206";
        Document domJenks = getAsDOM(restPathJenks, 200);
        print(domJenks);
        ByteArrayOutputStream baosJenks = new ByteArrayOutputStream();
        print(domJenks, baosJenks);
        String resultJenks = baosJenks.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesJenks =
                checkSLD(resultJenks.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rulesJenks.length == 3);
        checkRuleLineSymbolizer(rulesJenks[0], "#FF071C");
        checkRuleLineSymbolizer(rulesJenks[1], "#CC0616");
        checkRuleLineSymbolizer(rulesJenks[2], "#82040E");
    }

    @Test
    public void testNoDuplicatedExplicitRulesVectors() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationLines/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=cat2&ramp=CUSTOM&method=uniqueInterval&intervals=7&"
                        + "colors=#FF071C,#CC0616,#82040E,#68030B,#530209,#420207,#350206";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 4);
        checkRuleLineSymbolizer(rules[0], "#FF071C");
        checkRuleLineSymbolizer(rules[1], "#CC0616");
        checkRuleLineSymbolizer(rules[2], "#82040E");
        checkRuleLineSymbolizer(rules[3], "#68030B");
    }

    private void checkRuleLineSymbolizer(Rule rule, String color) {
        assertNotNull(rule.symbolizers());
        assertEquals(1, rule.symbolizers().size());
        assertThat(rule.symbolizers().get(0), instanceOf(LineSymbolizer.class));
        LineSymbolizer symbolizer = (LineSymbolizer) rule.symbolizers().get(0);
        assertNotNull(symbolizer.getStroke());
        assertEquals(color, symbolizer.getStroke().getColor().toString());
    }

    @Test
    public void testOpenIntervalFirstRuleConsistency() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints2/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=quantile&intervals=2&open=true";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 2);
        assertTrue(rules[0].getFilter() instanceof PropertyIsEqualTo);
        assertTrue(rules[1].getFilter() instanceof PropertyIsGreaterThan);
        checkNotOverlappingRules(rules[0], rules[1]);
        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints2/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=jenks&intervals=2&open=true";
        Document domJenks = getAsDOM(restPathJenks, 200);
        print(domJenks);
        ByteArrayOutputStream baosJenks = new ByteArrayOutputStream();
        print(domJenks, baosJenks);
        String resultXmlJenks = baosJenks.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesJenks =
                checkSLD(
                        resultXmlJenks
                                .replace("<Rules>", sldPrefix)
                                .replace("</Rules>", sldPostfix));
        assertTrue(rulesJenks.length == 2);
        assertTrue(rulesJenks[0].getFilter() instanceof PropertyIsEqualTo);
        assertTrue(rulesJenks[1].getFilter() instanceof PropertyIsGreaterThan);
        checkNotOverlappingRules(rulesJenks[0], rulesJenks[1]);
    }

    @Test
    public void testNotOverlappingRulesClosed() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints2/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=quantile&intervals=2";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 2);
        Rule first = rules[0];
        Rule second = rules[1];
        checkNotOverlappingRules(first, second);
        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints2/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=jenks&intervals=2&";
        Document domJenks = getAsDOM(restPathJenks, 200);
        print(domJenks);
        ByteArrayOutputStream baosJenks = new ByteArrayOutputStream();
        print(domJenks, baosJenks);
        String resultXmlJenks = baosJenks.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesJenks =
                checkSLD(
                        resultXmlJenks
                                .replace("<Rules>", sldPrefix)
                                .replace("</Rules>", sldPostfix));
        assertTrue(rulesJenks.length == 2);
        Rule firstJenks = rulesJenks[0];
        Rule secondJenks = rulesJenks[1];
        checkNotOverlappingRules(firstJenks, secondJenks);
    }

    @Test
    public void testNotOverlappingRulesOpen() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints2/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=quantile&intervals=3&open=true";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 3);
        Rule first = rules[0];
        Rule second = rules[1];
        checkNotOverlappingRules(first, second);
        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints2/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=jenks&intervals=3&open=true";
        Document domJenks = getAsDOM(restPathJenks, 200);
        print(domJenks);
        ByteArrayOutputStream baosJenks = new ByteArrayOutputStream();
        print(domJenks, baosJenks);
        String resultXmlJenks = baosJenks.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesJenks =
                checkSLD(
                        resultXmlJenks
                                .replace("<Rules>", sldPrefix)
                                .replace("</Rules>", sldPostfix));
        assertTrue(rulesJenks.length == 2);
        Rule firstJenks = rulesJenks[0];
        Rule secondJenks = rulesJenks[1];
        checkNotOverlappingRules(firstJenks, secondJenks);
    }

    private void checkNotOverlappingRules(Rule first, Rule second) throws IOException {
        SimpleFeatureType ft =
                (SimpleFeatureType)
                        getCatalog().getFeatureTypeByName("ClassificationPoints2").getFeatureType();
        SimpleFeature feature = DataUtilities.createFeature(ft, "=1|2.0|POINT(4 2.5)");
        assertTrue(first.getFilter().evaluate(feature));
        assertFalse(second.getFilter().evaluate(feature));
    }

    @Test
    public void testPercentagesInRulesLabelsVectorsQuantile() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathQuantile =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPolygons/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=quantile"
                        + "&intervals=3&open=true&percentages=true";
        Document domQuantile = getAsDOM(restPathQuantile, 200);
        print(domQuantile);
        ByteArrayOutputStream baosQuantile = new ByteArrayOutputStream();
        print(domQuantile, baosQuantile);
        String resultXml = baosQuantile.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesQuantile =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rulesQuantile.length == 3);
        for (Rule r : rulesQuantile) {
            Matcher rgxMatcher = rgx.matcher(r.getDescription().getTitle());
            assertTrue(rgxMatcher.find());
        }
    }

    @Test
    public void testPercentagesInRulesLabelsVectorsEqualArea() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathArea =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPolygons/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&ramp=red&method=equalArea"
                        + "&intervals=5&percentages=true";
        Document domArea = getAsDOM(restPathArea, 200);
        print(domArea);
        ByteArrayOutputStream baosArea = new ByteArrayOutputStream();
        print(domArea, baosArea);
        String resultArea = baosArea.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesArea =
                checkSLD(resultArea.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        for (Rule r : rulesArea) {
            Matcher rgxMatcher = rgx.matcher(r.getDescription().getTitle());
            assertTrue(rgxMatcher.find());
        }
    }

    @Test
    public void testPercentagesInRulesLabelsVectorsEqualInterval() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathArea =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=equalInterval"
                        + "&intervals=3&percentages=true";
        Document domArea = getAsDOM(restPathArea, 200);
        print(domArea);
        ByteArrayOutputStream baosArea = new ByteArrayOutputStream();
        print(domArea, baosArea);
        String resultArea = baosArea.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesArea =
                checkSLD(resultArea.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        for (Rule r : rulesArea) {
            Matcher rgxMatcher = rgx.matcher(r.getDescription().getTitle());
            assertTrue(rgxMatcher.find());
        }
    }

    @Test
    public void testPercentagesInRulesLabelsVectorsEqualIntervalWithOutlier() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathArea =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints3/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=equalInterval"
                        + "&intervals=3&percentages=true";
        Document domArea = getAsDOM(restPathArea, 200);
        print(domArea);
        ByteArrayOutputStream baosArea = new ByteArrayOutputStream();
        print(domArea, baosArea);
        String resultArea = baosArea.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesArea =
                checkSLD(resultArea.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        for (Rule r : rulesArea) {
            Matcher rgxMatcher = rgx.matcher(r.getDescription().getTitle());
            assertTrue(rgxMatcher.find());
        }
    }

    @Test
    public void testPercentagesInRulesLabelsVectorJenks() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPolygons/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=jenks&intervals=3&open=true"
                        + "&percentages=true";
        Document domJenks = getAsDOM(restPathJenks, 200);
        print(domJenks);
        ByteArrayOutputStream baosJenks = new ByteArrayOutputStream();
        print(domJenks, baosJenks);
        String resultXmlJenks = baosJenks.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesJenks =
                checkSLD(
                        resultXmlJenks
                                .replace("<Rules>", sldPrefix)
                                .replace("</Rules>", sldPostfix));
        assertTrue(rulesJenks.length == 3);
        for (Rule r : rulesJenks) {
            Matcher rgxMatcher = rgx.matcher(r.getDescription().getTitle());
            assertTrue(rgxMatcher.find());
        }
    }

    @Test
    public void testPercentagesInRulesLabelsVectorUnique() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathUnique =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPolygons/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=uniqueInterval&intervals=8&"
                        + "percentages=true";
        Document domUnique = getAsDOM(restPathUnique, 200);
        print(domUnique);
        ByteArrayOutputStream baosUnique = new ByteArrayOutputStream();
        print(domUnique, baosUnique);
        String resultXmlUnique = baosUnique.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesUnique =
                checkSLD(
                        resultXmlUnique
                                .replace("<Rules>", sldPrefix)
                                .replace("</Rules>", sldPostfix));
        assertTrue(rulesUnique.length == 8);
        for (Rule r : rulesUnique) {
            Matcher rgxMatcher = rgx.matcher(r.getDescription().getTitle());
            assertTrue(rgxMatcher.find());
        }
    }

    @Test
    public void testPercentagesInRuleLabelsVectorCustom() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&customClasses=1,30,#FF0000;30,50,#00FF00;50,90,#0000FF"
                        + "&percentages=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 3);
        for (Rule r : rules) {
            Matcher rgxMatcher = rgx.matcher(r.getDescription().getTitle());
            assertTrue(rgxMatcher.find());
        }
    }

    @Test
    public void testPercentagesInRulesLabelsRasterQuantile() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathQuantile =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:dem/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5"
                        + "&ramp=jet&fullSLD=true&percentages=true";
        Document domQuantile = getAsDOM(restPathQuantile, 200);
        RasterSymbolizer rsQuantile = getRasterSymbolizer(domQuantile);
        ColorMap cmQuantile = rsQuantile.getColorMap();
        ColorMapEntry[] entriesQuantile = cmQuantile.getColorMapEntries();
        assertEquals(entriesQuantile.length, 6);
        for (ColorMapEntry e : entriesQuantile) {
            if (e.getLabel() != null) {
                Matcher matcher = rgx.matcher(e.getLabel());
                matcher.find();
            }
        }
    }

    @Test
    public void testPercentagesInRulesLabelsRasterEqual() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);

        final String restPathEqual =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:dem/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=equalInterval&intervals=5"
                        + "&ramp=jet&fullSLD=true&percentages=true";
        ;
        Document domEqual = getAsDOM(restPathEqual, 200);
        RasterSymbolizer rsEqual = getRasterSymbolizer(domEqual);
        ColorMap cmEqual = rsEqual.getColorMap();
        ColorMapEntry[] entriesEqual = cmEqual.getColorMapEntries();
        assertEquals(entriesEqual.length, 6);
        double percentagesSum = 0.0;
        for (ColorMapEntry e : entriesEqual) {
            if (e.getLabel() != null) {
                String label = e.getLabel();
                int i = label.lastIndexOf("(");
                int i2 = label.indexOf("%)");
                percentagesSum += Double.valueOf(label.substring(i + 1, i2));
                Matcher matcher = rgx.matcher(e.getLabel());
                matcher.find();
            }
        }
        assertTrue(100.0 == percentagesSum);
    }

    @Test
    public void testPercentagesInRulesLabelsRasterJenks() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:dem/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=jenks&intervals=5"
                        + "&ramp=red&fullSLD=true&percentages=true";
        Document domjenks = getAsDOM(restPathJenks, 200);
        RasterSymbolizer rsJenks = getRasterSymbolizer(domjenks);
        ColorMap cmJenks = rsJenks.getColorMap();
        ColorMapEntry[] entriesJenks = cmJenks.getColorMapEntries();
        assertEquals(entriesJenks.length, 6);
        for (ColorMapEntry e : entriesJenks) {
            if (e.getLabel() != null) {
                Matcher matcher = rgx.matcher(e.getLabel());
                matcher.find();
            }
        }
    }

    @Test
    public void testPercentagesInRulesLabelsRasterUnique() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathUnique =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:tazbyte/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=uniqueInterval"
                        + "&ramp=jet&fullSLD=true&percentages=true&intervals=167";
        Document domUnique = getAsDOM(restPathUnique, 200);
        RasterSymbolizer rsUnique = getRasterSymbolizer(domUnique);
        ColorMap cmUnique = rsUnique.getColorMap();
        ColorMapEntry[] entriesUnique = cmUnique.getColorMapEntries();
        assertEquals(entriesUnique.length, 167);
        for (ColorMapEntry e : entriesUnique) {
            if (e.getLabel() != null) {
                Matcher matcher = rgx.matcher(e.getLabel());
                matcher.find();
            }
        }
    }

    @Test
    public void testPercentagesInRulesLabelsRasterCustom() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "customClasses=1,8,#FF0000;8,16,#00FF00;16,30,#0000FF&fullSLD=true"
                        + "&percentages=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(4, entries.length);
        double percentagesSum = 0.0;
        for (ColorMapEntry e : entries) {
            if (e.getLabel() != null) {
                String label = e.getLabel();
                int i = label.lastIndexOf("(");
                int i2 = label.indexOf("%)");
                percentagesSum += Double.valueOf(label.substring(i + 1, i2));
                Matcher matcher = rgx.matcher(e.getLabel());
                matcher.find();
            }
        }
        assertTrue(100.0 == percentagesSum);
    }

    @Test
    public void testPercentagesCustomScale() throws Exception {
        String regex = "\\d+(\\.\\d{1,2})%";
        Pattern rgx = Pattern.compile(regex);
        final String restPathQuantile =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:dem/"
                        + getServiceUrl()
                        + ".xml?"
                        + "method=quantile&intervals=5"
                        + "&ramp=jet&fullSLD=true&percentages=true"
                        + "&percentagesScale=2";
        Document domQuantile = getAsDOM(restPathQuantile, 200);
        RasterSymbolizer rsQuantile = getRasterSymbolizer(domQuantile);
        ColorMap cmQuantile = rsQuantile.getColorMap();
        ColorMapEntry[] entriesQuantile = cmQuantile.getColorMapEntries();
        assertEquals(entriesQuantile.length, 6);
        for (ColorMapEntry e : entriesQuantile) {
            if (e.getLabel() != null) {
                Matcher matcher = rgx.matcher(e.getLabel());
                matcher.find();
            }
        }
    }

    @Test
    public void testPercentagesWithOverlappingRules() throws Exception {
        String regex = "\\d+(\\.\\d)%";
        Pattern rgx = Pattern.compile(regex);
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints2/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=quantile&intervals=3&open=true"
                        + "&percentages=true";
        Document dom = getAsDOM(restPath, 200);
        print(dom);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 3);
        double percentagesSum = 0.0;
        for (Rule r : rules) {
            String title = r.getDescription().getTitle().toString();
            int i = title.lastIndexOf("(");
            int i2 = title.indexOf("%)");
            percentagesSum += Double.valueOf(title.substring(i + 1, i2));
            Matcher rgxMatcher = rgx.matcher(title);
            assertTrue(rgxMatcher.find());
        }
        assertTrue(percentagesSum == 100.0);
        final String restPathJenks =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints2/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=bar&ramp=red&method=jenks&intervals=3&open=true"
                        + "&percentages=true";
        Document domJenks = getAsDOM(restPathJenks, 200);
        print(domJenks);
        ByteArrayOutputStream baosJenks = new ByteArrayOutputStream();
        print(domJenks, baosJenks);
        String resultXmlJenks = baosJenks.toString().replace("\r", "").replace("\n", "");
        Rule[] rulesJenks =
                checkSLD(
                        resultXmlJenks
                                .replace("<Rules>", sldPrefix)
                                .replace("</Rules>", sldPostfix));
        assertTrue(rulesJenks.length == 2);
        percentagesSum = 0.0;
        for (Rule r : rulesJenks) {
            String title = r.getDescription().getTitle().toString();
            int i = title.lastIndexOf("(");
            int i2 = title.indexOf("%)");
            percentagesSum += Double.valueOf(title.substring(i + 1, i2));
            Matcher rgxMatcher = rgx.matcher(title);
            assertTrue(rgxMatcher.find());
        }
        assertTrue(percentagesSum == 100.0);
    }

    @Test
    public void testPercentagesInRulesLabelsRasterCustomZeroValues() throws Exception {
        // test custom classes with intervals outside data values
        // to test 0.0% value is put inside labels
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:srtm/"
                        + getServiceUrl()
                        + ".xml?"
                        + "customClasses=100000,800000,#FF0000;800000,1600000,#00FF00&fullSLD=true"
                        + "&percentages=true";
        Document dom = getAsDOM(restPath, 200);
        RasterSymbolizer rs = getRasterSymbolizer(dom);
        ColorMap cm = rs.getColorMap();
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(3, entries.length);
        for (ColorMapEntry e : entries) {
            if (e.getLabel() != null) {
                String label = e.getLabel();
                int i = label.lastIndexOf("(");
                int i2 = label.indexOf("%)");
                assertEquals(0d, Double.valueOf(label.substring(i + 1, i2)), 0d);
            }
        }
    }

    @Test
    public void testPercentagesInRuleLabelsVectorCustomZeroValues() throws Exception {
        // test custom classes with intervals outside data values
        // to test 0.0% value is put inside labels
        final String restPath =
                RestBaseController.ROOT_PATH
                        + "/sldservice/cite:ClassificationPoints/"
                        + getServiceUrl()
                        + ".xml?"
                        + "attribute=foo&customClasses=10000,30000,#FF0000;30000,50000,#00FF00"
                        + "&percentages=true";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertTrue(response.getStatus() == 200);
        Document dom = getAsDOM(restPath, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(dom, baos);
        String resultXml = baos.toString().replace("\r", "").replace("\n", "");
        Rule[] rules =
                checkSLD(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix));
        assertTrue(rules.length == 2);
        for (Rule r : rules) {
            r.getDescription().getTitle().toString().contains("(0.0%)");
        }
    }
}
