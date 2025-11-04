/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.geoserver.ogcapi.v1.processes.JobStatus.RESULTS_REL;
import static org.geoserver.ogcapi.v1.processes.JobStatus.STATUS_REL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.mail.util.SharedByteArrayInputStream;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.imageio.ImageIO;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.wps.MonkeyProcess;
import org.geoserver.wps.MultiRawProcess;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.DataUtilities;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Processors;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hamcrest.Matchers;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.mock.web.MockHttpServletResponse;

public class AbstractExecutionTest extends OGCApiTestSupport {

    public static final String JOBS_BASE = "http://localhost:8080/geoserver/ogc/processes/v1/jobs/";

    static {
        Processors.addProcessFactory(MonkeyProcess.getFactory());
        Processors.addProcessFactory(MultiRawProcess.getFactory());
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    protected void checkBufferCollectionJSON(MockHttpServletResponse response) throws IOException {
        assertEquals("application/json", response.getContentType());
        try (GeoJSONReader reader = new GeoJSONReader(response.getContentAsString())) {
            FeatureCollection fc = reader.getFeatures();
            assertEquals(
                    MultiPolygon.class,
                    fc.getSchema().getGeometryDescriptor().getType().getBinding());
            assertEquals(2, fc.size());
        }
    }

    protected void checkDEMContours(MockHttpServletResponse response) throws IOException {
        assertEquals("application/json", response.getContentType());
        try (GeoJSONReader reader = new GeoJSONReader(response.getContentAsString())) {
            SimpleFeatureCollection fc = reader.getFeatures();
            assertEquals(
                    LineString.class,
                    fc.getSchema().getGeometryDescriptor().getType().getBinding());
            // currently it extracs 137, making the test tolerant to small contour algorithm variations
            assertThat(fc.size(), Matchers.greaterThan(100));
            ReferencedEnvelope envelope = new ReferencedEnvelope(145, 146, -41, -43, DefaultGeographicCRS.WGS84);
            Set<Double> levels = Set.of(100d, 200d, 300d);
            try (SimpleFeatureIterator fi = fc.features()) {
                while (fi.hasNext()) {
                    SimpleFeature feature = fi.next();
                    LineString sf = (LineString) feature.getDefaultGeometry();
                    assertTrue(
                            "Contour line is not inside the data envelope",
                            envelope.contains(sf.getEnvelopeInternal()));
                    assertTrue(levels.contains(feature.getAttribute("value")));
                }
            }
        }
    }

    protected void checkRectangleClip(MockHttpServletResponse response) throws Exception {
        assertEquals("application/json", response.getContentType());
        // System.out.println(response.getContentAsString());
        try (GeoJSONReader reader = new GeoJSONReader(response.getContentAsString())) {
            SimpleFeatureCollection fc = reader.getFeatures();
            assertEquals(
                    MultiPolygon.class,
                    fc.getSchema().getGeometryDescriptor().getType().getBinding());
            assertEquals(1, fc.size());
            SimpleFeature first = DataUtilities.first(fc);
            assertEquals("Illinois", first.getAttribute("STATE_NAME"));
        }
    }

    protected String getBody(String fileName) throws IOException {
        String body;
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            body = IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        return body;
    }

    /** Parses a multipart message from the response */
    protected Multipart getMultipart(MockHttpServletResponse response) throws MessagingException, IOException {
        byte[] body = response.getContentAsByteArray();
        String contentType = response.getContentType();
        ByteArrayDataSource ds = new ByteArrayDataSource(new SharedByteArrayInputStream(body), contentType);
        // MimeMultipart(DataSource) will parse using the provided Content-Type (boundary included)
        return new MimeMultipart(ds);
    }

    protected void checkStatusLinks(JSONObject statusObject, String jobId) {
        // check the links
        JSONArray links = statusObject.getJSONArray("links");
        boolean resultsLinkFound = false;
        boolean selfLinkFound = false;
        for (int i = 0; i < links.size(); i++) {
            JSONObject link = links.getJSONObject(i);
            if (!"application/json".equals(link.getString("type"))) continue;
            String rel = link.getString("rel");
            if (RESULTS_REL.equals(rel)) {
                String resultsHref = link.getString("href");
                assertEquals(JOBS_BASE + jobId + "/results", resultsHref);
                resultsLinkFound = true;
            } else if (STATUS_REL.equals(rel)) {
                String href = link.getString("href");
                assertEquals(JOBS_BASE + jobId, href);
                selfLinkFound = true;
            } else {
                fail("Unexpected link rel: " + rel);
            }
        }
        assertTrue(selfLinkFound);
        assertTrue(resultsLinkFound);
    }

    protected void assertImagesIdentical(byte[] inputBytes, byte[] outputBytes) throws IOException {
        RenderedImage inputImage = ImageIO.read(new ByteArrayInputStream(inputBytes));
        RenderedImage outputImage = ImageIO.read(new ByteArrayInputStream(outputBytes));
        assertEquals(inputImage.getColorModel(), outputImage.getColorModel());
        assertEquals(inputImage.getSampleModel(), outputImage.getSampleModel());
        assertEquals(inputImage.getWidth(), outputImage.getWidth());
        assertEquals(inputImage.getHeight(), outputImage.getHeight());
        int[] inputPixel = new int[1];
        int[] outputPixel = new int[1];
        Raster inputData = inputImage.getData();
        Raster outputData = inputImage.getData();
        for (int i = 0; i < inputImage.getHeight(); i++) {
            for (int j = 0; j < inputImage.getWidth(); j++) {
                inputData.getPixel(j, i, inputPixel);
                outputData.getPixel(j, i, outputPixel);
                assertArrayEquals(inputPixel, outputPixel);
            }
        }
    }

    protected byte[] readSamplePng() throws IOException {
        try (InputStream is = ExecutionGetTest.class.getResourceAsStream("bathy.png")) {
            assertNotNull(is);
            return is.readAllBytes();
        }
    }
}
