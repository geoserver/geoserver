package org.geoserver.wcs2_0;

import static junit.framework.Assert.assertEquals;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.io.File;

import javax.imageio.stream.FileImageInputStream;

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
    public void testReprojectXML() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<wcs:GetCoverage\n"
                + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                + "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n"
                + "  xmlns:crs=\"http://www.opengis.net/wcs/crs/1.0\"\n"                
                + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n"
                + "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n"
                + "  http://www.opengis.net/wcs/geotiff/1.0 \n"
                + "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n"
                + "  service=\"WCS\"\n" 
                + "  version=\"2.0.1\">\n" 
                + "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n"
                + "  <wcs:DimensionTrim>\n"
                + "    <wcs:Dimension>Long</wcs:Dimension>\n"
                + "    <wcs:TrimLow>146.5</wcs:TrimLow>\n"
                + "    <wcs:TrimHigh>147.0</wcs:TrimHigh>\n"
                + "  </wcs:DimensionTrim>\n"
                + "  <wcs:DimensionTrim>\n"
                + "    <wcs:Dimension>Lat</wcs:Dimension>\n"
                + "    <wcs:TrimLow>-43.5</wcs:TrimLow>\n"
                + "    <wcs:TrimHigh>-43.0</wcs:TrimHigh>\n"
                + "  </wcs:DimensionTrim>\n"
                + "  <wcs:Extension>\n"                   
                + "    <crs:outputCrs>"
                + "      <crs:outputCrs>http://www.opengis.net/def/crs/EPSG/0/4326</crs:outputCrs>"
                + "    </crs:outputCrs>\n"
                + "  </wcs:Extension>\n"                   
                + "  <wcs:format>image/tiff</wcs:format>\n" 
                + "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi()
                .createReaderInstance();
        reader.setInput(new FileImageInputStream(file));
    }
    
    @Test
    public void testTrimmingCoverageXML() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<wcs:GetCoverage\n"
                + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                + "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n"
                + "  xmlns:crs=\"http://www.opengis.net/wcs/crs/1.0\"\n"                
                + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n"
                + "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n"
                + "  http://www.opengis.net/wcs/geotiff/1.0 \n"
                + "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n"
                + "  service=\"WCS\"\n" 
                + "  version=\"2.0.1\">\n" 
                + "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n"
                + "  <wcs:DimensionTrim>\n"
                + "    <wcs:Dimension>Long</wcs:Dimension>\n"
                + "    <wcs:TrimLow>146.5</wcs:TrimLow>\n"
                + "    <wcs:TrimHigh>147.0</wcs:TrimHigh>\n"
                + "  </wcs:DimensionTrim>\n"
                + "  <wcs:DimensionTrim>\n"
                + "    <wcs:Dimension>Lat</wcs:Dimension>\n"
                + "    <wcs:TrimLow>-43.5</wcs:TrimLow>\n"
                + "    <wcs:TrimHigh>-43.0</wcs:TrimHigh>\n"
                + "  </wcs:DimensionTrim>\n"
                + "  <wcs:Extension>\n"                   
                + "    <crs:subsettingCrs>"
                + "      <crs:subsettingCrs>http://www.opengis.net/def/crs/EPSG/0/4326</crs:subsettingCrs>"
                + "    </crs:subsettingCrs>\n"
                + "  </wcs:Extension>\n"                   
                + "  <wcs:format>image/tiff</wcs:format>\n" 
                + "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi()
                .createReaderInstance();
        reader.setInput(new FileImageInputStream(file));
    }
    
    @Test
    public void testScaleFactorIndividualXML() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<wcs:GetCoverage\n"
                + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                + "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n"
                + "  xmlns:scal=\"http://www.opengis.net/wcs/scaling/1.0\"\n"                
                + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n"
                + "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n"
                + "  http://www.opengis.net/wcs/geotiff/1.0 \n"
                + "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n"
                + "  service=\"WCS\"\n" 
                + "  version=\"2.0.1\">\n" 
                + "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n"
                + "  <wcs:Extension>\n"                   
                + "    <scal:ScaleAxesByFactor>"
                + "    <scal:ScaleAxis>"
                + "      <scal:axis>"
                + "        http://www.opengis.net/def/axis/OGC/1/i"
                + "      </scal:axis>"
                + "      <scal:scaleFactor>3.5</scal:scaleFactor>"
                + "    </scal:ScaleAxis>"
                + "    <scal:ScaleAxis>"
                + "     <scal:axis>"
                + "        http://www.opengis.net/def/axis/OGC/1/j"
                + "      </scal:axis>"
                + "      <scal:scaleFactor>3.5</scal:scaleFactor>"
                + "    </scal:ScaleAxis>"
                + "    <scal:ScaleAxis>"
                + "      <scal:axis>"
                + "        http://www.opengis.net/def/axis/OGC/1/k"
                + "      </scal:axis>"
                + "      <scal:scaleFactor>2.0</scal:scaleFactor>"
                + "    </scal:ScaleAxis>"
                + "   </scal:ScaleAxesByFactor>"
                + "  </wcs:Extension>\n"                
                + "  <wcs:format>image/tiff</wcs:format>\n"    
                + "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi()
                .createReaderInstance();
        reader.setInput(new FileImageInputStream(file));
    } 
    @Test
    public void testScaleSizeIndividualXML() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<wcs:GetCoverage\n"
                + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                + "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n"
                + "  xmlns:scal=\"http://www.opengis.net/wcs/scaling/1.0\"\n"                
                + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n"
                + "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n"
                + "  http://www.opengis.net/wcs/geotiff/1.0 \n"
                + "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n"
                + "  service=\"WCS\"\n" 
                + "  version=\"2.0.1\">\n" 
                + "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n"
                + "  <wcs:Extension>\n"                   
                + "  <scal:ScaleToSize>\n"
                + "    <scal:TargetAxisSize>\n"
                + "      <scal:axis>\n"
                + "        http://www.opengis.net/def/axis/OGC/1/i\n"
                + "      </scal:axis>\n"
                + "      <scal:low>10</scal:low>\n"
                + "      <scal:high>20</scal:high>\n"
                + "    </scal:TargetAxisSize>\n"
                + "    <scal:TargetAxisSize>\n"
                + "      <scal:axis>\n"
                + "        http://www.opengis.net/def/axis/OGC/1/j\n"
                + "      </scal:axis>\n"
                + "      <scal:low>20</scal:low>\n"
                + "      <scal:high>30</scal:high>\n"
                + "    </scal:TargetAxisSize>\n"
                + "  </scal:ScaleToSize>\n"
                + "  <wcs:format>image/tiff</wcs:format>\n"    
                + " </wcs:Extension>\n"                 
                + "</wcs:GetCoverage>";
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi()
                .createReaderInstance();
        reader.setInput(new FileImageInputStream(file));        
    }
        
    @Test
    public void testScaleExtentIndividualXML() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<wcs:GetCoverage\n"
                + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                + "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n"
                + "  xmlns:scal=\"http://www.opengis.net/wcs/scaling/1.0\"\n"                
                + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n"
                + "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n"
                + "  http://www.opengis.net/wcs/geotiff/1.0 \n"
                + "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n"
                + "  service=\"WCS\"\n" 
                + "  version=\"2.0.1\">\n" 
                + "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n"
                + "  <wcs:Extension>\n"                   
                + "  <scal:ScaleToExtent>\n"
                + "    <scal:TargetAxisExtent>\n"
                + "      <scal:axis>\n"
                + "        http://www.opengis.net/def/axis/OGC/1/i\n"
                + "      </scal:axis>\n"
                + "      <scal:low>10</scal:low>\n"
                + "      <scal:high>20</scal:high>\n"
                + "    </scal:TargetAxisExtent>\n"
                + "    <scal:TargetAxisExtent>\n"
                + "      <scal:axis>\n"
                + "        http://www.opengis.net/def/axis/OGC/1/j\n"
                + "      </scal:axis>\n"
                + "      <scal:low>20</scal:low>\n"
                + "      <scal:high>30</scal:high>\n"
                + "    </scal:TargetAxisExtent>\n"
                + "  </scal:ScaleToExtent>\n"
                + "  </wcs:Extension>\n"  
                + "  <wcs:format>image/tiff</wcs:format>\n"                
                + "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi()
                .createReaderInstance();
        reader.setInput(new FileImageInputStream(file));
    } 
    
    @Test
    public void testScaleFactorXML() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<wcs:GetCoverage\n"
                + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                + "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n"
                + "  xmlns:scal=\"http://www.opengis.net/wcs/scaling/1.0\"\n"                
                + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n"
                + "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n"
                + "  http://www.opengis.net/wcs/geotiff/1.0 \n"
                + "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n"
                + "  service=\"WCS\"\n" 
                + "  version=\"2.0.1\">\n" 
                + "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n"
                + "  <wcs:Extension>\n"                   
                + "    <scal:ScaleByFactor>"
                + "      <scal:ScaleFactor>2.0</scal:ScaleFactor>"
                + "    </scal:ScaleByFactor>\n"
                + "  </wcs:Extension>\n"                
                + "  <wcs:format>image/tiff</wcs:format>\n"                
                + "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi()
                .createReaderInstance();
        reader.setInput(new FileImageInputStream(file));
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
