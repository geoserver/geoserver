/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Testing Scaling Extension KVP
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
@RunWith(Parameterized.class)
public class ScaleKvpTest extends WCSKVPTestSupport {

    private Logger LOGGER = Logging.getLogger(ScaleKvpTest.class);
    private String axisPrefix;
    private String subsetPrefix;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {"", ""},
                    {
                        "http://www.opengis.net/def/axis/OGC/1/",
                        "http://www.opengis.net/def/axis/OGC/0/"
                    }
                });
    }

    public ScaleKvpTest(String axisPrefix, String subsetPrefix) {
        this.axisPrefix = axisPrefix;
        this.subsetPrefix = subsetPrefix;
    }

    @Test
    public void capabilties() throws Exception {
        final File xml = new File("./src/test/resources/getcapabilities/getCap.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        Document dom = postAsDOM("wcs", request);
        //         print(dom);

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
    public void scaleFactor() throws Exception {

        // subsample
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=0.5");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check we can read it as a TIFF and it is similare to the origina one
        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange().getSpan(0) / 2,
                    targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange().getSpan(1) / 2,
                    targetCoverage.getGridGeometry().getGridRange().getSpan(1));
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // upsample
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=2");

        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check we can read it as a TIFF and it is similare to the origina one
        readerTarget = new GeoTiffReader(file);
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange().getSpan(0) * 2,
                    targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange().getSpan(1) * 2,
                    targetCoverage.getGridGeometry().getGridRange().getSpan(1));
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // error 0
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=0");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidScaleFactor.getExceptionCode(), "0.0");

        // error < 0
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=-12");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidScaleFactor.getExceptionCode(), "-12.0");
    }

    @Test
    public void scaleAxesByFactor() throws Exception {

        // subsample
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEAXES="
                                + axisPrefix
                                + "i(0.5),"
                                + axisPrefix
                                + "j(0.5)");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check we can read it as a TIFF and it is similare to the origina one
        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange().getSpan(0) / 2,
                    targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange().getSpan(1) / 2,
                    targetCoverage.getGridGeometry().getGridRange().getSpan(1));
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // upsample
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEAXES="
                                + axisPrefix
                                + "i(2),"
                                + axisPrefix
                                + "j(2)");

        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check we can read it as a TIFF and it is similare to the origina one
        readerTarget = new GeoTiffReader(file);
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange().getSpan(0) * 2,
                    targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange().getSpan(1) * 2,
                    targetCoverage.getGridGeometry().getGridRange().getSpan(1));
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // error 0
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEAXES="
                                + axisPrefix
                                + "i(0),"
                                + axisPrefix
                                + "j(0.5)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidScaleFactor.getExceptionCode(), "0.0");

        // error < 0
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEAXES="
                                + axisPrefix
                                + "i(-1),"
                                + axisPrefix
                                + "j(0.5)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidScaleFactor.getExceptionCode(), "-1.0");
    }

    @Test
    public void scaleToSize() throws Exception {

        // check we can read it as a TIFF and it is similar to the original one
        GeoTiffReader readerTarget = null;
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        try {
            // source
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);
            final Envelope2D sourceEnvelope = sourceCoverage.getEnvelope2D();

            // subsample
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff"
                                    + "&subset="
                                    + subsetPrefix
                                    + "Long("
                                    + sourceEnvelope.x
                                    + ","
                                    + (sourceEnvelope.x + sourceEnvelope.width / 2)
                                    + ")"
                                    + "&subset="
                                    + subsetPrefix
                                    + "Lat("
                                    + sourceEnvelope.y
                                    + ","
                                    + (sourceEnvelope.y + sourceEnvelope.height / 2)
                                    + ")"
                                    + "&SCALESIZE="
                                    + axisPrefix
                                    + "i(50),"
                                    + axisPrefix
                                    + "j(50)");

            assertEquals("image/tiff", response.getContentType());
            byte[] tiffContents = getBinary(response);
            File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
            FileUtils.writeByteArrayToFile(file, tiffContents);

            readerTarget = new GeoTiffReader(file);
            targetCoverage = readerTarget.read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            final GeneralEnvelope finalEnvelope =
                    new GeneralEnvelope(
                            new double[] {sourceEnvelope.x, sourceEnvelope.y},
                            new double[] {
                                sourceEnvelope.x + sourceEnvelope.width / 2,
                                sourceEnvelope.y + sourceEnvelope.height / 2
                            });
            finalEnvelope.setCoordinateReferenceSystem(
                    sourceCoverage.getCoordinateReferenceSystem());
            assertEquals(50, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(50, targetCoverage.getGridGeometry().getGridRange().getSpan(1));

            // get extrema (allow some difference as sub-sampling on read will be skipping input
            // pixels)
            assertEquals(
                    29.0, new ImageWorker(targetCoverage.getRenderedImage()).getMaximums()[0], 1);
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        try {

            // source
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);
            final Envelope2D sourceEnvelope = sourceCoverage.getEnvelope2D();

            // upsample
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALESIZE="
                                    + axisPrefix
                                    + "i(1000),"
                                    + axisPrefix
                                    + "j(1000)&"
                                    + "&subset="
                                    + subsetPrefix
                                    + "Long("
                                    + sourceEnvelope.x
                                    + ","
                                    + (sourceEnvelope.x + sourceEnvelope.width / 2)
                                    + ")"
                                    + "&subset="
                                    + subsetPrefix
                                    + "Lat("
                                    + sourceEnvelope.y
                                    + ","
                                    + (sourceEnvelope.y + sourceEnvelope.height / 2)
                                    + ")");

            assertEquals("image/tiff", response.getContentType());
            byte[] tiffContents = getBinary(response);
            File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
            FileUtils.writeByteArrayToFile(file, tiffContents);

            // check we can read it as a TIFF and it is similare to the origina one
            readerTarget = new GeoTiffReader(file);

            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(1));

            final GeneralEnvelope finalEnvelope =
                    new GeneralEnvelope(
                            new double[] {sourceEnvelope.x, sourceEnvelope.y},
                            new double[] {
                                sourceEnvelope.x + sourceEnvelope.width * 2,
                                sourceEnvelope.y + sourceEnvelope.height * 2
                            });
            finalEnvelope.setCoordinateReferenceSystem(
                    sourceCoverage.getCoordinateReferenceSystem());

            // get extrema
            assertEquals(29.0, new ImageWorker(targetCoverage.getRenderedImage()).getMaximums()[0]);
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // error 0
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALESIZE="
                                + axisPrefix
                                + "i(100),"
                                + axisPrefix
                                + "j(0)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "0");

        // error < 0
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALESIZE="
                                + axisPrefix
                                + "i(-2),"
                                + axisPrefix
                                + "j(100)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "-2");
    }

    @Test
    public void scaleToExtent() throws Exception {

        // subsample
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "i(0,99),"
                                + axisPrefix
                                + "j(0,99)");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check we can read it as a TIFF and it is similare to the origina one
        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // upsample
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "i(100,1099),"
                                + axisPrefix
                                + "j(100,1099)");

        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check we can read it as a TIFF and it is similare to the origina one
        readerTarget = new GeoTiffReader(file);
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(1));

            // geotiff encoding looses the min values and pushes to zero
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(0));
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(1));
            assertEquals(999, targetCoverage.getGridGeometry().getGridRange().getHigh(0));
            assertEquals(999, targetCoverage.getGridGeometry().getGridRange().getHigh(1));
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // test coverage directly to make sure we respect LLC & URC
        try {
            targetCoverage =
                    (GridCoverage2D)
                            executeGetCoverage(
                                    "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                            + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                            + axisPrefix
                                            + "i(100,1099),"
                                            + axisPrefix
                                            + "j(100,1099)");
            assertNotNull(targetCoverage);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            //            assertEquals(sourceCoverage.getCoordinateReferenceSystem(),
            // targetCoverage.getCoordinateReferenceSystem());
            //            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(1));

            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getLow(0));
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getLow(1));
            assertEquals(1099, targetCoverage.getGridGeometry().getGridRange().getHigh(0));
            assertEquals(1099, targetCoverage.getGridGeometry().getGridRange().getHigh(1));
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // error minx > maxx
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "i(1000,0),"
                                + axisPrefix
                                + "j(0,1000)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "0");

        // error minx ==  maxx
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "i(1000,1000),"
                                + axisPrefix
                                + "j(0,1000)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "1000");

        // error miny > maxy
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "j(1000,0),"
                                + axisPrefix
                                + "i(0,1000)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "0");

        // error miny ==  maxy
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "j(1000,1000),"
                                + axisPrefix
                                + "i(0,1000)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "1000");
    }

    @Test
    public void scaleToExtentWithTrim() throws Exception {

        GeoTiffReader readerTarget = null;
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        try {
            // source
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);
            final Envelope2D sourceEnvelope = sourceCoverage.getEnvelope2D();

            // subsample
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                    + axisPrefix
                                    + "i(50,149),"
                                    + axisPrefix
                                    + "j(50,149)"
                                    + "&subset="
                                    + subsetPrefix
                                    + "Long("
                                    + sourceEnvelope.x
                                    + ","
                                    + (sourceEnvelope.x + sourceEnvelope.width / 2)
                                    + ")"
                                    + "&subset="
                                    + subsetPrefix
                                    + "Lat("
                                    + sourceEnvelope.y
                                    + ","
                                    + (sourceEnvelope.y + sourceEnvelope.height / 2)
                                    + ")");

            assertEquals("image/tiff", response.getContentType());
            byte[] tiffContents = getBinary(response);
            File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
            FileUtils.writeByteArrayToFile(file, tiffContents);

            readerTarget = new GeoTiffReader(file);
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            final GeneralEnvelope finalEnvelope =
                    new GeneralEnvelope(
                            new double[] {sourceEnvelope.x, sourceEnvelope.y},
                            new double[] {
                                sourceEnvelope.x + sourceEnvelope.width / 2,
                                sourceEnvelope.y + sourceEnvelope.height / 2
                            });
            finalEnvelope.setCoordinateReferenceSystem(
                    sourceCoverage.getCoordinateReferenceSystem());
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getSpan(1));

            // get extrema
            assertEquals(29.0, new ImageWorker(targetCoverage.getRenderedImage()).getMaximums()[0]);
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        try {

            // source
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);
            final Envelope2D sourceEnvelope = sourceCoverage.getEnvelope2D();

            // upsample
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                    + axisPrefix
                                    + "i(100,1099),"
                                    + axisPrefix
                                    + "j(100,1099)"
                                    + "&subset="
                                    + subsetPrefix
                                    + "Long("
                                    + sourceEnvelope.x
                                    + ","
                                    + (sourceEnvelope.x + sourceEnvelope.width / 2)
                                    + ")"
                                    + "&subset="
                                    + subsetPrefix
                                    + "Lat("
                                    + sourceEnvelope.y
                                    + ","
                                    + (sourceEnvelope.y + sourceEnvelope.height / 2)
                                    + ")");

            assertEquals("image/tiff", response.getContentType());
            byte[] tiffContents = getBinary(response);
            File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
            FileUtils.writeByteArrayToFile(file, tiffContents);

            // check we can read it as a TIFF and it is similare to the origina one
            readerTarget = new GeoTiffReader(file);

            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            final GeneralEnvelope finalEnvelope =
                    new GeneralEnvelope(
                            new double[] {sourceEnvelope.x, sourceEnvelope.y},
                            new double[] {
                                sourceEnvelope.x + sourceEnvelope.width / 2,
                                sourceEnvelope.y + sourceEnvelope.height / 2
                            });
            finalEnvelope.setCoordinateReferenceSystem(
                    sourceCoverage.getCoordinateReferenceSystem());
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(1));

            // geotiff encoding looses the min values and pushes to zero
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(0));
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(1));
            assertEquals(999, targetCoverage.getGridGeometry().getGridRange().getHigh(0));
            assertEquals(999, targetCoverage.getGridGeometry().getGridRange().getHigh(1));

            // get extrema
            assertEquals(29.0, new ImageWorker(targetCoverage.getRenderedImage()).getMaximums()[0]);
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // test coverage directly to make sure we respect LLC & URC
        try {
            targetCoverage =
                    (GridCoverage2D)
                            executeGetCoverage(
                                    "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                            + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                            + axisPrefix
                                            + "i(100,1099),"
                                            + axisPrefix
                                            + "j(100,1099)");
            assertNotNull(targetCoverage);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            //            assertEquals(sourceCoverage.getCoordinateReferenceSystem(),
            // targetCoverage.getCoordinateReferenceSystem());
            //            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(1));

            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getLow(0));
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getLow(1));
            assertEquals(1099, targetCoverage.getGridGeometry().getGridRange().getHigh(0));
            assertEquals(1099, targetCoverage.getGridGeometry().getGridRange().getHigh(1));
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        // error minx > maxx
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "i(1000,0),"
                                + axisPrefix
                                + "j(0,1000)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "0");

        // error minx ==  maxx
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "i(1000,1000),"
                                + axisPrefix
                                + "j(0,1000)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "1000");

        // error miny > maxy
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "j(1000,0),"
                                + axisPrefix
                                + "i(0,1000)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "0");

        // error miny ==  maxy
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT="
                                + axisPrefix
                                + "j(1000,1000),"
                                + axisPrefix
                                + "i(0,1000)");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "1000");
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-8491 */
    @Test
    public void testConcurrentRequests() throws Exception {
        ExecutorService executor =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 8);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Future<Object> future =
                        executor.submit(
                                () -> {
                                    // subsample
                                    MockHttpServletResponse response =
                                            getAsServletResponse(
                                                    "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                                            + "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=0.5");

                                    assertEquals("image/tiff", response.getContentType());
                                    return null;
                                });
                futures.add(future);
            }
            // let it throw exceptions in case it fails
            for (Future<Object> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
