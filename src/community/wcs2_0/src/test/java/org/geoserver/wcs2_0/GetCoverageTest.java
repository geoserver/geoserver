package org.geoserver.wcs2_0;

import static junit.framework.Assert.*;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetCoverageTest extends WCSTestSupport {

    @Test
    public void testGetMissingCoverage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=notThereBaby");

        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }
    
    @Test
    public void testGetFullCoverage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");
        
        assertEquals("image/tiff;subtype=\"geotiff\"", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_full.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // check we can read it as a TIFF
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = null;
        try {
            coverage = reader.read(null);
        } finally {
            reader.dispose();
            scheduleForCleaning(coverage);
        }
        
        // TODO: add more checks, make sure we returned the whole thing
    }
    
    @Test 
    public void testGeotiffExtension() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        		"<wcs:GetCoverage\n" + 
        		"  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
        		"  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
        		"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
        		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
        		"  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
        		"  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
        		"  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
        		"  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
        		"  service=\"WCS\"\n" + 
        		"  version=\"2.0.1\">\n" + 
        		"  <wcs:Extension>\n" + 
        		"    <wcsgeotiff:compression>JPEG</wcsgeotiff:compression>\n" + 
        		"    <wcsgeotiff:jpeg_quality>75</wcsgeotiff:jpeg_quality>\n" + 
        		"    <wcsgeotiff:predictor>None</wcsgeotiff:predictor>\n" + 
        		"    <wcsgeotiff:interleave>pixel</wcsgeotiff:interleave>\n" + 
        		"    <wcsgeotiff:tiling>true</wcsgeotiff:tiling>\n" + 
        		"    <wcsgeotiff:tileheight>256</wcsgeotiff:tileheight>\n" + 
        		"    <wcsgeotiff:tilewidth>256</wcsgeotiff:tilewidth>\n" + 
        		"  </wcs:Extension>\n" + 
        		"  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
        		"  <wcs:format>image/tiff</wcs:format>\n" + 
        		"</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_gtiff.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // TODO: check the tiff structure is the one requested
    }


    

    // TODO: re-enable when we have subsetting support in GetCoverage
    // @Test
    // public void testBBoxRequest() throws Exception {
    // Document dom = getAsDOM("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=" +
    // getLayerId(TASMANIA_BM) + "&subset=lon(-10,10)&subset=lat(-20,20)");
    // print(dom);
    //
    // // checkFullCapabilitiesDocument(dom);
    // }

}
