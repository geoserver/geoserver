/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.coverages;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.springframework.mock.web.MockHttpServletResponse;

public class CoverageTest extends CoveragesTestSupport {

    private static final double EPS = 1e-6;

    @Test
    public void testGetFullCoverage() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/coverages/collections/rs:DEM/coverage");
        assertEquals(200, response.getStatus());
        assertEquals(CoveragesService.GEOTIFF_MIME, response.getContentType());

        GridCoverage2D targetCoverage = getCoverage(response);

        // check the raster space
        final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
        assertEquals(120, gridRange.getSpan(0));
        assertEquals(240, gridRange.getSpan(1));

        // check the model space
        final GeneralEnvelope expectedEnvelope =
                new GeneralEnvelope(new double[] {145, -43}, new double[] {146, -41});
        expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
        assertEquals(145, targetCoverage.getEnvelope().getMinimum(0), EPS);
        assertEquals(146, targetCoverage.getEnvelope().getMaximum(0), EPS);
        assertEquals(-43, targetCoverage.getEnvelope().getMinimum(1), EPS);
        assertEquals(-41, targetCoverage.getEnvelope().getMaximum(1), EPS);
    }

    @Test
    public void testGetBBOX() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/coverages/collections/rs:DEM/coverage?bbox=145.5,-42,146,-41.5");
        assertEquals(200, response.getStatus());
        assertEquals(CoveragesService.GEOTIFF_MIME, response.getContentType());

        GridCoverage2D targetCoverage = getCoverage(response);

        // check the raster space
        final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
        assertEquals(60, gridRange.getSpan(0));
        assertEquals(60, gridRange.getSpan(1));

        // check the model space
        final GeneralEnvelope expectedEnvelope =
                new GeneralEnvelope(new double[] {145, -43}, new double[] {146, -41});
        expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
        assertEquals(145.5, targetCoverage.getEnvelope().getMinimum(0), EPS);
        assertEquals(146, targetCoverage.getEnvelope().getMaximum(0), EPS);
        assertEquals(-42, targetCoverage.getEnvelope().getMinimum(1), EPS);
        assertEquals(-41.5, targetCoverage.getEnvelope().getMaximum(1), EPS);
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
