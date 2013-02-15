package org.geoserver.wcs2_0.kvp;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.junit.Assert;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class ScaleKvpTest extends WCSKVPTestSupport {

    @Test
    public void scaleFactor() throws Exception {
        
        // subsample
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=0.5");
        
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
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(sourceCoverage.getGridGeometry().getGridRange().getSpan(0)/2, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(sourceCoverage.getGridGeometry().getGridRange().getSpan(1)/2, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
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
        
        // upsample
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=2");
        
        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // check we can read it as a TIFF and it is similare to the origina one
        readerTarget = new GeoTiffReader(file);
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(sourceCoverage.getGridGeometry().getGridRange().getSpan(0)*2, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(sourceCoverage.getGridGeometry().getGridRange().getSpan(1)*2, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
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
        
        
        // error 0
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=0");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidScaleFactor.getExceptionCode(), "0.0");
        
        // error < 0
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEFACTOR=-12");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidScaleFactor.getExceptionCode(), "-12.0");
    }

    @Test
    public void scaleAxesByFactor() throws Exception {
        
        // subsample
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEAXES=http://www.opengis.net/def/axis/OGC/1/i(0.5)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(0.5)");
        
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
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(sourceCoverage.getGridGeometry().getGridRange().getSpan(0)/2, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(sourceCoverage.getGridGeometry().getGridRange().getSpan(1)/2, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
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
        
        // upsample
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEAXES=http://www.opengis.net/def/axis/OGC/1/i(2)," +
        "http://www.opengis.net/def/axis/OGC/1/j(2)");
        
        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // check we can read it as a TIFF and it is similare to the origina one
        readerTarget = new GeoTiffReader(file);
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(sourceCoverage.getGridGeometry().getGridRange().getSpan(0)*2, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(sourceCoverage.getGridGeometry().getGridRange().getSpan(1)*2, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
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
        
        
        // error 0
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEAXES=http://www.opengis.net/def/axis/OGC/1/i(0)," +
        "http://www.opengis.net/def/axis/OGC/1/j(0.5)");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidScaleFactor.getExceptionCode(), "0.0");
        
        // error < 0
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEAXES=http://www.opengis.net/def/axis/OGC/1/i(-1)," +
        "http://www.opengis.net/def/axis/OGC/1/j(0.5)");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidScaleFactor.getExceptionCode(), "-1.0");
    }
    
    @Test
    public void scaleSize() throws Exception {
        
        // subsample
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALESIZE=http://www.opengis.net/def/axis/OGC/1/i(100)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(100)");
        
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
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
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
        
        // upsample
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALESIZE=http://www.opengis.net/def/axis/OGC/1/i(1000)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(1000)");
        
        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // check we can read it as a TIFF and it is similare to the origina one
        readerTarget = new GeoTiffReader(file);
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
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
        
        
        // error 0
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALESIZE=http://www.opengis.net/def/axis/OGC/1/i(100)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(0)");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "0");
        
        // error < 0
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALESIZE=http://www.opengis.net/def/axis/OGC/1/i(-2)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(100)");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "-2");
    }
    
    @Test
    public void scaleToExtent() throws Exception {
        
        // subsample
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT=http://www.opengis.net/def/axis/OGC/1/i(0,99)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(0,99)");
        
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
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
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
        
        // upsample
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT=http://www.opengis.net/def/axis/OGC/1/i(100,1099)," +
                    "http://www.opengis.net/def/axis/OGC/1/j(100,1099)");
        
        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // check we can read it as a TIFF and it is similare to the origina one
        readerTarget = new GeoTiffReader(file);
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
            
            // geotiff encoding looses the min values and pushes to zero
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(0));
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(1));
            assertEquals(999, targetCoverage.getGridGeometry().getGridRange().getHigh(0));
            assertEquals(999, targetCoverage.getGridGeometry().getGridRange().getHigh(1));
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
        
        // test coverage directly to make sure we respect LLC & URC
        try {
            targetCoverage = (GridCoverage2D) executeGetCoverage("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                    "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT=http://www.opengis.net/def/axis/OGC/1/i(100,1099)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(100,1099)");
            Assert.assertNotNull(targetCoverage);            
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEnvelopeEquals(sourceCoverage, targetCoverage);
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(1000, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
            
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getLow(0));
            assertEquals(100, targetCoverage.getGridGeometry().getGridRange().getLow(1));
            assertEquals(1099, targetCoverage.getGridGeometry().getGridRange().getHigh(0));
            assertEquals(1099, targetCoverage.getGridGeometry().getGridRange().getHigh(1));
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
        
        // error minx > maxx
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT=http://www.opengis.net/def/axis/OGC/1/i(1000,0)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(0,1000)");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "0");
        
        // error minx ==  maxx
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT=http://www.opengis.net/def/axis/OGC/1/i(1000,1000)," +
                        "http://www.opengis.net/def/axis/OGC/1/j(0,1000)");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "1000");
        
        // error miny > maxy
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT=http://www.opengis.net/def/axis/OGC/1/j(1000,0)," +
                        "http://www.opengis.net/def/axis/OGC/1/i(0,1000)");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "0");
        
        // error miny ==  maxy
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&&Format=image/tiff&SCALEEXTENT=http://www.opengis.net/def/axis/OGC/1/j(1000,1000)," +
                        "http://www.opengis.net/def/axis/OGC/1/i(0,1000)");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WCS20ExceptionCode.InvalidExtent.getExceptionCode(), "1000");      
    }
    
}
