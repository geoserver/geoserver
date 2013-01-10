package org.geoserver.wcs2_0.post;

import static org.junit.Assert.assertNotNull;

import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class DescribeCoverageTest extends WCSTestSupport {

    @Test
    public void testDescribeCoveragePOST() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        		"<wcs:DescribeCoverage xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n" + 
        		"  xmlns:wcs='http://www.opengis.net/wcs/2.0' xmlns:gml='http://www.opengis.net/gml/3.2'\n" + 
        		"  xsi:schemaLocation='http://www.opengis.net/wcs/2.0 http://schemas.opengis.net/wcs/2.0/wcsAll.xsd'\n" + 
        		"  service=\"WCS\" version=\"2.0.1\">\n" + 
        		"  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
        		"</wcs:DescribeCoverage>";
        
        Document dom = postAsDOM("wcs", request);
        assertNotNull(dom);
        print(dom, System.out);
        
        checkValidationErrors(dom, WCS20_SCHEMA);
        
    }
}
