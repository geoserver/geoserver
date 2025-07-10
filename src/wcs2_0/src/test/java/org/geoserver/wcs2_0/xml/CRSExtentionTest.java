/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralBounds;
import org.geotools.referencing.CRS;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** @author Simone Giannecchini, GeoSolutions SAS */
public class CRSExtentionTest extends WCSTestSupport {
    @Test
    public void capabilties() throws Exception {
        final File xml = new File("./src/test/resources/getcapabilities/getCap.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        Document dom = postAsDOM("wcs", request);
        //         print(dom);

        // check the KVP extension 1.0.1
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs'])",
                dom);

        // proper case enforcing on values
        dom = getAsDOM("wcs?request=Getcapabilities&service=wCS");
        // print(dom);

        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo(
                "1", "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }

    @Test
    public void reprojectTo3857XML() throws Exception {
        final File xml = new File("./src/test/resources/crs/requestGetCoverageOutputCRS.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read();
            final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), targetCRS));

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralBounds expectedEnvelope = new GeneralBounds(
                    new double[] {1.6308305401213994E7, -5543147.203861462},
                    new double[] {1.6475284637403902E7, -5311971.846945147});
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope, scale, (GeneralBounds) targetCoverage.getEnvelope(), scale);
            assertEquals(360, gridRange.getSpan(0));
            assertEquals(360, gridRange.getSpan(1));

        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void testGetCoverageSubsettingCRSFullXML() throws Exception {
        final File xml = new File("./src/test/resources/crs/requestGetCoverageSubsettingCRS.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read();
            final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), targetCRS));

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralBounds expectedEnvelope = new GeneralBounds(
                    new double[] {1.6308305401213994E7, -5388389.272818998},
                    new double[] {1.636396514661063E7, -5311971.846945147});
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope, scale, (GeneralBounds) targetCoverage.getEnvelope(), scale);
            assertEquals(120, gridRange.getSpan(0));
            assertEquals(120, gridRange.getSpan(1));

        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void testGetCoverageSubsettingTrimCRSXML() throws Exception {
        final File xml = new File("./src/test/resources/crs/requestGetCoverageSubsettingTrimCRS.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read();
            final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), targetCRS));

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralBounds expectedEnvelope = new GeneralBounds(
                    new double[] {1.6308305401213994E7, -5543147.203861462},
                    new double[] {1.6475284637403902E7, -5311971.846945147});
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope, scale, (GeneralBounds) targetCoverage.getEnvelope(), scale);
            assertEquals(360, gridRange.getSpan(0));
            assertEquals(360, gridRange.getSpan(1));

        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void subsettingNativeCRSReprojectTo3857() throws Exception {
        final File xml = new File("./src/test/resources/crs/requestGetCoverageSubsettingTrimCRS2.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read();
            final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), targetCRS));

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralBounds expectedEnvelope = new GeneralBounds(
                    new double[] {1.6308305401213994E7, -5543147.203861462},
                    new double[] {1.6475284637403902E7, -5311971.846945147});
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope, scale, (GeneralBounds) targetCoverage.getEnvelope(), scale);
            assertEquals(360, gridRange.getSpan(0));
            assertEquals(360, gridRange.getSpan(1));

        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}
