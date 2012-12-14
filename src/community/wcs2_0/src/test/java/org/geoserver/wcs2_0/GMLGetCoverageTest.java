package org.geoserver.wcs2_0;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayInputStream;

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
public class GMLGetCoverageTest extends WCSTestSupport {

   
    @Test 
    public void testGMLExtension() throws Exception {

        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        "<wcs:GetCoverage\n" + 
        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd\"" + 
        "  service=\"WCS\"\n" + 
        "  version=\"2.0.1\">\n" + 
        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
        "  <wcs:format>application/gml+xml</wcs:format>\n" + 
        "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("application/gml+xml", response.getContentType());
        Document dom = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));
        print(dom);

    }
}
