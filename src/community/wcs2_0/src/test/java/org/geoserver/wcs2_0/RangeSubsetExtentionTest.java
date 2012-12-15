package org.geoserver.wcs2_0;

import static junit.framework.Assert.assertEquals;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.io.File;

import javax.imageio.stream.FileImageInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * Testing range subsetting capabilities
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
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

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi()
                .createReaderInstance();
        reader.setInput(new FileImageInputStream(file)); 
        assertEquals(360, reader.getWidth(0));
        assertEquals(360, reader.getHeight(0));
        reader.dispose();           
    }
    

}
