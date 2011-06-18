package org.geoserver.wcs;

import org.geoserver.wcs.test.WCSTestSupport;
import org.w3c.dom.Document;

public class WCSDisabledTest extends WCSTestSupport {
    
    public void testDisabledServiceResponse() throws Exception {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.setEnabled(false);
        getGeoServer().save(wcs);
        
        Document doc = getAsDOM("wcs?service=WCS&request=getCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement()
                .getNodeName());
    }
    
    public void testEnabledServiceResponse() throws Exception {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.setEnabled(true);
        getGeoServer().save(wcs);

        Document doc = getAsDOM("wcs?service=WCS&request=getCapabilities");
        assertEquals("wcs:Capabilities", doc.getDocumentElement()
                .getNodeName());
    }
}
