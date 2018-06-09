/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.*;

import java.util.logging.Level;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.w3c.dom.Document;

public class WMSDisabledTest extends WMSTestSupport {

    @Test
    public void testDisabledServiceResponse() throws Exception {
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(false);
        getGeoServer().save(wms);

        Document doc = getAsDOM("wms?service=WMS&version=1.1.1&request=getCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testEnabledServiceResponse() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(true);
        getGeoServer().save(wms);

        Document doc = getAsDOM("wms?service=WMS&version=1.1.1&request=getCapabilities");
        assertEquals("WMT_MS_Capabilities", doc.getDocumentElement().getNodeName());
    }
}
