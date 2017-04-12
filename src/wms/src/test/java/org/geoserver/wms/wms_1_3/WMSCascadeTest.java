/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSCascadeTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import org.springframework.mock.web.MockHttpServletResponse;

@RunWith(Parameterized.class)
public class WMSCascadeTest extends WMSCascadeTestSupport {
    
    private final boolean aphEnabled;

    @Parameters(name = "{index} APH enabled: {0}")
    public static Collection<Object[]> getParameters(){ 
        return Arrays.asList(new Object[]{true},new Object[]{false});
    }
    
    public WMSCascadeTest(boolean aphEnabled) {
        this.aphEnabled = aphEnabled;
    }
    
    @Before
    public void setupAdvancedProjectionHandling() {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, aphEnabled);
        gs.save(wms);
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        // on WMS 1.3 the requested area is enlarged to account for reprojection
        // this is not really needed, it's something we should optimize out. 
        // See GEOS-5837 and remove these when it is fixed
        URL pngImage = WMSTestSupport.class.getResource("world.png");
        wms13Client.expectGet(new URL(wms13BaseURL + "?service=WMS&version=1.3.0&request=GetMap&layers=world4326" 
                + "&styles&bbox=-110.0,-200.0,110.0,200.0&crs=EPSG:4326&bgcolor=0xFFFFFF&transparent=FALSE&format=image/png&width=190&height=100"), new MockHttpResponse(pngImage, "image/png"));
        wms11Client.expectGet(new URL(wms11BaseURL + "?service=WMS&version=1.1.1&request=GetMap&layers=world4326" 
                + "&styles&bbox=-200.0,-110.0,200.0,110.0&srs=EPSG:4326&bgcolor=0xFFFFFF&transparent=FALSE&format=image/png&width=190&height=100"), new MockHttpResponse(pngImage, "image/png"));
    }
    
    @Test
    public void testCascadeGetMapOnto13() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=-90,-180,90,180" +
        		"&styles=&layers=" + WORLD4326_130 + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                + "&width=180&height=90&crs=EPSG:4326");
        // we'll get a service exception if the requests are not the ones expected
        checkImage(response, "image/png", 180, 90);
    }
    
    @Test
    public void testCascadeGetMapOnto11() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=-90,-180,90,180" +
                "&styles=&layers=" + WORLD4326_110 + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                + "&width=180&height=90&crs=EPSG:4326");
        // we'll get a service exception if the requests are not the ones expected
        checkImage(response, "image/png", 180, 90);
    }
    
    @Test
    public void testCascadeCapabilitiesClientNoGetFeatureInfo() throws Exception {
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.3.0&service=wms");
        print(dom);
        
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("link", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        NamespaceContext newNsCtxt = new SimpleNamespaceContext(namespaces);
        
        xpath.setNamespaceContext(newNsCtxt);
        
        xpath.evaluate("//wms:Layer[name='" + WORLD4326_110_NFI + "']", dom);
    }
    
   
    
}
