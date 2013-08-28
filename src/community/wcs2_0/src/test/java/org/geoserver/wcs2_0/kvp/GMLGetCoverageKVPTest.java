package org.geoserver.wcs2_0.kvp;
import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.geoserver.wcs2_0.WCSTestSupport;
import org.geoserver.wcs2_0.response.GMLCoverageResponseDelegate;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * Testing {@link GMLCoverageResponseDelegate}
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class GMLGetCoverageKVPTest extends WCSTestSupport {

   
    @Test 
    public void gmlFormat() throws Exception {
        MockHttpServletResponse response = 
            getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
        "&coverageId=wcs__BlueMarble&format=application%2Fgml%2Bxml");
        
        assertEquals("application/gml+xml", response.getContentType());
        Document dom = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));     
//        print(dom);
        
        // validate
//        checkValidationErrors(dom, WCS20_SCHEMA);TODO Fix
        
        // check it is good
//        assertXpathEvaluatesTo("3", "count(//gml:RectifiedGridCoverage//gml:rangeType//swe:DataRecord)", dom);
//        assertXpathEvaluatesTo("1", "count(//gml:RectifiedGridCoverage)", dom);

    }
}
