package org.geoserver.wcs2_0;

import static org.custommonkey.xmlunit.XMLAssert.*;

import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WCSTestSupport {
    
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
        // print(dom);
        
        checkFullCapabilitiesDocument(dom);
    }

    private void checkFullCapabilitiesDocument(Document dom) throws Exception {
        checkValidationErrors(dom, WCS20_SCHEMA);
        
        // todo: check all the layers are here, the profiles, and so on
        
        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs'])", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:ServiceMetadata/wcs:Extension[wcscrs:crsSupported = 'http://www.opengis.net/def/crs/EPSG/0/4326'])", dom);
        
        // check the interpolation extension
        assertXpathEvaluatesTo("1", "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/interpolation'])", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:ServiceMetadata/wcs:Extension[int:interpolationSupported='http://www.opengis.net/def/interpolation/OGC/1/nearest-neighbor'])", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:ServiceMetadata/wcs:Extension[int:interpolationSupported='http://www.opengis.net/def/interpolation/OGC/1/linear'])", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:ServiceMetadata/wcs:Extension[int:interpolationSupported='http://www.opengis.net/def/interpolation/OGC/1/cubic'])", dom);
    }
}

