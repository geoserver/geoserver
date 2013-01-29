package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.assertNotNull;

import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class DescribeCoverageTest extends WCSTestSupport {
    protected final static String DESCRIBE_URL = "wcs?service=WCS&version="+VERSION+"&request=DescribeCoverage";

    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__BlueMarble");
        assertNotNull(dom);
        print(dom, System.out);
        
        checkValidationErrors(dom, WCS20_SCHEMA);
    }
    
    @Test
    public void testMultiBandKVP() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__multiband");
        assertNotNull(dom);
        print(dom, System.out);
        
        checkValidationErrors(dom, WCS20_SCHEMA);
    }
}
