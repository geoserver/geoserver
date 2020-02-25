/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import it.geosolutions.imageio.utilities.ImageIOUtilities;
import java.net.URL;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.util.NumberRange;
import org.geotools.util.URLs;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverageReader;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class CoverageControllerTest extends CatalogRESTTestSupport {

    private static final double DELTA = 1E-6;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void addBlueMarbleCoverage() throws Exception {
        getTestData().addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());
    }

    @Test
    public void testGetAllByWorkspaceXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/wcs/coverages.xml");
        assertEquals(
                catalog.getCoveragesByNamespace(catalog.getNamespaceByPrefix("wcs")).size(),
                dom.getElementsByTagName("coverage").getLength());
    }

    @Test
    public void testGetAllByWorkspaceJSON() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/wcs/coverages.json");
        JSONArray coverages = json.getJSONObject("coverages").getJSONArray("coverage");
        assertEquals(
                catalog.getCoveragesByNamespace(catalog.getNamespaceByPrefix("wcs")).size(),
                coverages.size());
    }

    void addCoverageStore(boolean autoConfigureCoverage) throws Exception {
        URL zip = getClass().getResource("test-data/usa.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/file.worldimage"
                                + (!autoConfigureCoverage ? "?configure=none" : ""),
                        bytes,
                        "application/zip");
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405,
                putAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages")
                        .getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages")
                        .getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.xml");

        assertXpathEvaluatesTo("BlueMarble", "/coverage/name", dom);
        assertXpathEvaluatesTo("1", "count(//latLonBoundingBox)", dom);
        assertXpathEvaluatesTo("1", "count(//nativeFormat)", dom);
        assertXpathEvaluatesTo("1", "count(//grid)", dom);
        assertXpathEvaluatesTo("1", "count(//supportedFormats)", dom);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.json");
        JSONObject coverage = ((JSONObject) json).getJSONObject("coverage");
        assertNotNull(coverage);

        assertEquals("BlueMarble", coverage.get("name"));
    }

    @Test
    public void testGetAsHTML() throws Exception {
        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.html");
        assertEquals("html", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testGetWrongCoverage() throws Exception {
        // Parameters for the request
        String ws = "wcs";
        String cs = "BlueMarble";
        String c = "BlueMarblesssss";
        // Request path
        String requestPath =
                RestBaseController.ROOT_PATH + "/workspaces/" + ws + "/coverages/" + c + ".html";
        String requestPath2 =
                RestBaseController.ROOT_PATH
                        + "/workspaces/"
                        + ws
                        + "/coveragestores/"
                        + cs
                        + "/coverages/"
                        + c
                        + ".html";
        // Exception path
        String exception = "No such coverage: " + ws + "," + c;
        String exception2 = "No such coverage: " + ws + "," + cs + "," + c;

        // CASE 1: No coveragestore set

        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception));

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());

        // CASE 2: coveragestore set

        // First request should thrown an exception
        response = getAsServletResponse(requestPath2);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception2));

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath2 + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception2));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    public void testPutWithCalculation() throws Exception {
        String path =
                RestBaseController.ROOT_PATH
                        + "/workspaces/wcs/coveragestores/DEM/coverages/DEM.xml";
        String clearLatLonBoundingBox = "<coverage>" + "<latLonBoundingBox/>" + "</coverage>";

        MockHttpServletResponse response =
                putAsServletResponse(path, clearLatLonBoundingBox, "text/xml");
        assertEquals(
                "Couldn't remove lat/lon bounding box: \n" + response.getContentAsString(),
                200,
                response.getStatus());

        Document dom = getAsDOM(path);
        assertXpathEvaluatesTo("0.0", "/coverage/latLonBoundingBox/minx", dom);
        print(dom);

        String updateNativeBounds = "<coverage>" + "<srs>EPSG:3785</srs>" + "</coverage>";

        response = putAsServletResponse(path, updateNativeBounds, "text/xml");

        assertEquals(
                "Couldn't update native bounding box: \n" + response.getContentAsString(),
                200,
                response.getStatus());
        dom = getAsDOM(path);
        print(dom);
        assertXpathExists("/coverage/nativeBoundingBox/minx[text()!='0.0']", dom);
    }

    //    public void testPostAsJSON() throws Exception {
    //        Document dom = getAsDOM( "wfs?request=getfeature&typename=wcs:pdsa");
    //        assertEquals( "ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    //
    //        addPropertyDataStore(false);
    //        String json =
    //          "{" +
    //           "'coverage':{" +
    //              "'name':'pdsa'," +
    //              "'nativeName':'pdsa'," +
    //              "'srs':'EPSG:4326'," +
    //              "'nativeBoundingBox':{" +
    //                 "'minx':0.0," +
    //                 "'maxx':1.0," +
    //                 "'miny':0.0," +
    //                 "'maxy':1.0," +
    //                 "'crs':'EPSG:4326'" +
    //              "}," +
    //              "'nativeCRS':'EPSG:4326'," +
    //              "'store':'pds'" +
    //             "}" +
    //          "}";
    //        MockHttpServletResponse response =
    //            postAsServletResponse( RestBaseController.ROOT_PATH +
    // "/workspaces/gs/coveragestores/pds/coverages/", json, "text/json");
    //
    //        assertEquals( 201, response.getStatusCode() );
    //        assertNotNull( response.getHeader( "Location") );
    //        assertTrue( response.getHeader("Location").endsWith(
    // "/workspaces/gs/coveragestores/pds/coverages/pdsa" ) );
    //
    //        dom = getAsDOM( "wfs?request=getfeature&typename=gs:pdsa");
    //        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
    //        assertEquals( 2, dom.getElementsByTagName( "gs:pdsa").getLength());
    //    }
    //

    @Test
    public void testPostToResource() throws Exception {
        String xml = "<coverage>" + "<name>foo</name>" + "</coverage>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble",
                        xml,
                        "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testPutXML() throws Exception {
        String xml = "<coverage>" + "<title>new title</title>" + "</coverage>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.xml");
        assertXpathEvaluatesTo("new title", "/coverage/title", dom);

        CoverageInfo c = catalog.getCoverageByName("wcs", "BlueMarble");
        assertEquals("new title", c.getTitle());
    }

    @Test
    public void testPutJSON() throws Exception {
        // update the coverage title
        String jsonPayload =
                "{\n"
                        + "    \"coverage\": {\n"
                        + "        \"title\": \"new title 2\"\n"
                        + "    }\n"
                        + "}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble",
                        jsonPayload,
                        "application/json");
        assertEquals(200, response.getStatus());
        // check that the coverage title was correctly updated
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.json");
        assertThat(json.getJSONObject("coverage").getString("title"), is("new title 2"));
        CoverageInfo coverage = catalog.getCoverageByName("wcs", "BlueMarble");
        assertEquals("new title 2", coverage.getTitle());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        CoverageInfo c = catalog.getCoverageByName("wcs", "BlueMarble");

        assertTrue(c.isEnabled());
        boolean isAdvertised = c.isAdvertised();

        String xml = "<coverage>" + "<title>new title</title>" + "</coverage>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        c = catalog.getCoverageByName("wcs", "BlueMarble");
        assertTrue(c.isEnabled());
        assertEquals(isAdvertised, c.isAdvertised());
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<coverage>" + "<title>new title</title>" + "</coverage>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/BlueMarble/coverages/NonExistant",
                        xml,
                        "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        assertNotNull(catalog.getCoverageByName("wcs", "BlueMarble"));
        for (LayerInfo l : catalog.getLayers(catalog.getCoverageByName("wcs", "BlueMarble"))) {
            catalog.remove(l);
        }
        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble")
                        .getStatus());
        assertNull(catalog.getCoverageByName("wcs", "BlueMarble"));
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages/NonExistant")
                        .getStatus());
    }

    @Test
    public void testDeleteRecursive() throws Exception {

        assertNotNull(catalog.getCoverageByName("wcs", "BlueMarble"));
        assertNotNull(catalog.getLayerByName("wcs:BlueMarble"));

        assertEquals(
                403,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble")
                        .getStatus());
        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble?recurse=true")
                        .getStatus());

        assertNull(catalog.getCoverageByName("wcs", "BlueMarble"));
        assertNull(catalog.getLayerByName("wcs:BlueMarble"));
    }

    @Test
    public void testCoverageWrapping() throws Exception {
        String xml =
                "<coverage>" + "<name>tazdem</name>" + "<title>new title</title>" + "</coverage>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/DEM/coverages/DEM",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/DEM/coverages/tazdem.xml");
        assertXpathEvaluatesTo("new title", "/coverage/title", dom);

        CoverageInfo c = catalog.getCoverageByName("wcs", "tazdem");
        assertEquals("new title", c.getTitle());
        List<CoverageDimensionInfo> dimensions = c.getDimensions();
        CoverageDimensionInfo dimension = dimensions.get(0);
        assertEquals("GRAY_INDEX", dimension.getName());
        NumberRange range = dimension.getRange();
        assertEquals(Double.NEGATIVE_INFINITY, range.getMinimum(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, range.getMaximum(), DELTA);
        assertEquals("GridSampleDimension[-Infinity,Infinity]", dimension.getDescription());
        List<Double> nullValues = dimension.getNullValues();
        assertEquals(-9999.0, nullValues.get(0), DELTA);

        // Updating dimension properties
        xml =
                "<coverage>"
                        + "<name>tazdem</name>"
                        + "<title>new title</title>"
                        + "<dimensions>"
                        + "<coverageDimension>"
                        + "<name>Elevation</name>"
                        + "<description>GridSampleDimension[-100.0,1000.0]</description>"
                        + "<nullValues>"
                        + "<double>-999</double>"
                        + "</nullValues>"
                        + "<range>"
                        + "<min>-100</min>"
                        + "<max>1000</max>"
                        + "</range>"
                        + "</coverageDimension>"
                        + "</dimensions>"
                        + "</coverage>";
        response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/wcs/coveragestores/DEM/coverages/tazdem",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        c = catalog.getCoverageByName("wcs", "tazdem");
        dimensions = c.getDimensions();
        dimension = dimensions.get(0);
        assertEquals("Elevation", dimension.getName());
        range = dimension.getRange();
        assertEquals(-100.0, range.getMinimum(), DELTA);
        assertEquals(1000.0, range.getMaximum(), DELTA);
        assertEquals("GridSampleDimension[-100.0,1000.0]", dimension.getDescription());
        nullValues = dimension.getNullValues();
        assertEquals(-999.0, nullValues.get(0), DELTA);

        CoverageStoreInfo coverageStore =
                catalog.getStoreByName("wcs", "DEM", CoverageStoreInfo.class);
        GridCoverageReader reader = null;
        GridCoverage2D coverage = null;
        try {
            reader = catalog.getResourcePool().getGridCoverageReader(coverageStore, "tazdem", null);
            coverage = (GridCoverage2D) reader.read("tazdem", null);
            GridSampleDimension sampleDim = coverage.getSampleDimension(0);
            double[] noDataValues = sampleDim.getNoDataValues();
            assertEquals(-999.0, noDataValues[0], DELTA);
            range = sampleDim.getRange();
            assertEquals(-100.0, range.getMinimum(), DELTA);
            assertEquals(1000.0, range.getMaximum(), DELTA);
        } finally {
            if (coverage != null) {
                try {
                    ImageIOUtilities.disposeImage(coverage.getRenderedImage());
                    coverage.dispose(true);
                } catch (Throwable t) {
                    // Does nothing;
                }
            }
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing;
                }
            }
        }
    }
}
