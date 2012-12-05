package org.geoserver.wcs2_0;

import java.util.Date;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WCSTestSupport {

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
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        print(dom);
        
        checkValidationErrors(dom, WCS20_SCHEMA);
    }
}
