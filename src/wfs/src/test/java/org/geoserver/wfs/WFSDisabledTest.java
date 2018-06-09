/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;

public class WFSDisabledTest extends WFSTestSupport {

    @Test
    public void testDisabledServiceResponse() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEnabled(false);
        getGeoServer().save(wfs);

        Document doc = getAsDOM("wfs?service=WFS&version=1.0.0&request=getCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testEnabledServiceResponse() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEnabled(true);
        getGeoServer().save(wfs);

        Document doc = getAsDOM("wfs?service=WFS&version=1.1.0&request=getCapabilities");
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
    }
}
