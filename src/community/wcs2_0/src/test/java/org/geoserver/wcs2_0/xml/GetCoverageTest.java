package org.geoserver.wcs2_0.xml;

import static junit.framework.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.Multipart;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * Testing WCS 2.0 Core {@link GetCoverage}
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Emanuele Tajariol, GeoSolutions SAS
 *
 */
public class GetCoverageTest extends WCSTestSupport {

    /**
     * Trimming only on Longitude
     * 
     * @throws Exception
     */
    @Test
    public void testCoverageTrimmingLatitudeNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/trimming/requestGetCoverageTrimmingLatitudeNativeCRSXML.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        checkCoverageTrimmingLatitudeNativeCRS(tiffContents);
    }
    
    /**
     * Trimming only on Longitude, plus multipart encoding
     * 
     * @throws Exception
     */
    @Test
    public void testCoverageTrimmingLatitudeNativeCRSXMLMultipart() throws Exception {
        final File xml= new File("./src/test/resources/requestGetCoverageTrimmingLatitudeNativeCRSXMLMultipart.xml");
        final String request= FileUtils.readFileToString(xml);
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
        assertXpathEvaluatesTo("fileReference", "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:arcrole", gml);
        assertXpathEvaluatesTo("cid:/coverages/wcs__BlueMarble.tif", "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:href", gml);
        assertXpathEvaluatesTo("http://www.opengis.net/spec/GMLCOV_geotiff-coverages/1.0/conf/geotiff-coverage", "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:role", gml);
        assertXpathEvaluatesTo("cid:/coverages/wcs__BlueMarble.tif", "//gml:rangeSet/gml:File/gml:fileReference", gml);
        assertXpathEvaluatesTo("image/tiff", "//gml:rangeSet/gml:File/gml:mimeType", gml);
        
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("/coverages/wcs__BlueMarble.tif", coveragePart.getHeader("Content-ID")[0]);
        assertEquals("image/tiff", coveragePart.getContentType());

        // make sure we can read the coverage back and perform checks on it
        byte[] tiffContents = IOUtils.toByteArray(coveragePart.getInputStream());
        checkCoverageTrimmingLatitudeNativeCRS(tiffContents);
    }

    private void checkCoverageTrimmingLatitudeNativeCRS(byte[] tiffContents) throws IOException,
            DataSourceException, NoSuchAuthorityCodeException, FactoryException {
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();
            
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{targetCoverage.getEnvelope().getMinimum(0),-43.5},
                    new double[]{targetCoverage.getEnvelope().getMaximum(0),-43.0});    
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(0), 360);
            assertEquals(gridRange.getSpan(1), 120);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
    
    @Test
    public void testCoverageTrimmingNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/trimming/requestGetCoverageTrimmingNativeCRSXML.xml");
        final String request= FileUtils.readFileToString(xml);
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
            
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{146.5,-43.5},
                    new double[]{147.0,-43.0});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(0), 120);
            assertEquals(gridRange.getSpan(1), 120);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
    
    @Test
    public void testGetFullCoverageXML() throws Exception {
        final File xml= new File("./src/test/resources/requestGetFullCoverage.xml");
        final String request= FileUtils.readFileToString(xml);
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
        GridCoverage2D targetCoverage = null, sourceCoverage=null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getGridGeometry().getGridRange(), targetCoverage.getGridGeometry().getGridRange());
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEquals(sourceCoverage.getEnvelope(), targetCoverage.getEnvelope());
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
    @Test
    public void testInputLimits() throws Exception {
        final File xml= new File("./src/test/resources/requestGetFullCoverage.xml");
        final String request= FileUtils.readFileToString(xml);
        // set limits
        setInputLimit(1);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        System.out.println(new String(this.getBinary(response)));
        assertEquals("application/xml", response.getContentType());
        // reset imits
        setInputLimit(-1);
       
    }
    @Test
    public void testOutputLimits() throws Exception {
        final File xml= new File("./src/test/resources/requestGetFullCoverage.xml");
        final String request= FileUtils.readFileToString(xml);        // set limits
        // set output limits
        setOutputLimit(1);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("application/xml", response.getContentType());
        // reset imits
        setOutputLimit(-1);
    }

    /**
     * Trimming only on Longitude
     * 
     * @throws Exception
     */
    @Test
    public void testCoverageTrimmingLongitudeNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/trimming/requestGetCoverageTrimmingLongNativeCRSXML.xml");
        final String request= FileUtils.readFileToString(xml);
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
            
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{146.5,targetCoverage.getEnvelope().getMinimum(1)},
                    new double[]{147.0,targetCoverage.getEnvelope().getMaximum(1)});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
    
            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(0), 120);
            assertEquals(gridRange.getSpan(1), 360);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
    @Test
    public void testCoverageTrimmingSlicingNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/trimming/requestGetCoverageTrimmingSlicingNativeCRSXML.xml");
        final String request= FileUtils.readFileToString(xml);
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
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{146.49999999999477,-43.5},
                    new double[]{146.99999999999477,-43.49583333333119});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
    
            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(1), 1);
            assertEquals(gridRange.getSpan(0), 120);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
    
    @Test
    public void testCoverageTrimmingDuplicatedNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/trimming/requestGetCoverageTrimmingDuplicatedNativeCRSXML.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("application/xml", response.getContentType());
//        checkOws20Exception(response, 404, "InvalidAxisLabel", "coverageId");
       
    }

    @Test
    public void testCoverageSlicingLongitudeNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/trimming/requestGetCoverageSlicingLongitudeNativeCRSXML.xml");
        final String request= FileUtils.readFileToString(xml);
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
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{146.5,-44.49999999999784},
                    new double[]{146.50416666666143,-42.99999999999787});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
    
            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(0), 1);
            assertEquals(gridRange.getSpan(1), 360);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void testCoverageSlicingLatitudeNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/trimming/requestGetCoverageSlicingLatitudeNativeCRSXML.xml");
        final String request= FileUtils.readFileToString(xml);
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
            final GeneralEnvelope expectedEnvelope= new GeneralEnvelope(
                    new double[]{146.49999999999477,-43.499999999997854},
                    new double[]{147.99999999999474,-43.49583333333119});
            expectedEnvelope.setCoordinateReferenceSystem(CRS.decode("EPSG:4326", true));
    
            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(expectedEnvelope,scale,(GeneralEnvelope) targetCoverage.getEnvelope(),scale);
            Assert.assertTrue(CRS.equalsIgnoreMetadata(targetCoverage.getCoordinateReferenceSystem(), expectedEnvelope.getCoordinateReferenceSystem()));
            assertEquals(gridRange.getSpan(1), 1);
            assertEquals(gridRange.getSpan(0), 360);
            
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}
