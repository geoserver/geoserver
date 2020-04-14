/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.geoserver.rest.catalog.CoverageStoreFileUploadTest.testBBoxLayerConfiguration;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.util.IOUtils;
import org.geotools.util.URLs;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class StructuredCoverageStoresTest extends CatalogRESTTestSupport {

    private static final String WATER_VIEW = "waterView";
    protected static QName WATTEMP =
            new QName(MockData.WCS_PREFIX, "watertemp", MockData.WCS_PREFIX);
    protected static QName BBOXTEST =
            new QName(MockData.WCS_PREFIX, "bboxtest", MockData.WCS_PREFIX);
    protected static QName S2_OVR = new QName(MockData.WCS_PREFIX, "s2_ovr", MockData.WCS_PREFIX);
    protected static QName IR_RGB = new QName(MockData.SF_URI, "ir-rgb", MockData.SF_PREFIX);
    private static final String RGB_IR_VIEW = "RgbIrView";

    List<File> movedFiles = new ArrayList<>();

    private XpathEngine xpath;

    private File mosaic;

    @BeforeClass
    public static void setupTimeZone() {
        System.setProperty("user.timezone", "GMT");
        System.setProperty("gt2.jdbc.trace", "true");
    }

    @Before
    public void prepare() {
        xpath = XMLUnit.newXpathEngine();
    }

    @AfterClass
    public static void cleanupTimeZone() {
        System.clearProperty("user.timezone");
    }

    @Before
    public void setupWaterTemp() throws Exception {
        // setup the data
        final Catalog cat = getCatalog();
        CoverageInfo waterView = cat.getCoverageByName(WATER_VIEW);
        if (waterView != null) {
            cat.remove(waterView);
        }
        getTestData()
                .addRasterLayer(
                        WATTEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());

        // drop the store caches
        getGeoServer().reset();

        mosaic = new File(testData.getDataDirectoryRoot(), WATTEMP.getLocalPart());
        for (File file :
                FileUtils.listFiles(mosaic, new RegexFileFilter("NCOM_.*100_.*tiff"), null)) {
            File target = new File(file.getParentFile().getParentFile(), file.getName());
            movedFiles.add(target);
            if (target.exists()) {
                assertTrue(target.delete());
            }
            assertTrue(file.renameTo(target));
        }
        for (File file : FileUtils.listFiles(mosaic, new RegexFileFilter("watertemp.*"), null)) {
            assertTrue(file.delete());
        }

        // setup a view on watertemp
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("watertemp");

        final InputCoverageBand band = new InputCoverageBand("watertemp", "0");
        final CoverageBand outputBand =
                new CoverageBand(
                        Collections.singletonList(band),
                        "watertemp@0",
                        0,
                        CompositionType.BAND_SELECT);
        final CoverageView coverageView =
                new CoverageView(WATER_VIEW, Collections.singletonList(outputBand));
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo(WATER_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);

        // setup the hetero_s2_ovr mosaic
        getDataDirectory().getRoot("s2_ovr").delete();
        getTestData()
                .addRasterLayer(
                        S2_OVR,
                        "hetero_s2_ovr.zip",
                        null,
                        null,
                        StructuredCoverageStoresTest.class,
                        getCatalog());
    }

    @Before
    public void setupIR_RGB() throws Exception {
        // setting up a view that we won't modify
        final Catalog cat = getCatalog();
        CoverageInfo waterView = cat.getCoverageByName(RGB_IR_VIEW);
        if (waterView != null) {
            cat.remove(waterView);
        }

        // drop the store caches
        getGeoServer().reset();

        // remove the old database and config files
        File dir = new File(getTestData().getDataDirectoryRoot(), "ir-rgb");
        FileUtils.deleteQuietly(dir);
        getTestData().addRasterLayer(IR_RGB, "ir-rgb.zip", null, null, SystemTestData.class, cat);

        // build the view
        final CoverageBand rBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "0")),
                        "rband",
                        0,
                        CompositionType.BAND_SELECT);
        final CoverageBand gBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "1")),
                        "gband",
                        1,
                        CompositionType.BAND_SELECT);
        final CoverageBand bBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "2")),
                        "bband",
                        2,
                        CompositionType.BAND_SELECT);
        final CoverageBand irBand =
                new CoverageBand(
                        Collections.singletonList(new InputCoverageBand("ir", "0")),
                        "irband",
                        3,
                        CompositionType.BAND_SELECT);
        final CoverageView coverageView =
                new CoverageView(RGB_IR_VIEW, Arrays.asList(rBand, gBand, bBand, irBand));
        coverageView.setEnvelopeCompositionType(null);
        coverageView.setSelectedResolution(null);

        // set it in the catalog
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo(RGB_IR_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addWorkspace(SystemTestData.WCS_PREFIX, SystemTestData.WCS_URI, getCatalog());

        // setup the namespace context for this test
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("gf", "http://www.geoserver.org/rest/granules");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // nothing to do
        testData.setUpSecurity();
    }

    @Test
    public void testIndexResourcesXML() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp.xml",
                        200);
        // print(dom);
        assertXpathEvaluatesTo("watertemp", "/coverageStore/name", dom);

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp.xml",
                        200);
        // print(dom);
        assertXpathEvaluatesTo("watertemp", "/coverage/name", dom);
        assertXpathEvaluatesTo("watertemp", "/coverage/nativeName", dom);
        // todo: check there is a link to the index

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index.xml",
                        200);
        // print(dom);
        assertXpathEvaluatesTo("4", "count(//Schema/attributes/Attribute)", dom);
        assertXpathEvaluatesTo(
                "org.locationtech.jts.geom.MultiPolygon",
                "/Schema/attributes/Attribute[name='the_geom']/binding",
                dom);
        assertXpathEvaluatesTo(
                "java.lang.String", "/Schema/attributes/Attribute[name='location']/binding", dom);
        assertXpathEvaluatesTo(
                "java.util.Date", "/Schema/attributes/Attribute[name='ingestion']/binding", dom);
        assertXpathEvaluatesTo(
                "java.lang.Integer", "/Schema/attributes/Attribute[name='elevation']/binding", dom);

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("2", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:elevation",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:elevation",
                dom);

        // get the granules ids
        String octoberId =
                xpath.evaluate(
                        "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/@fid",
                        dom);
        String novemberId =
                xpath.evaluate(
                        "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/@fid",
                        dom);

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/"
                                + octoberId
                                + ".xml");
        // print(dom);
        assertXpathEvaluatesTo(octoberId, "//gf:watertemp/@fid", dom);
        assertXpathEvaluatesTo(
                "NCOM_wattemp_000_20081031T0000000_12.tiff", "//gf:watertemp/gf:location", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp/gf:elevation", dom);
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/"
                                + novemberId
                                + ".xml");
        // print(dom);
        assertXpathEvaluatesTo(novemberId, "//gf:watertemp/@fid", dom);
        assertXpathEvaluatesTo(
                "NCOM_wattemp_000_20081101T0000000_12.tiff", "//gf:watertemp/gf:location", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp/gf:elevation", dom);
    }

    @Test
    public void testGranulesOnRenamedCoverage() throws Exception {
        // rename the watertemp coverage
        CoverageStoreInfo store = catalog.getCoverageStoreByName("watertemp");
        CoverageInfo coverage = catalog.getCoverageByCoverageStore(store, "watertemp");
        coverage.setName("renamed");
        catalog.save(coverage);

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/renamed/index.xml",
                        200);
        // print(dom);
        assertXpathEvaluatesTo("4", "count(//Schema/attributes/Attribute)", dom);
        assertXpathEvaluatesTo(
                "org.locationtech.jts.geom.MultiPolygon",
                "/Schema/attributes/Attribute[name='the_geom']/binding",
                dom);
        assertXpathEvaluatesTo(
                "java.lang.String", "/Schema/attributes/Attribute[name='location']/binding", dom);
        assertXpathEvaluatesTo(
                "java.util.Date", "/Schema/attributes/Attribute[name='ingestion']/binding", dom);
        assertXpathEvaluatesTo(
                "java.lang.Integer", "/Schema/attributes/Attribute[name='elevation']/binding", dom);
    }

    @Test
    public void testIndexResourcesJSON() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index.json");

        // print(json);
        JSONObject schema = json.getJSONObject("Schema");
        JSONObject external = schema.getJSONObject("attributes");
        JSONArray attributes = external.getJSONArray("Attribute");
        assertEquals(4, attributes.size());
        assertEquals(
                "org.locationtech.jts.geom.MultiPolygon",
                attributes.getJSONObject(0).get("binding"));

        json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.json");
        // print(json);
        JSONArray features = json.getJSONArray("features");
        String octoberId = null;
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            String location = feature.getJSONObject("properties").getString("location");
            if ("NCOM_wattemp_000_20081031T0000000_12.tiff".equals(location)) {
                octoberId = feature.getString("id");
            }
        }

        json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/"
                                        + octoberId
                                        + ".json",
                                200);
        // print(json);
        features = json.getJSONArray("features");
        assertEquals(1, features.size());
        JSONObject feature = features.getJSONObject(0);
        assertEquals(octoberId, feature.get("id"));
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("NCOM_wattemp_000_20081031T0000000_12.tiff", properties.get("location"));
        assertEquals(0, properties.get("elevation"));
    }

    @Test
    public void testMissingGranule() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/notThere.xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetWrongGranule() throws Exception {
        // Parameters for the request
        String ws = "wcs";
        String cs = "watertemp";
        String g = "notThere";
        // Request path
        String requestPath =
                RestBaseController.ROOT_PATH
                        + "/workspaces/"
                        + ws
                        + "/coveragestores/"
                        + cs
                        + "/coverages/"
                        + cs
                        + "/index/granules/"
                        + g;
        // Exception path
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertThat(response.getContentAsString(), containsString(g));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    public void testDeleteSingleGranule() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);

        // get the granule ids
        String octoberId =
                xpath.evaluate(
                        "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/@fid",
                        dom);
        assertNotNull(octoberId);

        // delete it
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/"
                                + octoberId);
        assertEquals(200, response.getStatus());

        // check it's gone from the index
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        assertXpathEvaluatesTo("1", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo(
                "0",
                "count(//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff'])",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:elevation",
                dom);
    }

    @Test
    public void testDeleteSingleGranuleGsConfigStyle() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);

        // get the granule ids
        String octoberId =
                xpath.evaluate(
                        "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/@fid",
                        dom);
        assertNotNull(octoberId);

        // delete it like gsconfig does (yes, it really appens "./json" at the end)
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/"
                                + octoberId
                                + "/.json");
        assertEquals(200, response.getStatus());

        // check it's gone from the index
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        assertXpathEvaluatesTo("1", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo(
                "0",
                "count(//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff'])",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:elevation",
                dom);
    }

    @Test
    public void testDeleteAllGranules() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml",
                        200);
        assertXpathEvaluatesTo("2", "count(//gf:watertemp)", dom);
        // print(dom);

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules");
        assertEquals(200, response.getStatus());

        // check it's gone from the index
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        assertXpathEvaluatesTo("0", "count(//gf:watertemp)", dom);
    }

    @Test
    public void testDeleteByFilter() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        assertXpathEvaluatesTo("2", "count(//gf:watertemp)", dom);
        // print(dom);

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages"
                                + "/watertemp/index/granules?filter=ingestion=2008-11-01T00:00:00Z");
        assertEquals(200, response.getStatus());

        // check it's gone from the index
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:elevation",
                dom);
    }

    @Test
    public void testHarvestSingle() throws Exception {
        File file = movedFiles.get(0);
        File target = new File(mosaic, file.getName());
        assertTrue(file.renameTo(target));

        URL url = URLs.fileToUrl(target.getCanonicalFile());
        String body = url.toExternalForm();
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/external.imagemosaic",
                        body,
                        "text/plain");
        assertEquals(202, response.getStatus());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("3", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo(
                "1", "count(//gf:watertemp[gf:location = '" + file.getName() + "'])", dom);
    }

    @Test
    public void testHarvestSingleSimplePath() throws Exception {
        File file = movedFiles.get(0);
        File target = new File(mosaic, file.getName());
        assertTrue(file.renameTo(target));

        String body = target.getCanonicalPath();
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/external.imagemosaic",
                        body,
                        "text/plain");
        assertEquals(202, response.getStatus());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("3", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo(
                "1", "count(//gf:watertemp[gf:location = '" + file.getName() + "'])", dom);
    }

    @Test
    public void testHarvestMulti() throws Exception {
        for (File file : movedFiles) {
            File target = new File(mosaic, file.getName());
            assertTrue(file.renameTo(target));
        }

        // re-harvest the entire mosaic (two files refreshed, two files added)
        URL url = URLs.fileToUrl(mosaic.getCanonicalFile());
        String body = url.toExternalForm();
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/external.imagemosaic",
                        body,
                        "text/plain");
        assertEquals(202, response.getStatus());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("4", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:elevation",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:elevation",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_100_20081031T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "100",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_100_20081031T0000000_12.tiff']/gf:elevation",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00Z",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_100_20081101T0000000_12.tiff']/gf:ingestion",
                dom);
        assertXpathEvaluatesTo(
                "100",
                "//gf:watertemp[gf:location = 'NCOM_wattemp_100_20081101T0000000_12.tiff']/gf:elevation",
                dom);
    }

    @Test
    public void testGetIndexAndGranulesOnView() throws Exception {
        // get and test the index
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/waterView/index.xml",
                        200);
        // print(dom);

        assertXpathEvaluatesTo("4", "count(//Schema/attributes/Attribute)", dom);
        assertXpathEvaluatesTo(
                "org.locationtech.jts.geom.MultiPolygon",
                "/Schema/attributes/Attribute[name='the_geom']/binding",
                dom);
        assertXpathEvaluatesTo(
                "java.lang.String", "/Schema/attributes/Attribute[name='location']/binding", dom);
        assertXpathEvaluatesTo(
                "java.util.Date", "/Schema/attributes/Attribute[name='ingestion']/binding", dom);
        assertXpathEvaluatesTo(
                "java.lang.Integer", "/Schema/attributes/Attribute[name='elevation']/binding", dom);

        // get and test the granules
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/waterView/index/granules.xml",
                        200);
        print(dom);
        assertXpathEvaluatesTo("2", "count(//gf:waterView)", dom);
    }

    @Test
    public void testGetIndexAndGranulesOnMultiCoverageView() throws Exception {
        // get and test the index
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/coveragestores/ir-rgb/coverages/RgbIrView/index.xml",
                        200);
        // print(dom);

        assertXpathEvaluatesTo("2", "count(//Schema/attributes/Attribute)", dom);
        assertXpathEvaluatesTo(
                "org.locationtech.jts.geom.Polygon",
                "/Schema/attributes/Attribute[name='the_geom']/binding",
                dom);
        assertXpathEvaluatesTo(
                "java.lang.String", "/Schema/attributes/Attribute[name='location']/binding", dom);

        // get and test the granules
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/coveragestores/ir-rgb/coverages/RgbIrView/index/granules.xml",
                        200);
        print(dom);
        assertXpathEvaluatesTo("2", "count(//gf:RgbIrView)", dom);
        assertXpathExists("//gf:RgbIrView[@fid='RgbIrView.rgb.1']", dom);
        assertXpathExists("//gf:RgbIrView[@fid='RgbIrView.ir.1']", dom);
    }

    @Test
    public void testGetGranuleInMultiCoverageView() throws Exception {
        // get a single granule
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/coveragestores/ir-rgb/coverages/RgbIrView/index/granules/RgbIrView.rgb.1",
                        200);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//gf:RgbIrView)", dom);
        assertXpathExists("//gf:RgbIrView[@fid='RgbIrView.rgb.1']", dom);

        // and get the other
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/coveragestores/ir-rgb/coverages/RgbIrView/index/granules/RgbIrView.ir.1",
                        200);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//gf:RgbIrView)", dom);
        assertXpathExists("//gf:RgbIrView[@fid='RgbIrView.ir.1']", dom);
    }

    @Test
    public void testDeleteGranuleInMultiCoverageView() throws Exception {
        // delete granule
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/coveragestores/ir-rgb/coverages/RgbIrView/index/granules/RgbIrView.rgb.1");
        assertEquals(200, response.getStatus());
        // try to get it, it should provide a 404 now
        response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/coveragestores/ir-rgb/coverages/RgbIrView/index/granules/RgbIrView.rgb.1");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetGranuleInSingleCoverageView() throws Exception {
        // get a single granule
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/waterView/index/granules/waterView.watertemp.1",
                        200);
        print(dom);
        assertXpathEvaluatesTo("1", "count(//gf:waterView)", dom);
        assertXpathExists("//gf:waterView[@fid='waterView.watertemp.1']", dom);
    }

    @Test
    public void testDeleteGranuleInSingleCoverageView() throws Exception {
        // delete granule
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/waterView/index/granules/waterView.watertemp.1");
        assertEquals(200, response.getStatus());

        // try to get it, it should provide a 404 now
        response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/watertemp/coverages/waterView/index/granules/waterView.watertemp.1");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeletePurgeNoneFilter() throws Exception {
        List<String> filesAfter = removeWithPurge("&purge=none");

        // list the files, they must still contain g3.tif
        assertThat(filesAfter, allOf(hasItem("g3.tif"), hasItem("g3.tif.ovr")));
    }

    @Test
    public void testDeletePurgeAll() throws Exception {
        List<String> filesAfter = removeWithPurge("&purge=all");

        // list the files, g3 must be gone
        assertThat(filesAfter, not(hasItem(containsString("g3.tif"))));
    }

    private List<String> removeWithPurge(String purgeSpec) throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/s2_ovr/coverages/s2_ovr/index/granules.xml");
        assertXpathEvaluatesTo("4", "count(//gf:s2_ovr)", dom);
        // print(dom);

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/s2_ovr/coverages"
                                + "/s2_ovr/index/granules?filter=location LIKE 'g3%25'"
                                + purgeSpec);
        assertEquals(200, response.getStatus());

        // check it's gone from the index
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/s2_ovr/coverages/s2_ovr/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("3", "count(//gf:s2_ovr)", dom);
        assertXpathExists("//gf:s2_ovr[gf:location = 'g1.tif']", dom);
        assertXpathExists("//gf:s2_ovr[gf:location = 'g2.tif']", dom);
        assertXpathExists("//gf:s2_ovr[gf:location = 'g4.tif']", dom);

        Resource mosaicContents = getDataDirectory().getRoot("s2_ovr");
        List<String> files =
                mosaicContents.list().stream().map(r -> r.name()).collect(Collectors.toList());
        return files;
    }

    @Test
    public void testDefaultBehaviourUpdateBBoxSingleGranuleDelete() throws Exception {
        CoverageStoreInfo storeInfo = setUpBBoxTest();

        MockHttpServletResponse responseDel =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/bboxtest/coverages/bboxtest/index/granules/bboxtest.2.xml");
        assertEquals(200, responseDel.getStatus());
        // no updatebbox param, native bbox will not be updated, thus asserting for no equality
        testBBoxLayerConfiguration(
                storeInfo, (current, old) -> assertNotEquals(current, old), catalog);
    }

    @Test
    public void testUpdateBBoxTrueParameterSingleGranuleDelete() throws Exception {
        CoverageStoreInfo storeInfo = setUpBBoxTest();

        MockHttpServletResponse responseDel =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/bboxtest/coverages/bboxtest/index/granules/bboxtest.2.xml?updateBBox=true");
        assertEquals(200, responseDel.getStatus());
        testBBoxLayerConfiguration(
                storeInfo, (current, old) -> assertEquals(current, old), catalog);
    }

    @Test
    public void testUpdateBBoxWithDeleteByFilterDefaultBeahaviour() throws Exception {
        CoverageStoreInfo storeInfo = setUpBBoxTest();
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/bboxtest/coverages"
                                + "/bboxtest/index/granules?filter=location = 'test_bbox_raster2.tiff'");
        assertEquals(200, response.getStatus());
        // no updatebbox param, native bbox will not be updated, thus asserting for no equality
        testBBoxLayerConfiguration(
                storeInfo, (current, old) -> assertNotEquals(current, old), catalog);
    }

    @Test
    public void testUpdateBBoxWithDeleteByFilter() throws Exception {
        CoverageStoreInfo storeInfo = setUpBBoxTest();
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/bboxtest/coverages"
                                + "/bboxtest/index/granules?filter=location = 'test_bbox_raster2.tiff'&updateBBox=true");
        assertEquals(200, response.getStatus());
        testBBoxLayerConfiguration(
                storeInfo, (current, old) -> assertEquals(current, old), catalog);
    }

    private CoverageStoreInfo setUpBBoxTest() throws Exception {
        final Catalog cat = getCatalog();
        getGeoServer().reset();
        // remove the old files
        File dir = new File(getTestData().getDataDirectoryRoot(), "bboxtest");
        FileUtils.deleteQuietly(dir);
        getTestData()
                .addRasterLayer(
                        BBOXTEST,
                        "test_bbox_raster1.zip",
                        null,
                        null,
                        StructuredCoverageStoresTest.class,
                        cat);
        byte[] bytes = null;
        URL zipHarvest = getClass().getResource("test_bbox_granules.zip");
        try (InputStream is = zipHarvest.openStream()) {
            bytes = IOUtils.toByteArray(is);
        }
        // Create the POST request
        MockHttpServletRequest request =
                createRequest(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/bboxtest/file.imagemosaic");
        request.setMethod("POST");
        request.setContentType("application/zip");
        request.setParameter("updateBBox", "true");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        dispatch(request);
        return cat.getCoverageStoreByName("bboxtest");
    }
}
