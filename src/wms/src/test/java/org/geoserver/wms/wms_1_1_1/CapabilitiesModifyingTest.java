package org.geoserver.wms.wms_1_1_1;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class CapabilitiesModifyingTest extends GeoServerTestSupport {
    public void testMisconfiguredLayerGeneratesErrorDocumentInDefaultConfig() throws Exception {
        for (FeatureTypeInfo ft : getCatalog().getFeatureTypes()) {
            ft.setLatLonBoundingBox(null);
            getCatalog().save(ft);
        }
        
        MockHttpServletResponse response = getAsServletResponse(
                "wms?service=WMS&request=GetCapabilities&version=1.1.1");
        assertTrue("Response does not contain ServiceExceptionReport: " + response.getOutputStreamContent(),
                response.getOutputStreamContent().endsWith("</ServiceExceptionReport>"));
    }
    
    public void testMisconfiguredLayerIsSkippedWhenWMSServiceIsConfiguredThatWay() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);
        
        for (FeatureTypeInfo ft : getCatalog().getFeatureTypes()) {
            ft.setLatLonBoundingBox(null);
            getCatalog().save(ft);
        }
        
        Document caps = getAsDOM(
                "wms?service=WMS&request=GetCapabilities&version=1.1.1");
        
        assertEquals("WMT_MS_Capabilities", caps.getDocumentElement().getTagName());
        // we misconfigured all the layers in the server, so there should be no named layers now.
        XMLAssert.assertXpathEvaluatesTo("", "//Layer/Name/text()", caps);
    }
    
    public void testMisconfiguredLayerGeneratesErrorDocumentInDefaultConfig_1_3_0() throws Exception {
        for (FeatureTypeInfo ft : getCatalog().getFeatureTypes()) {
            ft.setLatLonBoundingBox(null);
            getCatalog().save(ft);
        }
        
        MockHttpServletResponse response = getAsServletResponse(
                "wms?service=WMS&request=GetCapabilities&version=1.3.0");
        assertTrue("Response does not contain ServiceExceptionReport: " + response.getOutputStreamContent(),
                response.getOutputStreamContent().endsWith("</ServiceExceptionReport>"));
    }
    
    public void testMisconfiguredLayerIsSkippedWhenWMSServiceIsConfiguredThatWay_1_3_0() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);
        
        for (FeatureTypeInfo ft : getCatalog().getFeatureTypes()) {
            ft.setLatLonBoundingBox(null);
            getCatalog().save(ft);
        }
        
        Document caps = getAsDOM(
                "wms?service=WMS&request=GetCapabilities&version=1.3.0");
        
        assertEquals("WMS_Capabilities", caps.getDocumentElement().getTagName());
        // we misconfigured all the layers in the server, so there should be no named layers now.
        XMLAssert.assertXpathEvaluatesTo("", "//Layer/Name/text()", caps);
    }
}
