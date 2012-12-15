package org.geoserver.wcs2_0;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * Testing Scaling Extension 
 * 
 * @author Simone Giannecchini, GeoSolution SAS
 *
 */
public class ScalingExtentionTest extends WCSTestSupport {

    @Test
    public void testScaleAxesByFactorXML() throws Exception {
        final File xml= new File("./src/test/resources/requestGetCoverageScaleAxesByFactor.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        Assert.assertTrue(CRS.equalsIgnoreMetadata(reader.getCrs(), CRS.decode("EPSG:4326",true)));
        assertEquals(1260, reader.getOriginalGridRange().getSpan(0));
        assertEquals(1260, reader.getOriginalGridRange().getSpan(1));
        reader.dispose();        
    } 
    @Test
    public void testScaleToSizeXML() throws Exception {
        
        final File xml= new File("./src/test/resources/requestGetCoverageScaleToSize.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        Assert.assertTrue(CRS.equalsIgnoreMetadata(reader.getCrs(), CRS.decode("EPSG:4326",true)));
        assertEquals(1000, reader.getOriginalGridRange().getSpan(0));
        assertEquals(1000, reader.getOriginalGridRange().getSpan(1));
        reader.dispose();           
    }
        
    @Test
    public void testScaleToExtentXML() throws Exception {
        final File xml= new File("./src/test/resources/requestGetCoverageScaleToExtent.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        Assert.assertTrue(CRS.equalsIgnoreMetadata(reader.getCrs(), CRS.decode("EPSG:4326",true)));
        assertEquals(200, reader.getOriginalGridRange().getSpan(0));
        assertEquals(300, reader.getOriginalGridRange().getSpan(1));
        reader.dispose();         
    } 
    
    @Test
    public void testScaleByFactorXML() throws Exception {

        final File xml= new File("./src/test/resources/requestGetCoverageScaleByFactor.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        Assert.assertTrue(CRS.equalsIgnoreMetadata(reader.getCrs(), CRS.decode("EPSG:4326",true)));
        assertEquals(900, reader.getOriginalGridRange().getSpan(0));
        assertEquals(900, reader.getOriginalGridRange().getSpan(1));
        reader.dispose();   
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
