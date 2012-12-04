package org.geoserver.wcs2_0;

import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WCSTestSupport {

    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        print(dom);
        
        checkValidationErrors(dom, WCS20_SCHEMA);
    }
}
