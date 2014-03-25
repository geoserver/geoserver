package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.GetCapabilities;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
/**
 * Test for {@link GetCapabilities}
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
 */
public class GetCapabilitiesTest extends WCSTestSupport {
    
    @Before
    public void cleanupLimitedSRS() {
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        service.getSRS().clear();
        getGeoServer().save(service);
    }
    
    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        // print(dom);
        
        checkFullCapabilitiesDocument(dom);
    }
    
    @Test
    public void testCase() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=wCS");
        // print(dom);
        
        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }
    
    @Test
    public void testLimitedSRS() throws Exception {
        // check we support a lot of SRS by default
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        // print(dom);
        NodeList list = xpath.getMatchingNodes("//wcs:ServiceMetadata/wcs:Extension/wcscrs:crsSupported", dom);
        assertTrue(list.getLength() > 1000);

        // setup limited list
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        service.getSRS().add("4326");
        service.getSRS().add("32632");
        getGeoServer().save(service);
        
        dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        // print(dom);
        list = xpath.getMatchingNodes("//wcs:ServiceMetadata/wcs:Extension/wcscrs:crsSupported", dom);
        assertEquals(2, list.getLength());
    }
}

