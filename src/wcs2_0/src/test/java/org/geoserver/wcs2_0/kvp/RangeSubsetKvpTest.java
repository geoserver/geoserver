/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Testing Scaling Extension KVP
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class RangeSubsetKvpTest extends WCSKVPTestSupport {

    private Logger LOGGER = Logging.getLogger(RangeSubsetKvpTest.class);

    @Test
    public void capabilties() throws Exception {
        Document dom = getAsDOM("wcs?reQueSt=GetCapabilities&seErvIce=WCS");
        // print(dom);

        // check the KVP extension 1.0.1
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_range-subsetting/1.0/conf/record-subsetting'])",
                dom);

        // proper case enforcing on values
        dom = getAsDOM("wcs?request=Getcapabilities&service=wCS");
        // print(dom);

        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])",
                dom);
        assertXpathEvaluatesTo(
                "1", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }

    @Test
    public void test9to3() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__multiband&&Format=image/tiff&RANGESUBSET=Band1,Band6,Band7");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("gtiff", "gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:32611", true)));
        assertEquals(68, reader.getOriginalGridRange().getSpan(0));
        assertEquals(56, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(3, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("multiband")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void test9to4() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__multiband&&Format=image/tiff&RANGESUBSET=Band1,Band6,Band9,Band4");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("gtiff", "gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:32611", true)));
        assertEquals(68, reader.getOriginalGridRange().getSpan(0));
        assertEquals(56, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(4, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("multiband")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void test9to7() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__multiband&&Format=image/tiff&RANGESUBSET=Band1,Band6,Band4,Band9,Band8,Band7,Band2");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("gtiff", "gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:32611", true)));
        assertEquals(68, reader.getOriginalGridRange().getSpan(0));
        assertEquals(56, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(7, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("multiband")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void testBasic() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&RANGESUBSET=RED_BAND");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(360, reader.getOriginalGridRange().getSpan(0));
        assertEquals(360, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(1, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("BlueMarble")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void testRange() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&RANGESUBSET=RED_BAND:BLUE_BAND");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(360, reader.getOriginalGridRange().getSpan(0));
        assertEquals(360, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(3, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("BlueMarble")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void mixed() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&RANGESUBSET=RED_BAND:BLUE_BAND,RED_BAND,GREEN_BAND");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(360, reader.getOriginalGridRange().getSpan(0));
        assertEquals(360, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(5, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("BlueMarble")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void testWrong() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&RANGESUBSET=Band1,GREEN_BAND");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.NoSuchField.getExceptionCode(), "Band1");
    }
}
