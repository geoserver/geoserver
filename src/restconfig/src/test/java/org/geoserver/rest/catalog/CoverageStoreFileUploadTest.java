/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.geoserver.security.impl.DefaultFileAccessManager.GEOSERVER_DATA_SANDBOX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.security.FileAccessManager;
import org.geoserver.security.impl.DefaultFileAccessManager;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.referencing.FactoryException;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.URLs;
import org.geotools.util.factory.GeoTools;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class CoverageStoreFileUploadTest extends CatalogRESTTestSupport {

    @ClassRule
    public static TemporaryFolder temp = new TemporaryFolder();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        DataStoreInfo store = cb.buildDataStore("h2test");
        store.getConnectionParameters().put("dbtype", "h2");
        store.getConnectionParameters()
                .put("database", new File(getDataDirectory().findOrCreateDir("data"), "h2_test").getAbsolutePath());
        store.getConnectionParameters().put("MVCC", true);
        catalog.save(store);
    }

    @Before
    public void cleanup() throws IOException {
        // wipe out everything under "mosaic"
        CoverageInfo coverage = getCatalog().getResourceByName("mosaic", CoverageInfo.class);
        if (coverage != null) {
            removeStore(
                    coverage.getStore().getWorkspace().getName(),
                    coverage.getStore().getName());
        }
        removeStore("sf", "usa");
    }

    @Test
    public void testWorldImageUploadZipped() throws Exception {
        uploadUSAWorldImage();

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

        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/store%20with%20spaces/file.worldimage",
                bytes,
                "application/zip");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testUploadImageMosaic() throws Exception {
        URL zip = MockData.class.getResource("watertemp.zip");
        byte[] bytes = getBytes(zip);

        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/watertemp/file.imagemosaic",
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

        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/watertemp-repo/file.imagemosaic",
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

        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/watertemp2/file.imagemosaic",
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
        MockHttpServletRequest request = createRequest(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/watertemp2/file.imagemosaic");
        request.setMethod("POST");
        request.setContentType("application/zip");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        // Get The response
        dispatch(request);
        // Get the Mosaic Reader
        GridCoverageReader reader = storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
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
                createRequest(RestBaseController.ROOT_PATH + "/workspaces/wcs/coveragestores/BlueMarble");
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

        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/watertemp3/file.imagemosaic",
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
        MockHttpServletRequest request = createRequest(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/watertemp3/external.imagemosaic");
        request.setMethod("POST");
        request.setContentType("text/plain");
        request.setContent(("file:///" + outputDirectory.dir().getAbsolutePath()).getBytes(StandardCharsets.UTF_8));
        request.addHeader("Content-type", "text/plain");
        // Get The response
        dispatch(request);
        // Get the Mosaic Reader
        GridCoverageReader reader = storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
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
            MockHttpServletRequest request = createRequest(
                    RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/watertemp4/file.imagemosaic");
            request.setMethod("POST");
            request.setContentType("application/zip");
            request.setContent(bytes);
            request.addHeader("Content-type", "application/zip");
            // Get The response
            dispatch(request);
            // Get the Mosaic Reader
            reader2 =
                    (StructuredGridCoverage2DReader) storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
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
                    fail("Could not find the expected zip entry NCOM_wattemp_000_20081102T0000000_12.tiff");
                }
            }
            reader2 = uploadGeotiffAndCheck(storeInfo, bytes, "NCOM_wattemp_000_20081102T0000000_12.tiff");
            // now re-upload, used to blow up
            reader2 = uploadGeotiffAndCheck(storeInfo, bytes, "NCOM_wattemp_000_20081102T0000000_12.tiff");
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
        // Create the POST request
        MockHttpServletRequest request = createRequest(RestBaseController.ROOT_PATH
                + "/workspaces/gs/coveragestores/watertemp5/file.imagemosaic?filename="
                + filename);
        request.setMethod("POST");
        request.setContentType("image/tiff");
        request.setContent(bytes);
        request.addHeader("Content-type", "image/tiff");
        // Get The response
        assertEquals(202, dispatch(request).getStatus());
        // Get the Mosaic Reader
        StructuredGridCoverage2DReader reader2 =
                (StructuredGridCoverage2DReader) storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
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

    private void configureCoverageInfo(CatalogBuilder builder, CoverageStoreInfo storeInfo, GridCoverage2DReader reader)
            throws Exception {
        // coverage read params
        CoverageInfo cinfo = builder.buildCoverage(reader, new HashMap<>());

        // get the coverage name
        String name = reader.getGridCoverageNames()[0];
        cinfo.setName(name);
        cinfo.setNativeCoverageName(name);

        // add the store
        getCatalog().add(cinfo);
    }

    @Test
    public void testDefaultBehaviourUpdateBBoxPOST() throws Exception {
        setUpBBoxTest("bboxtest", "test_bbox_raster1.zip");
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
                createRequest(RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/bboxtest/file.imagemosaic");
        request.setMethod("POST");
        request.setContentType("application/zip");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        dispatch(request);
        testBBoxLayerConfiguration(storeInfo, (current, old) -> assertNotEquals(current, old), catalog);
        CoverageInfo coverage = getCatalog().getResourceByName("bboxtest", CoverageInfo.class);
        if (coverage != null) {
            removeStore(
                    coverage.getStore().getWorkspace().getName(),
                    coverage.getStore().getName());
        }
    }

    @Test
    public void testUpdateBBoxTrueParameterPOST() throws Exception {

        setUpBBoxTest("bboxtest2", "test_bbox_raster1.zip");
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
        MockHttpServletRequest request = createRequest(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/bboxtest2/file.imagemosaic");
        request.setMethod("POST");
        request.setParameter("updateBBox", "true");
        request.setContentType("application/zip");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        dispatch(request);
        testBBoxLayerConfiguration(storeInfo, (current, old) -> assertEquals(current, old), catalog);
        CoverageInfo coverage = getCatalog().getResourceByName("bboxtest2", CoverageInfo.class);
        if (coverage != null) {
            removeStore(
                    coverage.getStore().getWorkspace().getName(),
                    coverage.getStore().getName());
        }
    }

    @Test
    public void testUpdateBBoxTrueOnCoverageView() throws Exception {
        setUpBBoxTest("coverageview", "test_bbox_coverageview.zip");
        byte[] bytes = null;
        // check the coverage is actually there
        Catalog cat = getCatalog();
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("coverageview");
        assertNotNull(storeInfo);

        final CoverageView coverageView = buildCoverageView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo("coverageView", storeInfo, builder);
        coverageInfo.getParameters().put("USE_IMAGEN_IMAGEREAD", "false");
        coverageInfo.getDimensions().get(0).setName("rasterA");
        coverageInfo.getDimensions().get(1).setName("rasterB");
        cat.add(coverageInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("coverageView");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // Harvesting
        URL zipHarvest = getClass().getResource("test_bbox_singlegranule.zip");

        try (InputStream is = zipHarvest.openStream()) {
            bytes = IOUtils.toByteArray(is);
        }
        // Create the POST request
        MockHttpServletRequest request = createRequest(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/coverageview/file.imagemosaic");
        request.setMethod("POST");
        request.setParameter("updateBBox", "true");
        request.setContentType("application/zip");
        request.setContent(bytes);
        request.addHeader("Content-type", "application/zip");
        dispatch(request);
        testBBoxLayerConfiguration(storeInfo, (current, old) -> assertEquals(current, old), catalog);
        CoverageInfo coverage = getCatalog().getResourceByName("coverageview", CoverageInfo.class);
        if (coverage != null) {
            removeStore(
                    coverage.getStore().getWorkspace().getName(),
                    coverage.getStore().getName());
        }
    }

    private CoverageView buildCoverageView() {
        final CoverageView.CoverageBand aBand = new CoverageView.CoverageBand(
                Arrays.asList(new CoverageView.InputCoverageBand("rasterA", "0")),
                "rasterA",
                0,
                CoverageView.CompositionType.BAND_SELECT);
        final CoverageView.CoverageBand bBand = new CoverageView.CoverageBand(
                Arrays.asList(new CoverageView.InputCoverageBand("rasterB", "0")),
                "rasterB",
                1,
                CoverageView.CompositionType.BAND_SELECT);

        final CoverageView coverageView = new CoverageView("coverageView", Arrays.asList(aBand, bBand));
        coverageView.setEnvelopeCompositionType(CoverageView.EnvelopeCompositionType.UNION);
        coverageView.setSelectedResolution(null);
        return coverageView;
    }

    private void setUpBBoxTest(String storeName, String fileName) throws Exception {
        // Upload of the Mosaic via REST
        URL zip = getClass().getResource(fileName);
        byte[] bytes = getBytes(zip);

        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/gs/coveragestores/" + storeName + "/file.imagemosaic",
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

    @Test
    public void testWorldImageUploadExternalZipDirectory() throws Exception {
        // get the path to a directory
        File file = temp.getRoot();
        String body = file.getAbsolutePath();
        // the request will fail since it won't attempt to copy a directory
        MockHttpServletResponse response = putAsServletResponse(
                ROOT_PATH + "/workspaces/foo/coveragestores/bar/external.worldimage", body, "application/zip");
        assertEquals(500, response.getStatus());
        assertThat(response.getContentAsString(), startsWith("Error renaming zip file from "));
        // verify that the external file was not deleted
        assertTrue("The external file was unexpectedly deleted", file.exists());
    }

    @Test
    public void testWorldImageUploadExternalZipExistingDirectory() throws Exception {
        // create a file to copy and get its path
        File file1 = temp.newFile("test1.zip");
        String body = file1.getAbsolutePath();
        // create the file in the data directory
        File file2 = getResourceLoader().createDirectory("data/foo/bar1/test1.zip");
        // the request will fail since it won't overwrite an existing zip file
        MockHttpServletResponse response = putAsServletResponse(
                ROOT_PATH + "/workspaces/foo/coveragestores/bar1/external.worldimage", body, "application/zip");
        assertEquals(500, response.getStatus());
        assertThat(response.getContentAsString(), startsWith("Error renaming zip file from "));
        // verify that the external file was not deleted
        assertTrue("The external file was unexpectedly deleted", file1.exists());
        // verify that the file in the data directory was not deleted
        assertTrue("The file in the data directory was unexpectedly deleted", file2.isDirectory());
    }

    @Test
    public void testWorldImageUploadExternalZipBadFile() throws Exception {
        // create a file that is not a valid zip file and get its path
        File file = temp.newFile("test2.zip");
        String body = file.getAbsolutePath();
        // the request will fail unzipping since it is not a valid zip fail
        MockHttpServletResponse response = putAsServletResponse(
                ROOT_PATH + "/workspaces/foo/coveragestores/bar2/external.worldimage", body, "application/zip");
        assertEquals(500, response.getStatus());
        assertEquals("Error occured unzipping file", response.getContentAsString());
        // verify that the external file was not deleted
        assertTrue("The external file was unexpectedly deleted", file.exists());
        // verify that the zip file was deleted from the data directory
        assertEquals(
                "The data directory file was not deleted",
                Resource.Type.UNDEFINED,
                getResourceLoader().get("data/foo/bar2/test2.zip").getType());
    }

    @Test
    public void testWorldImageUploadExternalZipValid() throws Exception {
        // create a valid zip file and get its path
        File file = temp.newFile("test3.zip");
        FileUtils.copyURLToFile(getClass().getResource("test-data/usa.zip"), file);
        String body = file.getAbsolutePath();
        // verify that the coverage does not already exist
        assertNull(getCatalog().getCoverageStoreByName("sf", "usa"));
        assertNull(getCatalog().getCoverageByName("sf", "usa"));
        // the request should succeed
        MockHttpServletResponse response = putAsServletResponse(
                ROOT_PATH + "/workspaces/sf/coveragestores/usa/external.worldimage", body, "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());
        String content = response.getContentAsString();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));
        assertEquals("coverageStore", d.getDocumentElement().getNodeName());
        // verify that the coverage was created successfully
        assertNotNull(getCatalog().getCoverageStoreByName("sf", "usa"));
        assertNotNull(getCatalog().getCoverageByName("sf", "usa"));
        // verify that the external file was not deleted
        assertTrue("The external file was unexpectedly deleted", file.exists());
        // verify that the zip file was deleted from the data directory
        assertEquals(
                "The data directory file was not deleted",
                Resource.Type.UNDEFINED,
                getResourceLoader().get("data/sf/usa/test3.zip").getType());
    }

    @Test
    public void testFilesystemSandbox() throws Exception {
        // set up a system sandbox
        File systemSandbox = new File("./target/systemSandbox").getCanonicalFile();
        System.setProperty(GEOSERVER_DATA_SANDBOX, systemSandbox.getAbsolutePath());
        DefaultFileAccessManager fam = (DefaultFileAccessManager) FileAccessManager.lookupFileAccessManager();
        fam.reload();

        try {
            uploadUSAWorldImage();

            // check the coverage has been uploaded inside the system sandbox
            CoverageStoreInfo cs = getCatalog().getCoverageStoreByName("sf", "usa");
            assertNotNull(cs);
            // compute a OS independent test string (replacement is for Windows)
            String expected =
                    new File(systemSandbox, "/sf/usa/usa.png").getAbsolutePath().replace("\\", "/");
            assertThat(cs.getURL(), Matchers.containsString(expected));
        } finally {
            System.clearProperty(GEOSERVER_DATA_SANDBOX);
            fam.reload();
        }
    }

    private void uploadUSAWorldImage() throws Exception {
        URL zip = getClass().getResource("test-data/usa.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/sf/coveragestores/usa/file.worldimage",
                bytes,
                "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());

        String content = response.getContentAsString();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));
        assertEquals("coverageStore", d.getDocumentElement().getNodeName());
    }
}
