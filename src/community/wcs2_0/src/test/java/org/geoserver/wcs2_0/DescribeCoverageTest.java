package org.geoserver.wcs2_0;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

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
        final File xml= new File("./src/test/resources/testDescribeCoverage.xml");
        final String request= FileUtils.readFileToString(xml);
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        
        Document dom = postAsDOM("wcs", request);
        assertNotNull(dom);
        print(dom, System.out);
        
        checkValidationErrors(dom, WCS20_SCHEMA);
        
    }
}
