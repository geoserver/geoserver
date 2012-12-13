package org.geoserver.wcs2_0;

import static junit.framework.Assert.assertEquals;

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
        
        assertEquals("image/tiff", response.getContentType());
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
    
    // TODO: add tests for range subsetting
//    <?xml version="1.0" encoding="UTF-8"?>
//    <wcs:GetCoverage xmlns:wcs="http://www.opengis.net/wcs/2.0"
//        xmlns:gml="http://www.opengis.net/gml/3.2"
//        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
//        xmlns:rsub="http://www.opengis.net/wcs/range-subsetting/1.0"
//        service="WCS" version="2.0.1">
//        <wcs:CoverageId>C0001</wcs:CoverageId>
//        <wcs:Extension>    
//            <rsub:rangeSubset>
//                <rsub:rangeItem>
//                    <rsub:rangeComponent>band1</rsub:rangeComponent>
//                </rsub:rangeItem>    
//                <rsub:rangeItem>        
//                    <rsub:rangeInterval>
//                        <rsub:startComponent>band3</rsub:startComponent>
//                        <rsub:endComponent>band5</rsub:endComponent>
//                    </rsub:rangeInterval>
//                </rsub:rangeItem>        
//            </rsub:rangeSubset>
//        </wcs:Extension>
//    </wcs:GetCoverage>

}
