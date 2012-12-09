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
        
        checkFullCapabilitiesDocument(dom);
    }
    
    @Test
    public void testBasicPost() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "xmlns:wcs=\"http://www.opengis.net/wcs/2.0\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        Document dom = postAsDOM("wcs", request);
        print(dom);
        
        checkFullCapabilitiesDocument(dom);
    }

    private void checkFullCapabilitiesDocument(Document dom) throws Exception {
        checkValidationErrors(dom, WCS20_SCHEMA);
        
        // todo: check all the layers are here, the profiles, and so on
    }
}
