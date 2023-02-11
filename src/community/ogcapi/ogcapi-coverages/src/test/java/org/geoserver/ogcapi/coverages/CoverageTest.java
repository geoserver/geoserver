/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.coverages;

import static org.geoserver.catalog.ResourceInfo.TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.DimensionPresentation;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.springframework.mock.web.MockHttpServletResponse;

public class CoverageTest extends CoveragesTestSupport {

    private static final double EPS = 1e-6;

    @Test
    public void testGetFullCoverage() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/coverages/collections/rs:DEM/coverage");
        assertBBOXDEM(response, 120, 240, 145, 146, -43, -41);
    }

    @Test
    public void testGetBBOX() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/coverages/collections/rs:DEM/coverage?bbox=145.5,-42,146,-41.5");
        assertBBOXDEM(response, 60, 60, 145.5, 146, -42, -41.5);
    }

    @Test
    public void testSpatialSubsetRange() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/coverages/collections/rs:DEM/coverage?subset=Long(145.5:146),Lat(-42:-41.5)");
        assertBBOXDEM(response, 60, 60, 145.5, 146, -42, -41.5);
    }

    @Test
    public void testSpatialSubsetSlice() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/coverages/collections/rs:DEM/coverage?subset=Long(145.5),Lat(-41.5)");
        // a one pixel raster
        assertBBOXDEM(response, 1, 1, 145.5, 145.508333, -41.508333, -41.5);
    }

    @Test
    public void testSpatialSubsetWrongAxis() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/coverages/collections/rs:DEM/coverage?subset=CutIt(145.5)");
        assertEquals(400, response.getStatus());
        assertEquals("application/json", response.getContentType());
        DocumentContext error = getAsJSONPath(response);
        assertEquals("Invalid axis label provided: CutIt", error.read("title"));
    }

    @Test
    public void testDatetime() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, DimensionPresentation.LIST, null, null, null);

        // expected values for the various time slices
        double[] values = {20.53, 14.77, 14.60, 20.66, 20.53, 14.77};
        // work with different time resolutions
        String[] suffixes = {"", "-01", "-01-01", "-01-01T00:00:00Z", "-01-01T00:00:00.000Z"};
        for (String suffix : suffixes) {
            for (int i = 0; i < 6; i++) {
                String date = (2014 + i) + suffix;
                MockHttpServletResponse response =
                        getAsServletResponse(
                                "ogc/coverages/collections/sf:timeseries/coverage?datetime="
                                        + date);
                assertEquals(200, response.getStatus());
                assertEquals(CoveragesService.GEOTIFF_MIME, response.getContentType());

                GridCoverage2D targetCoverage = getCoverage(response);
                double[] pixel = new double[1];
                targetCoverage.evaluate(new Point2D.Double(14.32, 40.66), pixel);
                assertEquals(values[i], pixel[0], 0.01d);
                targetCoverage.dispose(true);
            }
        }
    }

    @Test
    public void testVersionHeader() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/coverages/collections/rs:DEM/coverage");
        assertTrue(headerHasValue(response, "API-Version", "1.0.1"));
    }

    private void assertBBOXDEM(
            MockHttpServletResponse response,
            int width,
            int height,
            double xMin,
            double xMax,
            double yMin,
            double yMax)
            throws IOException, FactoryException {
        assertEquals(200, response.getStatus());
        assertEquals(CoveragesService.GEOTIFF_MIME, response.getContentType());

        GridCoverage2D targetCoverage = getCoverage(response);

        // check the raster space
        final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
        assertEquals(width, gridRange.getSpan(0));
        assertEquals(height, gridRange.getSpan(1));

        // check the model space
        assertEquals(xMin, targetCoverage.getEnvelope().getMinimum(0), EPS);
        assertEquals(xMax, targetCoverage.getEnvelope().getMaximum(0), EPS);
        assertEquals(yMin, targetCoverage.getEnvelope().getMinimum(1), EPS);
        assertEquals(yMax, targetCoverage.getEnvelope().getMaximum(1), EPS);
    }

    private GridCoverage2D getCoverage(MockHttpServletResponse response) throws IOException {
        byte[] binary = getBinary(response);

        File file = File.createTempFile("temp", ".tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, binary);

        GeoTiffReader reader = new GeoTiffReader(file);
        try {
            // set for immediate load, no need to release files after
            ParameterValue<Boolean> deferredLoad = GeoTiffFormat.USE_JAI_IMAGEREAD.createValue();
            deferredLoad.setValue(false);
            return reader.read(new GeneralParameterValue[] {deferredLoad});
        } finally {
            try {
                reader.dispose();
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to dispose of the reader, unexpected but not necessarily serious",
                        e);
            }
            file.delete();
        }
    }
}
