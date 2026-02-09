/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;

import org.geotools.util.Version;
import org.junit.Test;
import org.w3c.dom.Document;

public class WMSDisabledTest extends WMSTestSupport {

    /** Tests that a disabled version returns an exception */
    @Test
    public void testDisabledVersionReturnsException() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(true);
        wms.getDisabledVersions().clear();
        wms.getDisabledVersions().add(new Version("1.3.0"));
        getGeoServer().save(wms);

        // request disabled version -> should fail
        Document doc = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());

        wms.getDisabledVersions().clear();
        getGeoServer().save(wms);
    }

    /** Tests that an enabled version still works when another version is disabled */
    @Test
    public void testEnabledVersionStillWorks() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(true);
        wms.getDisabledVersions().clear();
        wms.getDisabledVersions().add(new Version("1.1.1"));
        getGeoServer().save(wms);

        // request enabled version -> should succeed
        Document doc = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
        assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());

        wms.getDisabledVersions().clear();
        getGeoServer().save(wms);
    }
}
