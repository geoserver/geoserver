/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.restupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.util.RESTUtils;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.Status;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test class for checking REST resumable upload
 *
 * @author Nicola Lagomarsini
 */
public class ResumableUploadTest extends CatalogRESTTestSupport {
    /** Resource used for storing temporary uploads */
    private Resource tmpUploadFolder;

    /** Size for partial uploads */
    private long partialSize = 50;

    /** Relative path of the file to upload */
    private String fileName = "/relative/resumableUploadTest.shp";

    /** Root folder */
    private String root;

    @Before
    public void before() throws Exception {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        tmpUploadFolder = loader.get("tmp/upload");

        // Selection of the root directory
        File rootFile = getRootDirectory();
        root = rootFile.getAbsolutePath();

        // Setting of the global configuration
        GeoServerInfo global = getGeoServer().getGlobal();
        // Selections of the SettingsInfo associated to the GlobalSettings
        SettingsInfoImpl info = (SettingsInfoImpl) ModificationProxy.unwrap(global.getSettings());
        // If no metadata map is present, then a new one is added
        if (info.getMetadata() == null) {
            info.setMetadata(new MetadataMap());
        }
        // Selection of the metadata
        MetadataMap map = info.getMetadata();
        // Addition of the key associated to the root directory
        map.put(RESTUtils.ROOT_KEY, root);
        // Insertion of the settings inside the global ones
        global.setSettings(info);
        // Save settings
        getGeoServer().save(global);
    }

    @Test
    public void testPostRequest() throws Exception {
        String uploadId = sendPostRequest();
        assertNotNull(uploadId);
        File uploadedFile = getTempPath(uploadId);
        assertTrue(uploadedFile.exists());
        assertEquals(0, uploadedFile.length());
    }

    @Test
    public void testSuccessivePostRequest() throws Exception {
        String uploadId = sendPostRequest();
        String secondUploadId = sendPostRequest();
        assertNotNull(secondUploadId);
        assertNotEquals(uploadId, secondUploadId);
    }

