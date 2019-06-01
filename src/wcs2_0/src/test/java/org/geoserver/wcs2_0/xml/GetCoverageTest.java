/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode.InvalidSubsetting;
import static org.junit.Assert.assertNotNull;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.xml.namespace.QName;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.Wcs20Factory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wcs2_0.DefaultWebCoverageService20;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Testing WCS 2.0 Core {@link GetCoverage}
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Emanuele Tajariol, GeoSolutions SAS
 */
public class GetCoverageTest extends WCSTestSupport {

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    protected static QName WATTEMP_DILATED =
            new QName(MockData.SF_URI, "watertemp_dilated", MockData.SF_PREFIX);

    protected static QName TIMERANGES =
            new QName(MockData.SF_URI, "timeranges", MockData.SF_PREFIX);

    protected static QName CUSTOMDIMS =
            new QName(MockData.SF_URI, "customdimensions", MockData.SF_PREFIX);

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);

    private static final QName BORDERS = new QName(MockData.SF_URI, "borders", MockData.SF_PREFIX);

    private static final QName SPATIO_TEMPORAL =
            new QName(MockData.SF_URI, "spatio-temporal", MockData.SF_PREFIX);

    @Before
    public void clearDimensions() {
        clearDimensions(getLayerId(WATTEMP));
        clearDimensions(getLayerId(TIMERANGES));
        clearDimensions(getLayerId(CUSTOMDIMS));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(
                WATTEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());
        GeoServerDataDirectory dataDirectory = getDataDirectory();
        Resource watertemp = dataDirectory.getResourceLoader().get("watertemp");
        File data = watertemp.dir();
        FilenameFilter groundElevationFilter =
                new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return name.matches(".*_000_.*tiff") || name.matches("watertemp\\..*");
                    }
                };
        for (File file : data.listFiles(groundElevationFilter)) {
            file.delete();
        }

        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(BORDERS, "/borders.zip", null, getCatalog());
        testData.addRasterLayer(
                TIMERANGES, "timeranges.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(
                CUSTOMDIMS, "customdimensions.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(
                SPATIO_TEMPORAL,
                "spatio-temporal.zip",
                null,
                null,
                SystemTestData.class,
                getCatalog());

        sortByElevation(TIMERANGES);
        sortByElevation(CUSTOMDIMS);

        testData.addRasterLayer(
                WATTEMP_DILATED,
                "/watertemp_dilated.zip",
                null,
                null,
                this.getClass(),
                getCatalog());
    }

    // force sorting on elevation to get predictable results
    private void sortByElevation(QName layer) {
        CoverageInfo coverage = getCatalog().getCoverageByName(getLayerId(layer));
        String sortByKey = ImageMosaicFormat.SORT_BY.getName().toString();
        coverage.getParameters().put(sortByKey, "elevation");
        getCatalog().save(coverage);
    }

    /** Trimming only on Longitude */
    @Test
    public void testCoverageTrimmingLatitudeNativeCRSXML() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingLatitudeNativeCRSXML.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        checkCoverageTrimmingLatitudeNativeCRS(tiffContents);
    }

    /** Trimming only on Longitude, plus multipart encoding */
    @Test
    public void testCoverageTrimmingLatitudeNativeCRSXMLMultipart() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/requestGetCoverageTrimmingLatitudeNativeCRSXMLMultipart.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("multipart/related", response.getContentType());

        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());
        BodyPart xmlPart = multipart.getBodyPart(0);
        assertEquals("application/gml+xml", xmlPart.getHeader("Content-Type")[0]);
        assertEquals("wcs", xmlPart.getHeader("Content-ID")[0]);
        Document gml = dom(xmlPart.getInputStream());
        // print(gml);

        // check the gml part refers to the file as its range
        XMLAssert.assertXpathEvaluatesTo(
                "fileReference", "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:arcrole", gml);
        XMLAssert.assertXpathEvaluatesTo(
                "cid:/coverages/wcs__BlueMarble.tif",
                "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:href",
                gml);
        XMLAssert.assertXpathEvaluatesTo(
                "http://www.opengis.net/spec/GMLCOV_geotiff-coverages/1.0/conf/geotiff-coverage",
                "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:role",
                gml);
        XMLAssert.assertXpathEvaluatesTo(
                "cid:/coverages/wcs__BlueMarble.tif",
                "//gml:rangeSet/gml:File/gml:fileReference",
                gml);
        XMLAssert.assertXpathEvaluatesTo("image/tiff", "//gml:rangeSet/gml:File/gml:mimeType", gml);

        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("/coverages/wcs__BlueMarble.tif", coveragePart.getHeader("Content-ID")[0]);
        assertEquals("image/tiff", coveragePart.getContentType());

        // make sure we can read the coverage back and perform checks on it
        byte[] tiffContents = IOUtils.toByteArray(coveragePart.getInputStream());
        checkCoverageTrimmingLatitudeNativeCRS(tiffContents);
    }

    private void checkCoverageTrimmingLatitudeNativeCRS(byte[] tiffContents)
            throws IOException, DataSourceException, NoSuchAuthorityCodeException,
                    FactoryException {
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {targetCoverage.getEnvelope().getMinimum(0), -43.5},
                            new double[] {targetCoverage.getEnvelope().getMaximum(0), -43.0});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(),
                            expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(0), 360);
            assertEquals(gridRange.getSpan(1), 120);

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
    public void testCoverageTrimmingNativeCRSXML() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingNativeCRSXML.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(new double[] {146.5, -43.5}, new double[] {147.0, -43.0});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(),
                            expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(0), 120);
            assertEquals(gridRange.getSpan(1), 120);

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
    public void testCoverageTrimmingBorders() throws Exception {
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTrimmingBorders.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        // make sure we are not getting a service exception
        assertEquals("image/tiff", response.getContentType());
    }

    @Test
    public void testCoverageTrimmingOutsideBorders() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingOutsideBorders.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        // make sure we are not getting a service exception
        checkOws20Exception(response, 404, "InvalidSubsetting", null);
    }

    @Test
    public void testGetFullCoverageXML() throws Exception {
        final File xml = new File("./src/test/resources/requestGetFullCoverage.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        // check the headers
        assertEquals("image/tiff", response.getContentType());
        String contentDisposition = response.getHeader("Content-disposition");
        assertEquals("inline; filename=wcs__BlueMarble.tif", contentDisposition);

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
                    sourceCoverage.getGridGeometry().getGridRange(),
                    targetCoverage.getGridGeometry().getGridRange());
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEquals(sourceCoverage.getEnvelope(), targetCoverage.getEnvelope());
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
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void testInputLimits() throws Exception {
        final File xml = new File("./src/test/resources/requestGetFullCoverage.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        // set limits
        setInputLimit(1);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        // System.out.println(new String(this.getBinary(response)));
        assertEquals("application/xml", response.getContentType());
        // reset imits
        setInputLimit(-1);
    }

    @Test
    public void testOutputLimits() throws Exception {
        final File xml = new File("./src/test/resources/requestGetFullCoverage.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8"); // set limits
        // set output limits
        setOutputLimit(1);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        // reset imits
        setOutputLimit(-1);
    }

    /** Trimming only on Longitude */
    @Test
    public void testCoverageTrimmingLongitudeNativeCRSXML() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingLongNativeCRSXML.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {146.5, targetCoverage.getEnvelope().getMinimum(1)},
                            new double[] {147.0, targetCoverage.getEnvelope().getMaximum(1)});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(),
                            expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(0), 120);
            assertEquals(gridRange.getSpan(1), 360);

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
    public void testCoverageTrimmingSlicingNativeCRSXML() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingSlicingNativeCRSXML.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            // 1 dimensional slice along latitude
            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {146.49999999999477, -43.5},
                            new double[] {146.99999999999477, -43.49583333333119});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(),
                            expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(1), 1);
            assertEquals(gridRange.getSpan(0), 120);

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
    public void testCoverageTrimmingDuplicatedNativeCRSXML() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingDuplicatedNativeCRSXML.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        //        checkOws20Exception(response, 404, "InvalidAxisLabel", "coverageId");

    }

    @Test
    public void testCoverageTrimmingBordersOverlap() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingBordersOverlap.xml");
        testCoverageResult(
                xml,
                targetCoverage -> {
                    final GeneralEnvelope expectedEnvelope =
                            new GeneralEnvelope(new double[] {7, 40}, new double[] {11, 43});
                    expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
                    double pixelSize = 0.057934032977228;
                    // check the whole extent has been returned
                    assertTrue(
                            expectedEnvelope.equals(
                                    targetCoverage.getEnvelope(), pixelSize, false));
                });
    }

    @Test
    public void testCoverageTrimmingBordersOverlapVertical() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingBordersOverlapVertical.xml");
        testCoverageResult(
                xml,
                targetCoverage -> {
                    final GeneralEnvelope expectedEnvelope =
                            new GeneralEnvelope(new double[] {13, 37}, new double[] {14, 39});
                    expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
                    double pixelSize = 0.057934032977228;
                    // check the whole extent has been returned
                    assertTrue(
                            expectedEnvelope.equals(
                                    targetCoverage.getEnvelope(), pixelSize, false));
                });
    }

    @Test
    public void testCoverageTrimmingBordersOverlapOutside() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTrimmingBordersOverlapOutside.xml");
        testCoverageResult(
                xml,
                targetCoverage -> {
                    // the expected envelope is the intersection between the requested and native
                    // one
                    final GeneralEnvelope expectedEnvelope =
                            new GeneralEnvelope(new double[] {6.344, 40}, new double[] {11, 46.59});
                    expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
                    double pixelSize = 0.057934032977228;
                    // check the whole extent has been returned
                    assertTrue(
                            expectedEnvelope.equals(
                                    targetCoverage.getEnvelope(), pixelSize, false));
                });
    }

    @FunctionalInterface
    public interface GridTester {
        void test(GridCoverage2D coverage) throws Exception;
    }

    void testCoverageResult(File xml, GridTester tester) throws Exception {
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file =
                File.createTempFile("borderOverlap", "borderOverlap.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            tester.test(targetCoverage);
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
    public void testCoverageSlicingLongitudeNativeCRSXML() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageSlicingLongitudeNativeCRSXML.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            // 1 dimensional slice along longitude
            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {146.5, -44.49999999999784},
                            new double[] {146.50416666666143, -42.99999999999787});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(),
                            expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(0), 1);
            assertEquals(gridRange.getSpan(1), 360);

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
    public void testCoverageSlicingLatitudeNativeCRSXML() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageSlicingLatitudeNativeCRSXML.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            // 1 dimensional slice along latitude
            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {146.49999999999477, -43.499999999997854},
                            new double[] {147.99999999999474, -43.49583333333119});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(),
                            expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(1), 1);
            assertEquals(gridRange.getSpan(0), 360);

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
    public void testCoverageTimeSlicingNoTimeConfigured() throws Exception {
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__watertemp");
        request = request.replace("${slicePoint}", "2000-10-31T00:00:00.000Z");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        checkOws20Exception(
                response,
                404,
                WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel.getExceptionCode(),
                null);
    }

    @Test
    public void testCoverageTimeSlicingTimeBefore() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__watertemp");
        request = request.replace("${slicePoint}", "2000-10-31T00:00:00.000Z");
        // nearest neighbor match, lowest time returned
        checkWaterTempValue(request, 14.89799975766800344);
    }

    @Test
    public void testCoverageTimeSlicingTimeFirst() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__watertemp");
        request = request.replace("${slicePoint}", "2008-10-31T00:00:00.000Z");
        checkWaterTempValue(request, 14.89799975766800344);
    }

    @Test
    public void testCoverageTimeSlicingTimeClosest() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__watertemp");
        request = request.replace("${slicePoint}", "2000-10-31T11:30:00.000Z");
        // nearest neighbor match, lowest time returned
        checkWaterTempValue(request, 14.89799975766800344);
    }

    @Test
    public void testCoverageTimeSlicingTimeSecond() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        // System.out.println(getDataDirectory().root());
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__watertemp");
        request = request.replace("${slicePoint}", "2008-11-01T00:00:00.000Z");
        checkWaterTempValue(request, 14.52999974018894136);
    }

    @Test
    public void testCoverageTimeSlicingTimeAfter() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__watertemp");
        request = request.replace("${slicePoint}", "2011-11-01T00:00:00.000Z");
        // nearest neighbor match, highest time returned
        checkWaterTempValue(request, 14.52999974018894136);
    }

    @Test
    public void testCoverageTimeSlicingAgainstFirstRange() throws Exception {
        setupRasterDimension(
                getLayerId(TIMERANGES), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(TIMERANGES), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${slicePoint}", "2008-10-31T00:00:00.000Z");
        // timeranges is really just an expanded watertemp
        checkWaterTempValue(request, 18.478999927756377);
    }

    @Test
    public void testCoverageTimeSlicingAgainstRangeHole() throws Exception {
        setupRasterDimension(
                getLayerId(TIMERANGES), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(TIMERANGES), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${slicePoint}", "2008-11-04T11:00:00.000Z");
        // timeranges is really just an expanded watertemp, and we expect NN
        checkWaterTempValue(request, 14.52999974018894136);
    }

    @Test
    public void testCoverageTimeSlicingAgainstSecondRange() throws Exception {
        setupRasterDimension(
                getLayerId(TIMERANGES), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(TIMERANGES), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageTimeSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${slicePoint}", "2008-11-06T00:00:00.000Z");
        // timeranges is really just an expanded watertemp
        checkWaterTempValue(request, 14.52999974018894136);
    }

    @Test
    public void testCoverageTimeElevationSlicingAgainstLowestOldestGranule() throws Exception {
        setupTimeRangesTimeElevationCustom(
                TIMERANGES, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTimeElevationCustomSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${slicePointElevation}", "20");
        request = request.replace("${slicePointTime}", "2008-10-31T00:00:00.000Z");
        request = request.replace("${Custom}", "WAVELENGTH");
        request = request.replace("${slicePointCustom}", "20");
        // timeranges is really just an expanded watertemp
        checkWaterTempValue(request, 18.478999927756377);

        request = request.replace("WAVELENGTH", "wavelength");
        checkWaterTempValue(request, 18.478999927756377);
    }

    @Test
    public void testCoverageTimeElevationSlicingAgainstHighestNewestGranuleLatestWavelength()
            throws Exception {
        setupTimeRangesTimeElevationCustom(
                TIMERANGES, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTimeElevationCustomSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${slicePointElevation}", "140");
        request = request.replace("${slicePointTime}", "2008-11-07T00:00:00.000Z");
        request = request.replace("${Custom}", "WAVELENGTH");
        request = request.replace("${slicePointCustom}", "80");
        // timeranges is really just an expanded watertemp
        checkWaterTempValue(request, 14.52999974018894136);

        request = request.replace("WAVELENGTH", "wavelength");
        checkWaterTempValue(request, 14.52999974018894136);
    }

    @Test
    public void testCoverageMultipleCustomSubsets() throws Exception {
        setupTimeRangesTimeElevationCustom(
                CUSTOMDIMS, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        setupRasterDimension(
                getLayerId(CUSTOMDIMS),
                ResourceInfo.CUSTOM_DIMENSION_PREFIX + "CUSTOM",
                DimensionPresentation.LIST,
                null);
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageMultipleCustomSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__customdimensions");
        request = request.replace("${slicePointElevation}", "140");
        request = request.replace("${slicePointTime}", "2008-11-07T00:00:00.000Z");
        request = request.replace("${CustomOne}", "WAVELENGTH");
        request = request.replace("${slicePointCustomOne}", "80");
        request = request.replace("${CustomTwo}", "CUSTOM");
        request = request.replace("${slicePointCustomTwo}", "99");
        // timeranges is really just an expanded watertemp
        checkWaterTempValue(request, 14.52999974018894136);

        request = request.replace("WAVELENGTH", "wavelength").replace("CUSTOM", "custom");
        checkWaterTempValue(request, 14.52999974018894136);
    }

    @Test
    public void testCoverageTimeElevationSlicingAgainstHighestNewestGranule() throws Exception {
        setupTimeRangesTimeElevationCustom(
                TIMERANGES, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTimeElevationSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${slicePointElevation}", "140");
        request = request.replace("${slicePointTime}", "2008-11-07T00:00:00.000Z");
        // timeranges is really just an expanded watertemp
        checkWaterTempValue(request, 14.52999974018894136);
    }

    @Test
    public void testCoverageElevationSlicingDefaultTime() throws Exception {
        setupTimeRangesTimeElevationCustom(
                TIMERANGES, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        final File xml =
                new File("./src/test/resources/trimming/requestGetCoverageElevationSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${slicePoint}", "140");

        // check we get a proper exception
        checkWaterTempValue(request, 14.52999974018894136);
    }

    private void setupTimeRangesTimeElevationCustom(
            QName timeranges, String time, String elevation, String customDimensionName) {
        setupRasterDimension(getLayerId(timeranges), time, DimensionPresentation.LIST, null);
        setupRasterDimension(getLayerId(timeranges), elevation, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(timeranges),
                ResourceInfo.CUSTOM_DIMENSION_PREFIX + customDimensionName,
                DimensionPresentation.LIST,
                null);
    }

    private void checkWaterTempValue(String request, double expectedValue)
            throws Exception, IOException, DataSourceException {
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // checks spatial consistency
            GridCoverage2DReader sourceReader =
                    (GridCoverage2DReader)
                            getCatalog()
                                    .getCoverageByName(getLayerId(WATTEMP))
                                    .getGridCoverageReader(null, null);
            GeneralEnvelope expectedEnvelope = sourceReader.getOriginalEnvelope();
            assertEnvelopeEquals(
                    expectedEnvelope, 1.0, (GeneralEnvelope) targetCoverage.getEnvelope(), 1.0);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(),
                            expectedEnvelope.getCoordinateReferenceSystem()));

            // check raster space consistency
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
            GridEnvelope expectedGridRange = sourceReader.getOriginalGridRange();
            assertEquals(gridRange.getSpan(0), expectedGridRange.getSpan(0));
            assertEquals(gridRange.getSpan(1), expectedGridRange.getSpan(1));

            // check the reference pixel
            double[] pixel = new double[1];
            targetCoverage.getRenderedImage().getData().getPixel(1, 24, pixel);
            assertEquals(expectedValue, pixel[0], 1e-6);
        } finally {
            readerTarget.dispose();
            scheduleForCleaning(targetCoverage);
        }
    }

    @Test
    public void testDatelineCrossingMinGreaterThanMax() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageAcrossDateline.xml");
        checkDatelineCrossing(xml);
    }

    @Test
    public void testDatelineCrossingPositiveCoordinates() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageAcrossDateline2.xml");
        checkDatelineCrossing(xml);
    }

    private void checkDatelineCrossing(final File xml)
            throws IOException, Exception, DataSourceException {
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("rain_gtiff", "rain_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // check we got the right envelope
            Envelope2D envelope = targetCoverage.getEnvelope2D();
            assertEquals(160, envelope.getMinX(), 0d);
            assertEquals(0, envelope.getMinY(), 0d);
            assertEquals(200, envelope.getMaxX(), 0d);
            assertEquals(40, envelope.getMaxY(), 0d);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            DefaultGeographicCRS.WGS84,
                            targetCoverage.getCoordinateReferenceSystem2D()));

            // check we actually read the right stuff. For this case, we
            // just check we have the pixels in the range of values of that area
            Raster data = targetCoverage.getRenderedImage().getData();
            double[] pixel = new double[1];
            for (int i = data.getMinY(); i < data.getMinY() + data.getHeight(); i++) {
                for (int j = data.getMinX(); j < data.getMinX() + data.getWidth(); j++) {
                    data.getPixel(i, j, pixel);
                    double d = pixel[0];
                    assertTrue(String.valueOf(d), d > 500 && d < 5500);
                }
            }
        } finally {
            readerTarget.dispose();
            scheduleForCleaning(targetCoverage);
        }
    }

    @Test
    public void testDatelineCrossingPolar() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageAcrossDatelinePolar.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("polar_gtiff", "polar_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // check we got the right envelope
            Envelope2D envelope = targetCoverage.getEnvelope2D();
            // System.out.println(envelope);
            assertEquals(-1139998, envelope.getMinX(), 1d);
            assertEquals(-3333134, envelope.getMinY(), 1d);
            assertEquals(1139998, envelope.getMaxX(), 1d);
            assertEquals(-1023493, envelope.getMaxY(), 1d);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            CRS.decode("EPSG:3031", true),
                            targetCoverage.getCoordinateReferenceSystem2D()));

            // we don't check the values, as we don't have the smarts available in the
            // rendering subsystem to read a larger area also when the
            // reprojection makes the pixel shrink
        } finally {
            readerTarget.dispose();
            scheduleForCleaning(targetCoverage);
        }
    }

    @Test
    public void testDatelineCrossingMercatorPDC() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/requestGetCoverageAcrossDatelineMercatorPacific.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("polar_gtiff", "polar_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // check we got the right envelope
            Envelope2D envelope = targetCoverage.getEnvelope2D();
            assertEquals(160, envelope.getMinX(), 0d);
            assertEquals(0, envelope.getMinY(), 0d);
            assertEquals(200, envelope.getMaxX(), 0d);
            assertEquals(40, envelope.getMaxY(), 0d);
            assertTrue(
                    CRS.equalsIgnoreMetadata(
                            DefaultGeographicCRS.WGS84,
                            targetCoverage.getCoordinateReferenceSystem2D()));

            // check we actually read the right stuff. For this case, we
            // just check we have the pixels in the range of values of that area
            RenderedImage renderedImage = targetCoverage.getRenderedImage();
            Raster data = renderedImage.getData();
            double[] pixel = new double[1];
            for (int i = data.getMinY(); i < data.getMinY() + data.getHeight(); i++) {
                for (int j = data.getMinX(); j < data.getMinX() + data.getWidth(); j++) {
                    data.getPixel(i, j, pixel);
                    double d = pixel[0];
                    assertTrue(String.valueOf(d), d > 500 && d < 5500);
                }
            }
        } finally {
            readerTarget.dispose();
            scheduleForCleaning(targetCoverage);
        }
    }

    @Test
    public void testDeferredLoading() throws Exception {
        DefaultWebCoverageService20 wcs =
                GeoServerExtensions.bean(DefaultWebCoverageService20.class);
        GetCoverageType getCoverage = Wcs20Factory.eINSTANCE.createGetCoverageType();
        getCoverage.setCoverageId(getLayerId(SPATIO_TEMPORAL));
        getCoverage.setVersion("2.0.0");
        getCoverage.setService("WCS");
        GridCoverage coverage = null;
        try {
            coverage = wcs.getCoverage(getCoverage);
            assertNotNull(coverage);

            assertDeferredLoading(coverage.getRenderedImage());
        } finally {
            scheduleForCleaning(coverage);
        }
    }

    @Test
    public void testInvalidElevationTrimmingOutsideRange() throws Exception {
        setupTimeRangesTimeElevationCustom(
                TIMERANGES, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageDimensionTrimmingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${dimension}", "elevation");
        request = request.replace("${trimLow}", "-500");
        request = request.replace("${trimHigh}", "-400");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        String errorMessage =
                checkOws20Exception(response, 404, InvalidSubsetting.getExceptionCode(), "subset");
        assertEquals(
                "Requested elevation subset does not intersect the declared range 20.0/150.0",
                errorMessage);
    }

    @Test
    public void testInvalidElevationTrimmingInsideRange() throws Exception {
        setupTimeRangesTimeElevationCustom(
                TIMERANGES, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageDimensionTrimmingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${dimension}", "elevation");
        request = request.replace("${trimLow}", "99.5"); //
        request = request.replace("${trimHigh}", "99.7");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        String errorMessage =
                checkOws20Exception(response, 404, InvalidSubsetting.getExceptionCode(), "subset");
        assertEquals(
                "Requested elevation subset does not intersect available values [[20.0, 99.0], [100.0, 150.0]]",
                errorMessage);
    }

    @Test
    public void testInvalidTimeTrimmingOutsideRange() throws Exception {
        setupTimeRangesTimeElevationCustom(
                TIMERANGES, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageDimensionTrimmingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${dimension}", "time");
        request = request.replace("${trimLow}", "1990-11-01T00:00:00.000Z");
        request = request.replace("${trimHigh}", "1991-11-01T00:00:00.000Z");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        String errorMessage =
                checkOws20Exception(response, 404, InvalidSubsetting.getExceptionCode(), "subset");
        assertEquals(
                "Requested time subset does not intersect the declared range 2008-10-31T00:00:00.000Z/2008-11-07T00:00:00.000Z",
                errorMessage);
    }

    @Test
    public void testInvalidTimeTrimmingInsideRange() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP_DILATED), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(WATTEMP_DILATED),
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null);
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageDimensionTrimmingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__watertemp_dilated");
        request = request.replace("${dimension}", "time");
        request = request.replace("${trimLow}", "2008-11-01T11:00:00.000Z");
        request = request.replace("${trimHigh}", "2008-11-01T12:01:00.000Z");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        String errorMessage =
                checkOws20Exception(response, 404, InvalidSubsetting.getExceptionCode(), "subset");
        assertEquals(
                "Requested time subset does not intersect available values [2008-10-31T00:00:00.000Z, 2008-11-03T00:00:00.000Z]",
                errorMessage);
    }

    @Test
    public void testInvalidCustomDimensionSlicing() throws Exception {
        setupTimeRangesTimeElevationCustom(
                TIMERANGES, ResourceInfo.TIME, ResourceInfo.ELEVATION, "WAVELENGTH");
        final File xml =
                new File(
                        "./src/test/resources/trimming/requestGetCoverageTimeElevationCustomSlicingXML.xml");
        String request = FileUtils.readFileToString(xml, "UTF-8");
        request = request.replace("${coverageId}", "sf__timeranges");
        request = request.replace("${slicePointElevation}", "20");
        request = request.replace("${slicePointTime}", "2008-10-31T00:00:00.000Z");
        request = request.replace("${Custom}", "WAVELENGTH");
        request = request.replace("${slicePointCustom}", "-300");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        String errorMessage =
                checkOws20Exception(response, 404, InvalidSubsetting.getExceptionCode(), "subset");
        assertEquals(
                "Requested WAVELENGTH subset does not intersect the available values [12/24, 25/80]",
                errorMessage);

        request = request.replace("WAVELENGTH", "wavelength");
        response = postAsServletResponse("wcs", request);
        errorMessage =
                checkOws20Exception(response, 404, InvalidSubsetting.getExceptionCode(), "subset");
        assertEquals(
                "Requested WAVELENGTH subset does not intersect the available values [12/24, 25/80]",
                errorMessage);
    }
}
