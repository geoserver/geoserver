/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testing Scaling Extension
 *
 * @author Simone Giannecchini, GeoSolution SAS
 */
public class ScalingExtentionTest extends WCSTestSupport {

    private GridCoverage2D sourceCoverage;

    @Before
    public void setup() throws Exception {
        // check we can read it as a TIFF and it is similare to the origina one

        sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("BlueMarble")
                                .getGridCoverageReader(null, null)
                                .read(null);
    }

    @After
    public void close() {
        try {
            if (sourceCoverage != null) {
                scheduleForCleaning(sourceCoverage);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Test
    public void testScaleAxesByFactorXML() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageScaleAxesByFactor.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(1260, reader.getOriginalGridRange().getSpan(0));
        assertEquals(1260, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertNotNull(coverage);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
    }

    @Test
    public void testScaleToSizeXML() throws Exception {

        final File xml = new File("./src/test/resources/requestGetCoverageScaleToSize.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(1000, reader.getOriginalGridRange().getSpan(0));
        assertEquals(1000, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertNotNull(coverage);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
    }

    @Test
    public void testScaleToExtentXML() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageScaleToExtent.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(200, reader.getOriginalGridRange().getSpan(0));
        assertEquals(300, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertNotNull(coverage);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
    }

    @Test
    public void testScaleByFactorXML() throws Exception {

        final File xml = new File("./src/test/resources/requestGetCoverageScaleByFactor.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(900, reader.getOriginalGridRange().getSpan(0));
        assertEquals(900, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertNotNull(coverage);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
    }
}
