/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.geoserver.ogcapi.v1.processes.ProcessesService.EXCEPTION_TYPE_RESULT_NOT_READY;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.jayway.jsonpath.DocumentContext;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ExecutionPostTest extends AbstractExecutionTest {

    @Test
    public void testPostExecutionCoverageValues() throws Exception {
        String body = getBody("ExecuteGetCoverageValuesDefaults.json");
        JSONObject json = (JSONObject)
                postAsJSON("ogc/processes/v1/processes/gs:GetCoveragesValue/execution", body, "application/json");
        JSONArray values = json.getJSONArray("values");
        assertEquals(1, values.size());
        assertEquals(298, values.get(0));
    }

    @Test
    public void testGetExecutionInvalidInput() throws Exception {
        String body = getBody("ExecuteGetCoverageValuesInvalidName.json");
        JSONObject json = (JSONObject)
                postAsJSON("ogc/processes/v1/processes/gs:GetCoveragesValue/execution", body, "application/json");
        print(json);
        assertEquals("NoApplicableCode", json.getString("type"));
        assertThat(json.getString("title"), containsString("Could not find coverage notAGrid"));
    }

    @Test
    public void testInputOutputMimeTypes() throws Exception {
        String body = getBody("ExecuteBufferWKT.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/JTS:buffer/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertEquals("application/wkt", response.getContentType());
        assertEquals("POLYGON ((10 10, 10 -10, -10 -10, -10 10, 10 10))", response.getContentAsString());
    }

    @Test
    public void testBufferInlineGeoJSON() throws Exception {
        String body = getBody("ExecuteBufferGeoJSONRaw.json");
        MockHttpServletResponse response = postAsServletResponse(
                "ogc/processes/v1/processes/gs:BufferFeatureCollection/execution", body, "application/json");
        checkBufferCollectionJSON(response);
    }

    @Test
    public void testBufferInlineGML() throws Exception {
        String body = getBody("ExecuteBufferGMLRaw.json");
        MockHttpServletResponse response = postAsServletResponse(
                "ogc/processes/v1/processes/gs:BufferFeatureCollection/execution", body, "application/json");
        assertEquals("application/json", response.getContentType());
        // System.out.println(response.getContentAsString());
        try (GeoJSONReader reader = new GeoJSONReader(response.getContentAsString())) {
            SimpleFeatureCollection fc = reader.getFeatures();
            assertEquals(
                    MultiPolygon.class,
                    fc.getSchema().getGeometryDescriptor().getType().getBinding());
            assertEquals(1, fc.size());
        }
    }

    @Test
    public void testBufferReferenceGML() throws Exception {
        String body = getBody("ExecuteBufferGMLReference.json");
        URL collectionURL = getClass().getResource("states-FeatureCollection.xml");
        body = body.replace("$LINK", collectionURL.toExternalForm());
        MockHttpServletResponse response = postAsServletResponse(
                "ogc/processes/v1/processes/gs:BufferFeatureCollection/execution", body, "application/json");
        assertEquals("application/json", response.getContentType());
        // System.out.println(response.getContentAsString());
        try (GeoJSONReader reader = new GeoJSONReader(response.getContentAsString())) {
            SimpleFeatureCollection fc = reader.getFeatures();
            assertEquals(
                    MultiPolygon.class,
                    fc.getSchema().getGeometryDescriptor().getType().getBinding());
            assertEquals(2, fc.size());
        }
    }

    /** Inputs with cardinality greater than one and internal layer reference */
    @Test
    public void testContour() throws Exception {
        String body = getBody("ExecuteContourInternalReference.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:Contour/execution", body, "application/json");
        checkDEMContours(response);
    }

    /** Tests envelope input type */
    @Test
    public void testRectangularClip() throws Exception {
        String body = getBody("ExecuteRectangularClip.json");
        MockHttpServletResponse response = postAsServletResponse(
                "ogc/processes/v1/processes/gs:RectangularClip/execution", body, "application/json");
        checkRectangleClip(response);
    }

    @Test
    public void testCountDocument() throws Exception {
        String body = getBody("ExecuteCountDocument.json");
        URL collectionURL = getClass().getResource("states-FeatureCollection.xml");
        body = body.replace("$LINK", collectionURL.toExternalForm());

        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:Count/execution", body, "application/json");

        // System.out.println(response.getContentAsString());
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);
        assertEquals(2, json.getInt("result"));
    }

    @Test
    public void testBoundsDocument() throws Exception {
        // not a typo, parameters are the same, the process id is in the path
        String body = getBody("ExecuteCountDocument.json");
        URL collectionURL = getClass().getResource("states-FeatureCollection.xml");
        body = body.replace("$LINK", collectionURL.toExternalForm());

        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:Bounds/execution", body, "application/json");

        // System.out.println(response.getContentAsString());
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);
        JSONObject bounds = json.getJSONObject("bounds");
        assertArrayEquals(
                new Object[] {0d, 0d, 5d, 5d}, bounds.getJSONArray("bbox").toArray());
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", bounds.getString("crs"));
    }

    @Test
    public void testCDataDocumentOutput() throws Exception {
        String body = getBody("ExecuteBufferWKTDocument.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/JTS:buffer/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);
        // print(json);
        assertTrue(json.has("result"));
        JSONObject result = json.getJSONObject("result");
        assertEquals("application/wkt", result.getString("mediaType"));
        assertEquals("POLYGON ((10 10, 10 -10, -10 -10, -10 10, 10 10))", result.getString("value"));
    }

    @Test
    public void testCDataDocumentInlineJSON() throws Exception {
        String body = getBody("ExecuteBufferJSONDocument.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/JTS:buffer/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        // System.out.println(response.getContentAsString());
        JSONObject json = (JSONObject) json(response);
        assertTrue(json.has("result"));
        JSONObject result = json.getJSONObject("result");
        // JSON directly encoded in the output, no mediatype and object wrapper
        assertFalse(result.has("mediaType"));
        assertEquals("Polygon", result.getString("type"));
        assertEquals(
                JSONArray.fromObject("[[[10,10],[10,-10],[-10,-10],[-10,10],[10,10]]]"),
                result.getJSONArray("coordinates"));
    }

    /** The output is provided as a reference rather than inline, so we expect a JSON response pointing at the data */
    @Test
    public void testCDataDocumentJSONReference() throws Exception {
        String body = getBody("ExecuteBufferJSONReference.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/JTS:buffer/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);
        assertTrue(json.has("result"));
        JSONObject result = json.getJSONObject("result");
        assertEquals("application/json", result.getString("mediaType"));
        assertTrue(result.has("href"));

        // grab and follow the link, check the content
        String href = result.getString("href");
        href = href.substring(href.indexOf('?') - 3);
        JSONObject reference = (JSONObject) getAsJSON(href);
        assertEquals("Polygon", reference.getString("type"));
        assertEquals(
                JSONArray.fromObject("[[[10,10],[10,-10],[-10,-10],[-10,10],[10,10]]]"),
                reference.getJSONArray("coordinates"));
    }

    @Test
    public void testGMLDocumentInline() throws Exception {
        String body = getBody("ExecuteBufferGMLDocument.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/JTS:buffer/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        // System.out.println(response.getContentAsString());
        JSONObject json = (JSONObject) json(response);
        assertTrue(json.has("result"));
        JSONObject result = json.getJSONObject("result");
        // GML cannot be directly encoded in the output, so mediatype and escaped string
        assertEquals("text/xml; subtype=gml/2.1.2", result.getString("mediaType"));
        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><gml:Polygon xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"><gml:outerBoundaryIs><gml:LinearRing><gml:coordinates>10.0,10.0 10.0,-10.0 -10.0,-10.0 -10.0,10.0 10.0,10.0</gml:coordinates></gml:LinearRing></gml:outerBoundaryIs></gml:Polygon>";
        assertEquals(expected, result.getString("value"));
    }

    @Test
    public void testBinaryDocumentInline() throws Exception {
        String body = getBody("ExecuteBufferShapeZIPDocument.json");
        MockHttpServletResponse response = postAsServletResponse(
                "ogc/processes/v1/processes/gs:BufferFeatureCollection/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        // System.out.println(response.getContentAsString());
        JSONObject json = (JSONObject) json(response);
        assertTrue(json.has("result"));
        JSONObject result = json.getJSONObject("result");
        // GML cannot be directly encoded in the output, so mediatype and escaped string
        assertEquals("application/zip", result.getString("mediaType"));
        byte[] bytes = Base64.decodeBase64(result.getString("value"));

        Set<String> actualNames = getZipFileNames(bytes);
        assertThat(actualNames, Matchers.hasItems("features.shp", "features.shx", "features.dbf"));
    }

    private Set<String> getZipFileNames(byte[] bytes) throws IOException {
        Set<String> actualNames = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                actualNames.add(entry.getName());
                zis.closeEntry();
            }
        }
        return actualNames;
    }

    /**
     * A process with multiple raw outputs, should return a json document with the outputs
     *
     * @throws Exception
     */
    @Test
    public void testMultipleOutputs() throws Exception {
        String body = getBody("ExecuteMultipleOutputs.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:MultiRaw/execution", body, "application/json");
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);

        assertTrue(json.has("text"));
        JSONObject text = json.getJSONObject("text");
        assertEquals("text/plain", text.getString("mediaType"));
        assertEquals("This is the raw text", text.getString("value"));

        // the process returns a byte[100], so we expect a base64 encoded string of 100 bytes
        assertTrue(json.has("binary"));
        JSONObject binary = json.getJSONObject("binary");
        assertEquals("application/zip", binary.getString("mediaType"));
        assertEquals(new String(encodeBase64(new byte[100])), binary.getString("value"));
    }

    /** A process with multiple raw outputs, but only one got selected */
    @Test
    public void testMultipleOutputsSelection() throws Exception {
        String body = getBody("ExecuteMultipleOutputsSelection.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:MultiRaw/execution", body, "application/json");
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);

        assertTrue(json.has("text"));
        JSONObject text = json.getJSONObject("text");
        assertEquals("text/plain", text.getString("mediaType"));
        assertEquals("This is the raw text", text.getString("value"));

        // the "binary" output was not chosen
        assertFalse(json.has("binary"));
    }

    /**
     * A process with multiple raw outputs, requested with raw response, should return a multipart related response
     *
     * @throws Exception
     */
    @Test
    public void testMultipleOutputsRaw() throws Exception {
        String body = getBody("ExecuteMultipleOutputsRaw.json");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:MultiRaw/execution", body, "application/json");
        assertThat(response.getContentType(), Matchers.startsWith("multipart/related; boundary="));
        FileUtils.writeByteArrayToFile(new File("/tmp/test.bin"), response.getContentAsByteArray());
        Multipart multipart = getMultipart(response);
        int count = multipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String[] header = bodyPart.getHeader("Content-ID");
            assertNotNull(header);
            assertEquals(1, header.length);
            String contentId = header[0];
            if ("<text>".equals(contentId)) {
                assertEquals("text/plain", bodyPart.getContentType());
                String text = (String) bodyPart.getContent();
                assertEquals("This is the raw text", text);
            } else if ("<binary>".equals(contentId)) {
                assertEquals("application/zip", bodyPart.getContentType());
                byte[] bytes = IOUtils.toByteArray(bodyPart.getInputStream());
                assertArrayEquals(new byte[100], bytes);
            } else if ("<literal>".equals(contentId)) {
                assertEquals("text/plain", bodyPart.getContentType());
                String text = (String) bodyPart.getContent();
                assertEquals("text", text);
            } else {
                fail("Unexpected content id: " + contentId);
            }
        }
    }

    @Test
    public void testAsyncExecution() throws Exception {
        // build a POST request with async execution
        String body = getBody("ExecuteBufferGeoJSONRaw.json");
        MockHttpServletRequest request =
                createRequest("ogc/processes/v1/processes/gs:BufferFeatureCollection/execution");
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent(body.getBytes(UTF_8));
        request.addHeader("Prefer", "respond-async");
        MockHttpServletResponse response = dispatch(request);

        assertEquals(201, response.getStatus());
        String location = response.getHeader(HttpHeaders.LOCATION);
        assertThat(location, startsWith(JOBS_BASE));
        String jobId = location.substring(location.lastIndexOf('/') + 1);
        assertNotNull(jobId);

        String jobStatusRef = "ogc/processes/v1/jobs/" + jobId;
        JSONObject statusObject;
        String status;
        do {
            statusObject = (JSONObject) getAsJSON(jobStatusRef);
            assertEquals(jobId, statusObject.getString("jobID"));
            assertEquals("gs:BufferFeatureCollection", statusObject.getString("processID"));
            int progress = statusObject.getInt("progress");
            assertThat(progress, allOf(lessThanOrEqualTo(100), greaterThanOrEqualTo(0)));
            status = statusObject.getString("status");
            assertThat(status, anyOf(equalTo("accepted"), equalTo("running"), equalTo("successful")));
            String created = (String) statusObject.get("created");
            String finished = (String) statusObject.get("finished");
            // started not tested because it's not recorded in the WPS API
            if (finished != null) {
                assertTrue(created.compareTo(finished) <= 0);
            }
            Thread.sleep(20);
        } while (!"successful".equals(status));

        checkStatusLinks(statusObject, jobId);

        MockHttpServletResponse processOutput = getAsServletResponse("ogc/processes/v1/jobs/" + jobId + "/results");
        checkBufferCollectionJSON(processOutput);
    }

    @Test
    public void testEcho() throws Exception {
        String body = getBody("ExecuteEcho.json");
        JSONObject json =
                (JSONObject) postAsJSON("ogc/processes/v1/processes/gs:Echo/execution", body, "application/json");
        print(json);
        assertEquals("hithere", json.getString("stringOutput"));
        assertEquals(15, json.getDouble("doubleOutput"), 0);
        JSONObject bboxOutput = json.getJSONObject("boundingBoxOutput");
        assertEquals(List.of(0d, 0d, 1d, 1d), bboxOutput.getJSONArray("bbox"));
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/3857", bboxOutput.getString("crs"));
    }

    @Test
    public void testEchoImageMultipartValue() throws Exception {
        byte[] pngBytes = readSamplePng();
        String pngBase64 = new String(Base64.encodeBase64(pngBytes));
        String body = getBody("ExecuteEchoImage.json");
        body = body.replace("$IMAGE_VALUE", pngBase64);
        body = body.replace("$IMAGE_TRANSMISSION_MODE", "value");
        body = body.replace("$RESPONSE_TYPE", "raw");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:Echo/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertThat(response.getContentType(), Matchers.startsWith("multipart/related; boundary="));
        Multipart multipart = getMultipart(response);
        int count = multipart.getCount();
        assertEquals(2, count);
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String[] header = bodyPart.getHeader("Content-ID");
            assertNotNull(header);
            assertEquals(1, header.length);
            String contentId = header[0];
            if ("<stringOutput>".equals(contentId)) {
                assertEquals("text/plain", bodyPart.getContentType());
                String text = (String) bodyPart.getContent();
                assertEquals("hello", text);
            } else if ("<imageOutput>".equals(contentId)) {
                assertEquals("image/png", bodyPart.getContentType());
                byte[] imageBytes = IOUtils.toByteArray(bodyPart.getInputStream());
                assertImagesIdentical(pngBytes, imageBytes);
            } else {
                fail("Unexpected content id: " + contentId);
            }
        }
    }

    @Test
    public void testEchoImageDocumentValue() throws Exception {
        byte[] pngBytes = readSamplePng();
        String pngBase64 = new String(Base64.encodeBase64(pngBytes));
        String body = getBody("ExecuteEchoImage.json");
        body = body.replace("$IMAGE_VALUE", pngBase64);
        body = body.replace("$IMAGE_TRANSMISSION_MODE", "value");
        body = body.replace("$RESPONSE_TYPE", "document");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:Echo/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        DocumentContext json = getAsJSONPath(response);

        assertEquals("hello", json.read("$.stringOutput"));
        assertEquals("image/png", json.read("$.imageOutput.mediaType"));
        String imageBase64 = json.read("$.imageOutput.value");
        byte[] imageBytes = Base64.decodeBase64(imageBase64);
        assertImagesIdentical(pngBytes, imageBytes);
    }

    @Test
    public void testEchoImageDocumentReference() throws Exception {
        byte[] pngBytes = readSamplePng();
        String pngBase64 = new String(Base64.encodeBase64(pngBytes));
        String body = getBody("ExecuteEchoImage.json");
        body = body.replace("$IMAGE_VALUE", pngBase64);
        body = body.replace("$IMAGE_TRANSMISSION_MODE", "reference");
        body = body.replace("$RESPONSE_TYPE", "document");
        MockHttpServletResponse response =
                postAsServletResponse("ogc/processes/v1/processes/gs:Echo/execution", body, "application/json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        DocumentContext json = getAsJSONPath(response);

        assertEquals("hello", json.read("$.stringOutput"));
        assertEquals("image/png", json.read("$.imageOutput.mediaType"));
        String imageReference = json.read("$.imageOutput.href");
        String geoserverBase = "http://localhost:8080/geoserver";
        assertThat(imageReference, startsWith(geoserverBase));

        MockHttpServletResponse referenceResponse =
                getAsServletResponse(imageReference.substring(geoserverBase.length() + 1));
        assertEquals(200, referenceResponse.getStatus());
        assertEquals("image/png", referenceResponse.getContentType());
        byte[] imageBytes = getBinary(referenceResponse);
        assertImagesIdentical(pngBytes, imageBytes);
    }

    @Test
    public void testWaitAndDismiss() throws Exception {
        // build a POST request with async execution, with a very long pause in Echo (5 minutes)
        String body = getBody("ExecuteEchoLongPause.json");
        MockHttpServletRequest request = createRequest("ogc/processes/v1/processes/gs:Echo/execution");
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent(body.getBytes(UTF_8));
        request.addHeader("Prefer", "respond-async");
        MockHttpServletResponse response = dispatch(request);

        assertEquals(201, response.getStatus());
        String location = response.getHeader(HttpHeaders.LOCATION);
        assertThat(location, startsWith(JOBS_BASE));
        String jobId = location.substring(location.lastIndexOf('/') + 1);
        assertNotNull(jobId);

        // wait until running
        String jobStatusRef = "ogc/processes/v1/jobs/" + jobId;
        JSONObject statusObject;
        String status;
        do {
            statusObject = (JSONObject) getAsJSON(jobStatusRef);
            assertEquals(jobId, statusObject.getString("jobID"));
            assertEquals("gs:Echo", statusObject.getString("processID"));
            int progress = statusObject.getInt("progress");
            assertThat(progress, allOf(lessThanOrEqualTo(100), greaterThanOrEqualTo(0)));
            status = statusObject.getString("status");
            assertThat(status, anyOf(equalTo("accepted"), equalTo("running")));
            String created = (String) statusObject.get("created");
            String started = (String) statusObject.get("started");
            String finished = (String) statusObject.get("started");
            if (started != null) {
                assertTrue(created.compareTo(started) <= 0);
            }
            if (finished != null) {
                assertTrue(started.compareTo(finished) <= 0);
            }
            Thread.sleep(20);
        } while (!"running".equals(status));

        // check the results are not available yet and the error message is the correct one
        DocumentContext exception = getAsJSONPath(jobStatusRef + "/results", 404);
        assertEquals(EXCEPTION_TYPE_RESULT_NOT_READY, exception.read("$.type"));

        // dismiss the job
        response = deleteAsServletResponse(jobStatusRef);
        assertEquals(200, response.getStatus());
        JSON statusDismissed = json(response);
        assertEquals("dismissed", ((JSONObject) statusDismissed).getString("status"));
    }
}