    @Test
    public void testUploadFull() throws Exception {
        String uploadId = sendPostRequest();
        File uploadedFile = getTempPath(uploadId);
        assertTrue(uploadedFile.exists());
        MockHttpServletRequest request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        byte[] bigFile = generateFileAsBytes();
        request.setContent(bigFile);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(bigFile.length));
        MockHttpServletResponse response = dispatch(request);
        assertEquals(Status.SUCCESS_OK.getCode(), response.getStatus());
        assertFalse(uploadedFile.exists());
        File destinationFile = new File(FilenameUtils.concat(root, fileName.replaceAll("^/", "")));
        assertTrue(destinationFile.exists());
        assertEquals(bigFile.length, destinationFile.length());
        // Check uploaded file byte by byte
        boolean checkBytes = Arrays.equals(bigFile, toBytes(new FileInputStream(destinationFile)));
        assertTrue(checkBytes);
        // Check response content
        String restUrl = response.getContentAsString();
        assertEquals(fileName.replaceAll("^/", ""), restUrl);
    }

    @Test
    public void testUploadFullWithoutRestDir() throws Exception {
        // Set ROOT_KEY to null
        GeoServerInfo global = getGeoServer().getGlobal();
        SettingsInfoImpl info = (SettingsInfoImpl) ModificationProxy.unwrap(global.getSettings());
        MetadataMap map = info.getMetadata();
        map.remove(RESTUtils.ROOT_KEY);
        global.setSettings(info);
        getGeoServer().save(global);
        // Set root
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource data = loader.get("data");
        root = data.dir().getAbsolutePath();
        testUploadFull();
        before();
    }

    @Test
    public void testPartialUpload() throws Exception {
        String uploadId = sendPostRequest();
        MockHttpServletRequest request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        byte[] bigFile = generateFileAsBytes();
        byte[] partialFile = ArrayUtils.subarray(bigFile, 0, (int) partialSize);
        request.setContent(partialFile);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(bigFile.length));
        MockHttpServletResponse response = dispatch(request);
        assertEquals(
                ResumableUploadCatalogResource.RESUME_INCOMPLETE.getCode(), response.getStatus());
        assertEquals(null, response.getHeader("Content-Length"));
        assertEquals("0-" + (partialSize - 1), response.getHeader("Range"));
        File uploadedFile = getTempPath(uploadId);
        assertTrue(uploadedFile.exists());
        assertEquals(partialSize, uploadedFile.length());
        boolean checkBytes = Arrays.equals(partialFile, toBytes(new FileInputStream(uploadedFile)));
        assertTrue(checkBytes);
    }

    @Test
    public void testUploadPartialResume() throws Exception {
        String uploadId = sendPostRequest();
        byte[] bigFile = generateFileAsBytes();
        byte[] partialFile1 = ArrayUtils.subarray(bigFile, 0, (int) partialSize);
        // First upload

        MockHttpServletRequest request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        request.setContent(partialFile1);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(bigFile.length));
        dispatch(request);

        // Resume upload
        request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        byte[] partialFile2 =
                ArrayUtils.subarray(bigFile, (int) partialSize, (int) partialSize * 2);
        request.setContent(partialFile2);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(partialFile2.length));
        request.addHeader(
                "Content-Range",
                "bytes " + partialSize + "-" + partialSize * 2 + "/" + bigFile.length);
        MockHttpServletResponse response = dispatch(request);
        assertEquals(
                ResumableUploadCatalogResource.RESUME_INCOMPLETE.getCode(), response.getStatus());
        assertEquals(null, response.getHeader("Content-Length"));
        assertEquals("0-" + (partialSize * 2 - 1), response.getHeader("Range"));
        File uploadedFile = getTempPath(uploadId);
        assertTrue(uploadedFile.exists());
        assertEquals(partialSize * 2, uploadedFile.length());
        // Check uploaded file byte by byte
        boolean checkBytes =
                Arrays.equals(
                        ArrayUtils.addAll(partialFile1, partialFile2),
                        toBytes(new FileInputStream(uploadedFile)));
        assertTrue(checkBytes);
    }

    @Test
    public void testUploadFullResume() throws Exception {
        String uploadId = sendPostRequest();
        byte[] bigFile = generateFileAsBytes();
        byte[] partialFile1 = ArrayUtils.subarray(bigFile, 0, (int) partialSize);
        // First upload

        MockHttpServletRequest request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        request.setContent(partialFile1);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(bigFile.length));
        dispatch(request);

        // Resume upload
        request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        byte[] partialFile2 = ArrayUtils.subarray(bigFile, (int) partialSize, bigFile.length);
        request.setContent(partialFile2);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(partialFile2.length));
        request.addHeader(
                "Content-Range",
                "bytes " + partialSize + "-" + bigFile.length + "/" + bigFile.length);
        MockHttpServletResponse response = dispatch(request);
        assertEquals(Status.SUCCESS_OK.getCode(), response.getStatus());

        File uploadedFile = getTempPath(uploadId);

        assertFalse(uploadedFile.exists());
        File destinationFile = new File(FilenameUtils.concat(root, fileName.replaceAll("^/", "")));
        assertTrue(destinationFile.exists());
        assertEquals(bigFile.length, destinationFile.length());
        // Check uploaded file byte by byte
        boolean checkBytes = Arrays.equals(bigFile, toBytes(new FileInputStream(destinationFile)));
        assertTrue(checkBytes);
        // Check response content
        String restUrl = response.getContentAsString();
        assertEquals(fileName.replaceAll("^/", ""), restUrl);
    }

    @Test
    public void testPartialCleanup() throws Exception {
        // Change cleanup expirationDelay
        ResumableUploadResourceCleaner cleaner =
                (ResumableUploadResourceCleaner)
                        applicationContext.getBean("resumableUploadStorageCleaner");
        cleaner.setExpirationDelay(1000);
        // Upload file
        String uploadId = sendPostRequest();
        byte[] bigFile = generateFileAsBytes();
        byte[] partialFile = ArrayUtils.subarray(bigFile, 0, (int) partialSize);
        MockHttpServletRequest request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        request.setContent(partialFile);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(bigFile.length));
        dispatch(request);

        File uploadedFile = getTempPath(uploadId);
        assertTrue(uploadedFile.exists());
        // Wait to cleanup, max 2 minutes
        long startTime = new Date().getTime();
        while (uploadedFile.exists() && (new Date().getTime() - startTime) < 120000) {
            Thread.sleep(1000);
        }
        assertTrue(!uploadedFile.exists());
        cleaner.setExpirationDelay(300000);
    }

    @Test
    public void testSidecarCleanup() throws Exception {
        // Change cleanup expirationDelay
        ResumableUploadResourceCleaner cleaner =
                (ResumableUploadResourceCleaner)
                        applicationContext.getBean("resumableUploadStorageCleaner");
        cleaner.setExpirationDelay(1000);
        // Upload file
        String uploadId = sendPostRequest();
        byte[] bigFile = generateFileAsBytes();
        MockHttpServletRequest request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        request.setContent(bigFile);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(bigFile.length));
        dispatch(request);

        File uploadedFile = getTempPath(uploadId);
        assertFalse(uploadedFile.exists());
        File sidecarFile =
                new File(
                        FilenameUtils.concat(
                                tmpUploadFolder.dir().getCanonicalPath(), uploadId + ".sidecar"));
        assertTrue(sidecarFile.exists());
        // Wait to cleanup, max 2 minutes
        long startTime = new Date().getTime();
        while (sidecarFile.exists() && (new Date().getTime() - startTime) < 120000) {
            Thread.sleep(1000);
        }
        assertFalse(sidecarFile.exists());

        // Test GET after sidecar cleanup
        MockHttpServletResponse response =
                getAsServletResponse("/rest/resumableupload/" + uploadId, "text/plain");
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND.getCode(), response.getStatus());

        cleaner.setExpirationDelay(300000);
    }

    @Test
    public void testGetAfterPartial() throws Exception {
        String uploadId = sendPostRequest();
        byte[] bigFile = generateFileAsBytes();
        byte[] partialFile = ArrayUtils.subarray(bigFile, 0, (int) partialSize);
        // First upload

        MockHttpServletRequest request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        request.setContent(partialFile);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(bigFile.length));
        dispatch(request);
        File uploadedFile = getTempPath(uploadId);
        assertTrue(uploadedFile.exists());

        MockHttpServletResponse response =
                getAsServletResponse("/rest/resumableupload/" + uploadId, "text/plain");
        assertEquals(
                ResumableUploadCatalogResource.RESUME_INCOMPLETE.getCode(), response.getStatus());
        assertEquals(null, response.getHeader("Content-Length"));
        assertEquals("0-" + (partialSize - 1), response.getHeader("Range"));
    }

    @Test
    public void testGetAfterFull() throws Exception {
        String uploadId = sendPostRequest();
        File uploadedFile = getTempPath(uploadId);
        assertTrue(uploadedFile.exists());
        MockHttpServletRequest request = createRequest("/rest/resumableupload/" + uploadId);
        request.setMethod("PUT");
        request.setContentType("application/octet-stream");
        byte[] bigFile = generateFileAsBytes();
        request.setContent(bigFile);
        request.addHeader("Content-type", "application/octet-stream");
        request.addHeader("Content-Length", String.valueOf(bigFile.length));
        MockHttpServletResponse response = dispatch(request);
        assertEquals(Status.SUCCESS_OK.getCode(), response.getStatus());
        File sidecarFile =
                new File(
                        FilenameUtils.concat(
                                tmpUploadFolder.dir().getCanonicalPath(), uploadId + ".sidecar"));
        assertTrue(sidecarFile.exists());
    }

    private byte[] generateFileAsBytes() throws IOException {
        byte[] b = new byte[200];
        new Random().nextBytes(b);
        return b;
    }

    private byte[] toBytes(InputStream in) throws IOException {
        return IOUtils.toByteArray(in);
    }

    private File getRootDirectory() throws IOException {
        File dataDirectoryRoot = getTestData().getDataDirectoryRoot();
        File newroot = new File(dataDirectoryRoot, "RESUMABLE_UPLOADED");
        newroot.mkdirs();
        return newroot;
    }

    private File getTempPath(String uploadId) throws IOException {
        String tempPath =
                FilenameUtils.removeExtension(fileName)
                        + "_"
                        + uploadId
                        + "."
                        + FilenameUtils.getExtension(fileName);
        tempPath = tempPath.replaceAll("^/", "");
        tempPath = FilenameUtils.concat(tmpUploadFolder.dir().getCanonicalPath(), tempPath);
        return new File(tempPath);
    }

    private String sendPostRequest() throws Exception {
        MockHttpServletRequest request = createRequest("/rest/resumableupload/");
        request.setMethod("POST");
        request.setContentType("text/plain");
        request.setContent(fileName.getBytes("UTF-8"));
        request.addHeader("Content-type", "text/plain");
        MockHttpServletResponse response = dispatch(request);
        assertEquals(Status.SUCCESS_CREATED.getCode(), response.getStatus());
        String responseBody = response.getContentAsString();
        String url = responseBody.split("\\r?\\n")[1];
        String uploadId = FilenameUtils.getBaseName(url);
        return uploadId;
    }
}
