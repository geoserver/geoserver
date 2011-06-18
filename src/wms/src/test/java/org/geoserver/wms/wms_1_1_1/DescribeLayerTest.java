/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import java.util.logging.Level;

import junit.framework.Test;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;

public class DescribeLayerTest extends WMSTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeLayerTest());
    }
    
    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    public void testDescribeLayerVersion111() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request = "wms?service=wms&version=1.1.1&request=DescribeLayer&layers=" + layer;
        assertEquals("src/test/resources/geoserver", getGeoServer().getGlobal().getProxyBaseUrl());
        Document dom = getAsDOM(request, true);
        
        assertEquals("1.1.1", dom.getDocumentElement().getAttributes().getNamedItem("version").getNodeValue());
    }
    
//    public void testDescribeLayerVersion110() throws Exception {
//        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
//        String request = "wms?service=wms&version=1.1.0&request=DescribeLayer&layers=" + layer;
//        Document dom = getAsDOM(request);
//        assertEquals("1.1.0", dom.getDocumentElement().getAttributes().getNamedItem("version").getNodeValue());
//    }
    
    public void testWorkspaceQualified() throws Exception {
        Document dom = getAsDOM("cite/wms?service=wms&version=1.1.1&request=DescribeLayer" +
            "&layers=PrimitiveGeoFeature", true);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        dom = getAsDOM("sf/wms?service=wms&version=1.1.1&request=DescribeLayer" +
                "&layers=PrimitiveGeoFeature", true);
        assertEquals("WMS_DescribeLayerResponse", dom.getDocumentElement().getNodeName());
        
    }
}
