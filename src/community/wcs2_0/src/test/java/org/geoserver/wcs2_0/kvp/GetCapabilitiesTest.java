package org.geoserver.wcs2_0.kvp;

import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WCSTestSupport {
    
    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        print(dom);
        
        checkFullCapabilitiesDocument(dom);
    }
}

