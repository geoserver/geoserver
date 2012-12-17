package org.geoserver.wcs2_0;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.junit.Ignore;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * Testing range subsetting capabilities
 * 
 * @author Simone Giannecchini, GeoSolutions
 * TODO more tests with a landsat
 */
public class RangeSubsetExtentionTest extends WCSTestSupport {

    @Test
    public void testBasic() throws Exception {
        
        final File xml= new File("./src/test/resources/requestGetCoverageRangeSubsetting.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        Assert.assertTrue(CRS.equalsIgnoreMetadata(reader.getCrs(), CRS.decode("EPSG:4326",true)));
        assertEquals(360, reader.getOriginalGridRange().getSpan(0));
        assertEquals(360, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(1, coverage.getSampleDimensions().length);
        
        GridCoverage2D sourceCoverage = (GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();  
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);   
    }
    
    @Test
    @Ignore
    public void testRange() throws Exception {
        
        final File xml= new File("./src/test/resources/requestGetCoverageRangeSubsettingInterval.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        Assert.assertTrue(CRS.equalsIgnoreMetadata(reader.getCrs(), CRS.decode("EPSG:4326",true)));
        assertEquals(360, reader.getOriginalGridRange().getSpan(0));
        assertEquals(360, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(3, coverage.getSampleDimensions().length);
        
        GridCoverage2D sourceCoverage = (GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();  
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);   
    }
    

    @Test
    public void testWrong() throws Exception {
        
        final File xml= new File("./src/test/resources/requestGetCoverageWrongRangeSubsetting.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
    }    
}
