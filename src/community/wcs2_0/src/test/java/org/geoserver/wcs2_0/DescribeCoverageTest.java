package org.geoserver.wcs2_0;

import java.util.Date;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;

public class DescribeCoverageTest extends WCSTestSupport {

    protected final static String VERSION=WCS20Const.CUR_VERSION;
    protected final static String DESCRIBE_URL = "wcs?service=WCS&version="+VERSION+"&request=DescribeCoverage";

    @BeforeClass
    public static void bc() {
        System.out.println("-BeforeClass---> " + new Date());
    }

    @Before
    public void b() {
        System.out.println("-Before---> " + new Date());
    }

    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }
    
    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__BlueMarble");
        assertNotNull(dom);
        print(dom, System.out);
        
        checkValidationErrors(dom, WCS20_SCHEMA);
    }
    
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
