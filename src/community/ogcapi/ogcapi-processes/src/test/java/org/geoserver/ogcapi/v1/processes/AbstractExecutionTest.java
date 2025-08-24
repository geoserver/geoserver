/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.mail.util.SharedByteArrayInputStream;
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
}
