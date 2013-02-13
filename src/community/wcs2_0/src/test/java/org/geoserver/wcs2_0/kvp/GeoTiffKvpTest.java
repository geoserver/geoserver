package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import net.opengis.wcs20.GetCoverageType;

import org.apache.commons.io.FileUtils;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.coverage.CoverageUtilities;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GeoTiffKvpTest extends WCSKVPTestSupport {

    @Test
    public void extensionGeotiff() throws Exception {
        
        
        // complete
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
        		"&coverageId=theCoverage&compression=JPEG&jpeg_quality=75&predictor=None" +
        		"&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        Map<String, Object> extensions = getExtensionsMap(gc);
        
        assertEquals("JPEG", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:compression"));
        assertEquals("75", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:jpeg_quality"));
        assertEquals("None", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:predictor"));
        assertEquals("pixel", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:interleave"));
        assertEquals("true", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tiling"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tileheight"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tilewidth"));
    }
    
    
    @Test
    public void wrongJPEGQuality() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=-2&predictor=None" +
                        "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "-2");
        
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=101&predictor=None" +
                "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "101");
        
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=101&predictor=aaa" +
                "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.PredictorInvalid.toString(), "101");
    }
    
    @Test
    public void wrongCompression() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&compression=aaaG&predictor=None" +
                        "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.CompressionInvalid.toString(), "-2");
    }
    
    
    @Test
    public void getFullCoverageKVP() throws Exception {
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
    public void getFullCoverageLatLon() throws Exception {
        // impose latlon retaining
        final WCSInfo wcsInfo = getWCS();
        final boolean oldLatLon=wcsInfo.isLatLon();
        wcsInfo.setLatLon(true);
        getGeoServer().save(wcsInfo);
        
        // execute
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_full.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        final Hints hints= new Hints();
        hints.put(Hints.FORCE_AXIS_ORDER_HONORING, "EPSG");
        hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.FALSE);
        GeoTiffReader readerTarget = new GeoTiffReader(file,hints);
        GridCoverage2D targetCoverage = null, sourceCoverage=null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getGridGeometry().getGridRange(), targetCoverage.getGridGeometry().getGridRange());
            assertEquals(CRS.getAxisOrder(targetCoverage.getCoordinateReferenceSystem()),AxisOrder.NORTH_EAST);
            
            final GeneralEnvelope transformedEnvelope=CRS.transform((AffineTransform2D)CoverageUtilities.AXES_SWAP,targetCoverage.getEnvelope());
            transformedEnvelope.setCoordinateReferenceSystem(sourceCoverage.getCoordinateReferenceSystem());
            assertEquals(sourceCoverage.getEnvelope(), transformedEnvelope);
        } finally {
            // reinforce old settings
            wcsInfo.setLatLon(oldLatLon);
            getGeoServer().save(wcsInfo);
            
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

    
    
}
