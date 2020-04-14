/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.*;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.URLs;
import org.geotools.util.factory.GeoTools;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.referencing.FactoryException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class CoverageStoreFileUploadTest extends CatalogRESTTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        DataStoreInfo store = cb.buildDataStore("h2test");
        store.getConnectionParameters().put("dbtype", "h2");
        store.getConnectionParameters()
                .put(
                        "database",
                        new File(getDataDirectory().findOrCreateDir("data"), "h2_test")
                                .getAbsolutePath());
        store.getConnectionParameters().put("MVCC", true);
        catalog.save(store);
    }

    @Before
    public void cleanup() throws IOException {
        // wipe out everything under "mosaic"
        CoverageInfo coverage = getCatalog().getResourceByName("mosaic", CoverageInfo.class);
        if (coverage != null) {
            removeStore(
                    coverage.getStore().getWorkspace().getName(), coverage.getStore().getName());
        }
    }

    @Test
    public void testWorldImageUploadZipped() throws Exception {
        URL zip = getClass().getResource("test-data/usa.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/coveragestores/usa/file.worldimage",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());

        String content = response.getContentAsString();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));
        assertEquals("coverageStore", d.getDocumentElement().getNodeName());

        CoverageStoreInfo cs = getCatalog().getCoverageStoreByName("sf", "usa");
        assertNotNull(cs);
        CoverageInfo ci = getCatalog().getCoverageByName("sf", "usa");
        assertNotNull(ci);
    }

    @Test
    @Ignore
    // fixing https://osgeo-org.atlassian.net/browse/GEOS-6845, re-enable when a proper fix for
    // spaces in
    // name has been made
    public void testUploadWithSpaces() throws Exception {
        URL zip = getClass().getResource("test-data/usa.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/store%20with%20spaces/file.worldimage",
                        bytes,
                        "application/zip");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testUploadImageMosaic() throws Exception {
        URL zip = MockData.class.getResource("watertemp.zip");
        byte[] bytes = getBytes(zip);

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/watertemp/file.imagemosaic",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());

        // check the response contents
        String content = response.getContentAsString();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));

        XMLAssert.assertXpathEvaluatesTo("watertemp", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);

        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());
    }

    @Test
    public void testUploadImageMosaicRepoReference() throws Exception {
        URL zip = CoverageStoreFileUploadTest.class.getResource("watertemp-repo.zip");
        byte[] bytes = getBytes(zip);

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/watertemp-repo/file.imagemosaic",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());

        // check the response contents
        String content = response.getContentAsString();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));

        XMLAssert.assertXpathEvaluatesTo("watertemp-repo", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);

        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp-repo");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp-repo");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // check harvesting happened as expected
        DataStore ds = (DataStore) getCatalog().getDataStoreByName("h2test").getDataStore(null);
        assertNotNull(ds);
        SimpleFeatureSource fs = ds.getFeatureSource("watertemp-repo");
        assertNotNull(fs);
        assertEquals(4, fs.getCount(Query.ALL));
    }

    public byte[] getBytes(URL zip) throws IOException {
        try (InputStream is = zip.openStream()) {
            return IOUtils.toByteArray(is);
        }
    }

    @Test
    public void testHarvestImageMosaic() throws Exception {
        // Upload of the Mosaic via REST
        URL zip = MockData.class.getResource("watertemp.zip");
        byte[] bytes = getBytes(zip);

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/watertemp2/file.imagemosaic",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());

        // check the response contents
        String content = response.getContentAsString();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));

        XMLAssert.assertXpathEvaluatesTo("watertemp2", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);

        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp2");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp2");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // Harvesting of the Mosaic
        URL zipHarvest = MockData.class.getResource("harvesting.zip");
        // Extract a Byte array from the zip file
        try (InputStream is = zipHarvest.openStream()) {
            bytes = IOUtils.toByteArray(is);
        }
        // Create the POST request
        MockHttpServletRequest request =
                createRequest(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/watertemp2/file.imagemosaic");
        request.setMethod("POST");
        request.setContentType("application/zip");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        // Get The response
        dispatch(request);
        // Get the Mosaic Reader
        GridCoverageReader reader =
                storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
        // Test if all the TIME DOMAINS are present
        String[] metadataNames = reader.getMetadataNames();
        assertNotNull(metadataNames);
        assertEquals("true", reader.getMetadataValue("HAS_TIME_DOMAIN"));
        assertEquals(
                "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z",
                reader.getMetadataValue(metadataNames[0]));
    }

    @Test
    public void testHarvestNotAllowedOnSimpleCoverageStore() throws Exception {
        // add bluemarble
        getTestData().addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());

        // Harvesting of the Mosaic
        URL zipHarvest = MockData.class.getResource("harvesting.zip");
        // Extract a Byte array from the zip file
        byte[] bytes = getBytes(zipHarvest);
        // Create the POST request
        MockHttpServletRequest request =
                createRequest(
                        RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/BlueMarble");
        request.setMethod("POST");
        request.setContentType("application/zip");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        // Get The response
        MockHttpServletResponse response = dispatch(request);
        // not allowed
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testHarvestImageMosaicWithDirectory() throws Exception {
        // Upload of the Mosaic via REST
        URL zip = MockData.class.getResource("watertemp.zip");
        byte[] bytes = getBytes(zip);

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/watertemp3/file.imagemosaic",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());

        // check the response contents
        String content = response.getContentAsString();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));

        XMLAssert.assertXpathEvaluatesTo("watertemp3", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);

        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp3");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp3");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // Harvesting of the Mosaic
        URL zipHarvest = MockData.class.getResource("harvesting.zip");
        Resource newZip = Files.asResource(new File("./target/harvesting2.zip"));
        // Copy the content of the first zip to the second
        IOUtils.copyStream(zipHarvest.openStream(), newZip.out(), true, true);
        Resource outputDirectory = Files.asResource(new File("./target/harvesting"));
        RESTUtils.unzipFile(newZip, outputDirectory);
        // Create the POST request
        MockHttpServletRequest request =
                createRequest(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/watertemp3/external.imagemosaic");
        request.setMethod("POST");
        request.setContentType("text/plain");
        request.setContent(
                ("file:///" + outputDirectory.dir().getAbsolutePath()).getBytes("UTF-8"));
        request.addHeader("Content-type", "text/plain");
        // Get The response
        dispatch(request);
        // Get the Mosaic Reader
        GridCoverageReader reader =
                storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
        // Test if all the TIME DOMAINS are present
        String[] metadataNames = reader.getMetadataNames();
        assertNotNull(metadataNames);
        assertEquals("true", reader.getMetadataValue("HAS_TIME_DOMAIN"));
        assertEquals(
                "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z",
                reader.getMetadataValue(metadataNames[0]));
        // Removal of the temporary directory
        outputDirectory.delete();
    }

    @Test
    public void testHarvestExternalImageMosaic() throws Exception {
        // Check if an already existing directory called "mosaic" is present
        URL resource = getClass().getResource("test-data/mosaic");
        if (resource != null) {
            File oldDir = URLs.urlToFile(resource);
            if (oldDir.exists()) {
                FileUtils.deleteDirectory(oldDir);
            }
        }
        // reading of the mosaic directory
        Resource mosaic = readMosaic();
        // Creation of the builder for building a new CoverageStore
        CatalogBuilder builder = new CatalogBuilder(getCatalog());
        // Definition of the workspace associated to the coverage
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("gs");
        // Creation of a CoverageStore
        CoverageStoreInfo store = builder.buildCoverageStore("watertemp4");
        store.setURL(URLs.fileToUrl(Resources.find(mosaic)).toExternalForm());
        store.setWorkspace(ws);
        ImageMosaicFormat imageMosaicFormat = new ImageMosaicFormat();
        store.setType((imageMosaicFormat.getName()));
        // Addition to the catalog
        getCatalog().add(store);
        builder.setStore(store);
        // Input reader used for reading the mosaic folder
        GridCoverage2DReader reader = null;
        // Reader used for checking if the mosaic has been configured correctly
        StructuredGridCoverage2DReader reader2 = null;

        try {
            // Selection of the reader to use for the mosaic
            reader = imageMosaicFormat.getReader(URLs.fileToUrl(Resources.find(mosaic)));

            // configure the coverage
            configureCoverageInfo(builder, store, reader);

            // check the coverage is actually there
            CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp4");
            assertNotNull(storeInfo);
            CoverageInfo ci = getCatalog().getCoverageByName("mosaic");
            assertNotNull(ci);
            assertEquals(storeInfo, ci.getStore());

            // Harvesting of the Mosaic
            URL zipHarvest = MockData.class.getResource("harvesting.zip");
            // Extract a Byte array from the zip file
            byte[] bytes = getBytes(zipHarvest);
            // Create the POST request
            MockHttpServletRequest request =
                    createRequest(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/gs/coveragestores/watertemp4/file.imagemosaic");
            request.setMethod("POST");
            request.setContentType("application/zip");
            request.setContent(bytes);
            request.addHeader("Content-type", "application/zip");
            // Get The response
            MockHttpServletResponse response = dispatch(request);
            // Get the Mosaic Reader
            reader2 =
                    (StructuredGridCoverage2DReader)
                            storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
            // Test if all the TIME DOMAINS are present
            String[] metadataNames = reader2.getMetadataNames();
            assertNotNull(metadataNames);
            assertEquals("true", reader2.getMetadataValue("HAS_TIME_DOMAIN"));
            assertEquals(
                    "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z",
                    reader2.getMetadataValue(metadataNames[0]));
            // Removal of all the data associated to the mosaic
            reader2.delete(true);
        } finally {
            // Reader disposal
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
            if (reader2 != null) {
                try {
                    reader2.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    @Test
    public void testReHarvestSingleTiff() throws Exception {
        // Check if an already existing directory called "mosaic" is present
        URL resource = getClass().getResource("test-data/mosaic");
        if (resource != null) {
            File oldDir = URLs.urlToFile(resource);
            if (oldDir.exists()) {
                FileUtils.deleteDirectory(oldDir);
            }
        }
        // reading of the mosaic directory
        Resource mosaic = readMosaic();
        // Creation of the builder for building a new CoverageStore
        CatalogBuilder builder = new CatalogBuilder(getCatalog());
        // Definition of the workspace associated to the coverage
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("gs");
        // Creation of a CoverageStore
        CoverageStoreInfo store = builder.buildCoverageStore("watertemp5");
        store.setURL(URLs.fileToUrl(Resources.find(mosaic)).toExternalForm());
        store.setWorkspace(ws);
        ImageMosaicFormat imageMosaicFormat = new ImageMosaicFormat();
        store.setType((imageMosaicFormat.getName()));
        // Addition to the catalog
        getCatalog().add(store);
        builder.setStore(store);
        // Input reader used for reading the mosaic folder
        GridCoverage2DReader reader = null;
        // Reader used for checking if the mosaic has been configured correctly
        StructuredGridCoverage2DReader reader2 = null;

        try {
            // Selection of the reader to use for the mosaic
            reader = imageMosaicFormat.getReader(URLs.fileToUrl(Resources.find(mosaic)));

            // configure the coverage
            configureCoverageInfo(builder, store, reader);

            // check the coverage is actually there
            CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp5");
            assertNotNull(storeInfo);
            CoverageInfo ci = getCatalog().getCoverageByName("mosaic");
            assertNotNull(ci);
            assertEquals(storeInfo, ci.getStore());

            // Harvesting of the Mosaic
            URL zipHarvest = MockData.class.getResource("harvesting.zip");
            // Extract the first file as payload (the tiff)
            byte[] bytes = null;
            try (ZipInputStream zis = new ZipInputStream(zipHarvest.openStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if ("NCOM_wattemp_000_20081102T0000000_12.tiff".equals(entry.getName())) {
                        bytes = IOUtils.toByteArray(zis);
                    }
                }
                if (bytes == null) {
                    fail(
                            "Could not find the expected zip entry NCOM_wattemp_000_20081102T0000000_12.tiff");
                }
            }
            reader2 =
                    uploadGeotiffAndCheck(
                            storeInfo, bytes, "NCOM_wattemp_000_20081102T0000000_12.tiff");
            // now re-upload, used to blow up
            reader2 =
                    uploadGeotiffAndCheck(
                            storeInfo, bytes, "NCOM_wattemp_000_20081102T0000000_12.tiff");
            // Removal of all the data associated to the mosaic
            reader2.delete(true);
        } finally {
            // Reader disposal
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
            if (reader2 != null) {
                try {
                    reader2.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    private StructuredGridCoverage2DReader uploadGeotiffAndCheck(
            CoverageStoreInfo storeInfo, byte[] bytes, String filename) throws Exception {
        StructuredGridCoverage2DReader reader2;
        // Create the POST request
        MockHttpServletRequest request =
                createRequest(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/watertemp5/file.imagemosaic?filename="
                                + filename);
        request.setMethod("POST");
        request.setContentType("image/tiff");
        request.setContent(bytes);
        request.addHeader("Content-type", "image/tiff");
        // Get The response
        assertEquals(202, dispatch(request).getStatus());
        // Get the Mosaic Reader
        reader2 =
                (StructuredGridCoverage2DReader)
                        storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
        // Test if all the TIME DOMAINS are present
        String[] metadataNames = reader2.getMetadataNames();
        assertNotNull(metadataNames);
        assertEquals("true", reader2.getMetadataValue("HAS_TIME_DOMAIN"));
        assertEquals(
                "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z",
                reader2.getMetadataValue(metadataNames[0]));
        return reader2;
    }

    private Resource readMosaic() throws FactoryException, IOException {
        // Select the zip file containing the mosaic
        URL mosaicZip = getClass().getResource("test-data/watertemp2.zip");
        Resource zipFile = Files.asResource(URLs.urlToFile(mosaicZip));

        // Creation of another zip file which is a copy of the one before
        Resource newZip = zipFile.parent().get("watertemp2_temp.zip");
        // Copy the content of the first zip to the second
        IOUtils.copyStream(zipFile.in(), newZip.out(), true, true);

        Resource mosaic = zipFile.parent().get("mosaic");
        mosaic.delete();

        RESTUtils.unzipFile(newZip, mosaic);
        return mosaic;
    }

    private void configureCoverageInfo(
            CatalogBuilder builder, CoverageStoreInfo storeInfo, GridCoverage2DReader reader)
            throws Exception {
        // coverage read params
        final Map customParameters = new HashMap();

        CoverageInfo cinfo = builder.buildCoverage(reader, customParameters);

        // get the coverage name
        String name = reader.getGridCoverageNames()[0];
        cinfo.setName(name);
        cinfo.setNativeCoverageName(name);

        // add the store
        getCatalog().add(cinfo);
    }

    @Test
    public void testDefaultBehaviourUpdateBBoxPOST() throws Exception {
        setUpBBoxTest("bboxtest");
        byte[] bytes = null;
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("bboxtest");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("bboxtest");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());
        // Harvesting
        URL zipHarvest = getClass().getResource("test_bbox_granules.zip");
        try (InputStream is = zipHarvest.openStream()) {
            bytes = IOUtils.toByteArray(is);
        }
        // Create the POST request
        MockHttpServletRequest request =
                createRequest(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/bboxtest/file.imagemosaic");
        request.setMethod("POST");
        request.setContentType("application/zip");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        dispatch(request);
        testBBoxLayerConfiguration(
                storeInfo, (current, old) -> assertNotEquals(current, old), catalog);
        CoverageInfo coverage = getCatalog().getResourceByName("bboxtest", CoverageInfo.class);
        if (coverage != null) {
            removeStore(
                    coverage.getStore().getWorkspace().getName(), coverage.getStore().getName());
        }
    }

    @Test
    public void testUpdateBBoxTrueParameterPOST() throws Exception {

        setUpBBoxTest("bboxtest2");
        byte[] bytes = null;
        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("bboxtest2");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("bboxtest2");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // Harvesting
        URL zipHarvest = getClass().getResource("test_bbox_granules.zip");

        try (InputStream is = zipHarvest.openStream()) {
            bytes = IOUtils.toByteArray(is);
        }
        // Create the POST request
        MockHttpServletRequest request =
                createRequest(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/bboxtest2/file.imagemosaic");
        request.setMethod("POST");
        request.setParameter("updateBBox", "true");
        request.setContentType("application/zip");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        dispatch(request);
        testBBoxLayerConfiguration(
                storeInfo, (current, old) -> assertEquals(current, old), catalog);
        CoverageInfo coverage = getCatalog().getResourceByName("bboxtest2", CoverageInfo.class);
        if (coverage != null) {
            removeStore(
                    coverage.getStore().getWorkspace().getName(), coverage.getStore().getName());
        }
    }

    private void setUpBBoxTest(String storeName) throws Exception {
        // Upload of the Mosaic via REST
        URL zip = getClass().getResource("test_bbox_raster1.zip");
        byte[] bytes = getBytes(zip);

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/"
                                + storeName
                                + "/file.imagemosaic",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());
    }

    static void testBBoxLayerConfiguration(
            CoverageStoreInfo storeInfo,
            BiConsumer<ReferencedEnvelope, ReferencedEnvelope> assertConsumer,
            Catalog catalog)
            throws IOException {
        StructuredGridCoverage2DReader sr =
                (StructuredGridCoverage2DReader) storeInfo.getGridCoverageReader(null, null);
        String[] coveragesNames = sr.getGridCoverageNames();
        for (String name : coveragesNames) {
            ReferencedEnvelope current = new ReferencedEnvelope(sr.getOriginalEnvelope(name));
            ReferencedEnvelope old =
                    catalog.getCoverageByCoverageStore(storeInfo, name).getNativeBoundingBox();
            assertConsumer.accept(current, old);
        }
    }
}
