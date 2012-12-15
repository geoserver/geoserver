package org.geoserver.wcs2_0;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * Testing WCS 2.0 Core {@link GetCoverage}
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Emanuele Tajariol, GeoSolutions SAS
 *
 */
public class GetCoverageTest extends WCSTestSupport {

    @Test
    public void testGetMissingCoverage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=notThereBaby");

        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }
    
    /**
     * Trimming only on Longitude
     * 
     * @throws Exception
     */
    @Test
    public void testCoverageTrimmingLatitudeNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/requestGetCoverageTrimmingLatitudeNativeCRSXML.xml");
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
        final File xml= new File("./src/test/resources/requestGetCoverageTrimmingNativeCRSXML.xml");
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
    public void testGetFullCoverageKVP() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_full.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
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
    public void testGetFullCoverageXML() throws Exception {
        final File xml= new File("./src/test/resources/requestGetFullCoverage.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
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

    /**
     * Trimming only on Longitude
     * 
     * @throws Exception
     */
    @Test
    public void testCoverageTrimmingLongitudeNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/requestGetCoverageTrimmingLongNativeCRSXML.xml");
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
        final File xml= new File("./src/test/resources/requestGetCoverageTrimmingSlicingNativeCRSXML.xml");
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
        final File xml= new File("./src/test/resources/requestGetCoverageTrimmingDuplicatedNativeCRSXML.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("application/xml", response.getContentType());
//        checkOws20Exception(response, 404, "InvalidAxisLabel", "coverageId");
       
    }

    @Test
    public void testCoverageSlicingLongitudeNativeCRSXML() throws Exception {
        final File xml= new File("./src/test/resources/requestGetCoverageSlicingLongitudeNativeCRSXML.xml");
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
        final File xml= new File("./src/test/resources/requestGetCoverageSlicingLatitudeNativeCRSXML.xml");
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
