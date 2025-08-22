/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.MonkeyProcess;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ExecutionGetTest extends AbstractExecutionTest {

    @Test
    public void testGetExecutionCoverageValues() throws Exception {
        // call with defaults: sync execution, inline response, json format
        JSONObject json = (JSONObject) getAsJSON("ogc/processes/v1/processes/gs:GetCoveragesValue/execution?name="
                + getLayerId(SystemTestData.TASMANIA_DEM) + "&x=145.220&y=-41.504");
        JSONArray values = json.getJSONArray("values");
        assertEquals(1, values.size());
        assertEquals(298, values.get(0));
    }

    @Test
    public void testGetExecutionInvalidInput() throws Exception {
        // call with defaults: sync execution, inline response, json format
        JSONObject json = (JSONObject) getAsJSON(
                "ogc/processes/v1/processes/gs:GetCoveragesValue/execution?name=notAGrid&x=145.220&y=-41.504");
        assertEquals("NoApplicableCode", json.getString("type"));
        assertThat(json.getString("title"), containsString("Could not find coverage notAGrid"));
    }

    @Test
    public void testEchoProcess() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/processes/v1/processes/gs:Echo/execution?stringInput=Hello%20World&boundingBoxInput=0,0,1,1&boundingBoxInput[crs]=http://www.opengis.net/def/crs/EPSG/0/3857");
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);
        // print(json);
        assertEquals("Hello World", json.getString("stringOutput"));
        assertEquals(5, json.getDouble("doubleOutput"), 0);
        JSONObject bboxOutput = json.getJSONObject("boundingBoxOutput");
        assertEquals(List.of(0d, 0d, 1d, 1d), bboxOutput.getJSONArray("bbox"));
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/3857", bboxOutput.getString("crs"));
    }

    @Test
    public void testEchoProcessNoBoundingBox() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/processes/v1/processes/gs:Echo/execution?stringInput=Hello%20World");
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);
        // print(json);
        assertEquals("Hello World", json.getString("stringOutput"));
        assertEquals(5, json.getDouble("doubleOutput"), 0);
        assertFalse(json.has("boundingBoxOutput"));
    }

    @Test
    public void testInputOutputMimeTypes() throws Exception {
        // call with custom format for input and output
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/processes/v1/processes/JTS:buffer/execution?geom[type]=application/wkt&geom=POINT(0 0)&distance=10&capStyle=Square&response[f]=application/wkt");
        assertEquals(200, response.getStatus());
        assertEquals("application/wkt", response.getContentType());
        assertEquals("POLYGON ((10 10, 10 -10, -10 -10, -10 10, 10 10))", response.getContentAsString());
    }

    @Test
    public void testBufferReferenceGML() throws Exception {
        URL collectionURL = getClass().getResource("states-FeatureCollection.xml");
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/processes/v1/processes/gs:BufferFeatureCollection/execution?features[href]="
                        + collectionURL.toExternalForm()
                        + "&features[type]=text/xml; subtype=wfs-collection/1.1&distance=1000&response[f]=application/json");
        checkBufferCollectionJSON(response);
    }

    /** Tests envelope input type */
    @Test
    public void testRectangularClip() throws Exception {
        URL collectionURL = getClass().getResource("states-FeatureCollection.json");
        MockHttpServletResponse response =
                getAsServletResponse("ogc/processes/v1/processes/gs:RectangularClip/execution?features[href]="
                        + collectionURL.toExternalForm()
                        + "&features[type]=application/json&clip=0,0,2,2&response[f]=application/json");
        checkRectangleClip(response);
    }

    /**
     * Single output, should automatically be inline
     *
     * @throws Exception
     */
    @Test
    public void testCount() throws Exception {
        URL collectionURL = getClass().getResource("states-FeatureCollection.xml");
        MockHttpServletResponse response =
                getAsServletResponse("ogc/processes/v1/processes/gs:Count/execution?features[href]="
                        + collectionURL.toExternalForm() + "&features[type]=text/xml; subtype=wfs-collection/1.1");

        assertEquals("text/plain", response.getContentType());
        assertEquals("2", response.getContentAsString());
    }

    @Test
    public void testBoundsDocument() throws Exception {
        URL collectionURL = getClass().getResource("states-FeatureCollection.xml");
        MockHttpServletResponse response =
                getAsServletResponse("ogc/processes/v1/processes/gs:Bounds/execution?features[href]="
                        + collectionURL.toExternalForm() + "&features[type]=text/xml; subtype=wfs-collection/1.1");

        assertEquals("application/json", response.getContentType());
        // here we have a raw bbox response, not wrapped in a document (1 output -> raw)
        JSONObject json = (JSONObject) json(response);
        assertArrayEquals(
                new Object[] {0d, 0d, 5d, 5d}, json.getJSONArray("bbox").toArray());
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", json.getString("crs"));
    }

    /** A process with multiple raw outputs, should return a json document with the outputs */
    @Test
    public void testMultipleOutputs() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/processes/v1/processes/gs:MultiRaw/execution?id=theLiteral&binaryMimeType=application/zip");
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);

        assertTrue(json.has("literal"));
        assertEquals("theLiteral", json.getString("literal"));

        assertTrue(json.has("text"));
        JSONObject text = json.getJSONObject("text");
        assertEquals("text/plain", text.getString("mediaType"));
        assertEquals("This is the raw text", text.getString("value"));

        // the process returns a byte[100], so we expect a base64 encoded string of 100 bytes
        assertTrue(json.has("binary"));
        JSONObject binary = json.getJSONObject("binary");
        assertEquals("application/zip", binary.getString("mediaType"));
        assertEquals(new String(Base64.encodeBase64(new byte[100])), binary.getString("value"));
    }

    /**
     * A process with multiple raw outputs, should return a json document with the outputs. The literal output is not
     * included, the others are included by default.
     */
    @Test
    public void testMultipleOutputsSelectTwo() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/processes/v1/processes/gs:MultiRaw/execution?id=theLiteral&binaryMimeType=application/zip&literal[include]=false");
        assertEquals("application/json", response.getContentType());
        JSONObject json = (JSONObject) json(response);

        assertFalse(json.has("literal"));

        assertTrue(json.has("text"));
        JSONObject text = json.getJSONObject("text");
        assertEquals("text/plain", text.getString("mediaType"));
        assertEquals("This is the raw text", text.getString("value"));

        // the process returns a byte[100], so we expect a base64 encoded string of 100 bytes
        assertTrue(json.has("binary"));
        JSONObject binary = json.getJSONObject("binary");
        assertEquals("application/zip", binary.getString("mediaType"));
        assertEquals(new String(Base64.encodeBase64(new byte[100])), binary.getString("value"));
    }

    /**
     * A process with multiple raw outputs, but only one got selected. Should fall back to raw response as only one
     * output was left
     */
    @Test
    public void testMultipleOutputsSelectOne() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/processes/v1/processes/gs:MultiRaw/execution?"
                        + "id=text&binaryMimeType=application/zip&text[include]=true&binary[include]=false&literal[include]=false");
        assertEquals("text/plain", response.getContentType());
        assertEquals("This is the raw text", response.getContentAsString());
    }

    @Test
    public void testAsync() throws Exception {
        // call with async execution
        MockHttpServletRequest request =
                createRequest("ogc/processes/v1/processes/gs:Monkey/execution?id=test123&response[f]=application/json");
        request.setMethod("GET");
        request.addHeader("Prefer", "respond-async");
        request.setContent(new byte[] {});
        MockHttpServletResponse response = dispatch(request);

        assertEquals(201, response.getStatus());
        String location = response.getHeader(HttpHeaders.LOCATION);
        assertThat(location, startsWith(JOBS_BASE));
        String jobId = location.substring(location.lastIndexOf('/') + 1);
        assertNotNull(jobId);

        // step
        MonkeyProcess.progress("test123", 20, false);
        JSONObject statusObject = waitMonkey(jobId, Set.of("accepted", "running"), "running");
        assertEquals(11, statusObject.getInt("progress")); // 11% because WPS accounts for encoding time

        // read collection
        String geojson = getBody("states-FeatureCollection.json");
        SimpleFeatureCollection fc = null;
        try (GeoJSONReader reader = new GeoJSONReader(geojson)) {
            fc = reader.getFeatures();
        }

        // complete
        MonkeyProcess.exit("test123", fc, false);
        statusObject = waitMonkey(jobId, Set.of("successful", "running"), "successful");
        assertEquals(100, statusObject.getInt("progress"));

        checkStatusLinks(statusObject, jobId);

        MockHttpServletResponse processOutput = getAsServletResponse("ogc/processes/v1/jobs/" + jobId + "/results");
        assertEquals("application/json", processOutput.getContentType());
        try (GeoJSONReader reader = new GeoJSONReader(processOutput.getContentAsString())) {
            SimpleFeatureCollection rc = reader.getFeatures();
            assertEquals(
                    MultiPolygon.class,
                    rc.getSchema().getGeometryDescriptor().getType().getBinding());
            assertEquals(2, rc.size());
        }
    }

    @Test
    public void testAsyncDismiss() throws Exception {
        // call with async execution
        MockHttpServletRequest request =
                createRequest("ogc/processes/v1/processes/gs:Monkey/execution?id=test123&response[f]=application/json");
        request.setMethod("GET");
        request.addHeader("Prefer", "respond-async");
        request.setContent(new byte[] {});
        MockHttpServletResponse response = dispatch(request);

        assertEquals(201, response.getStatus());
        String location = response.getHeader(HttpHeaders.LOCATION);
        assertThat(location, startsWith(JOBS_BASE));
        String jobId = location.substring(location.lastIndexOf('/') + 1);
        assertNotNull(jobId);

        // step
        MonkeyProcess.progress("test123", 20, true);
        JSONObject statusObject = waitMonkey(jobId, Set.of("accepted", "running"), "running");
        assertEquals(11, statusObject.getInt("progress")); // 11% because WPS accounts for encoding time

        // dismiss
        String statusRef = "ogc/processes/v1/jobs/" + jobId;
        response = deleteAsServletResponse(statusRef);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        JSONObject dismissStatus = (JSONObject) json(response);
        print(dismissStatus);

        assertEquals("dismissed", dismissStatus.getString("status"));

        // attempt another step (won't work but should unlock the execution)
        MonkeyProcess.progress("test123", 40, false);

        // check it really got dismissed
        JSONObject status = (JSONObject) getAsJSON(statusRef);
        assertEquals("dismissed", status.getString("status"));
    }

    private JSONObject waitMonkey(String jobId, Set<String> acceptableStatuses, String waitForStatus) throws Exception {
        String jobStatusRef = "ogc/processes/v1/jobs/" + jobId;
        JSONObject statusObject;
        String status;
        do {
            statusObject = (JSONObject) getAsJSON(jobStatusRef);
            assertEquals("gs:Monkey", statusObject.getString("processID"));
            int progress = statusObject.getInt("progress");
            assertThat(progress, allOf(lessThanOrEqualTo(100), greaterThanOrEqualTo(0)));
            status = statusObject.getString("status");
            assertTrue("Found unexpected status: " + status, acceptableStatuses.contains(status));

            Thread.sleep(20);
        } while (!waitForStatus.equals(status));
        return statusObject;
    }
}
