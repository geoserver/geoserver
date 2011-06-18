package org.geoserver.wfs.xml;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.w3c.dom.Document;

public class GMLOutputFormatTest extends WFSTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GMLOutputFormatTest());
    }

    public void testGML2() throws Exception {
        Document dom = getAsDOM( "wfs?request=getfeature&version=1.0.0&outputFormat=gml2&typename=" + 
            MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        assertNotNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull( getFirstElementByTagName(dom, "gml:exterior")); 
        
        dom = getAsDOM( "wfs?request=getfeature&version=1.1.0&outputFormat=gml2&typename=" + 
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        assertNotNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull( getFirstElementByTagName(dom, "gml:exterior")); 
        
        dom = getAsDOM( "wfs?request=getfeature&version=1.0.0&outputFormat=text/xml; subtype%3Dgml/2.1.2&typename=" + 
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        assertNotNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull( getFirstElementByTagName(dom, "gml:exterior")); 
        
        dom = getAsDOM( "wfs?request=getfeature&version=1.1.0&outputFormat=text/xml; subtype%3Dgml/2.1.2&typename=" + 
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        assertNotNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNull( getFirstElementByTagName(dom, "gml:exterior")); 
    }
    
    public void testGML2GZIP() throws Exception {
//        InputStream input = get( "wfs?request=getfeature&version=1.0.0&outputFormat=gml2-gzip&typename=" + 
//            MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
//        GZIPInputStream zipped = new GZIPInputStream( input );
//        
//        Document dom = dom( zipped );
//        zipped.close();
//        
//        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
//        assertNotNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
//        assertNull( getFirstElementByTagName(dom, "gml:exterior")); 
    }
    
    public void testGML3() throws Exception {
        Document dom = getAsDOM( "wfs?request=getfeature&version=1.0.0&outputFormat=gml3&typename=" + 
            MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        assertNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull( getFirstElementByTagName(dom, "gml:exterior")); 
        
        dom = getAsDOM( "wfs?request=getfeature&version=1.1.0&outputFormat=gml3&typename=" + 
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        assertNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull( getFirstElementByTagName(dom, "gml:exterior")); 
        
        dom = getAsDOM( "wfs?request=getfeature&version=1.0.0&outputFormat=text/xml; subtype%3Dgml/3.1.1&typename=" + 
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        assertNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull( getFirstElementByTagName(dom, "gml:exterior")); 
        
        dom = getAsDOM( "wfs?request=getfeature&version=1.1.0&outputFormat=text/xml; subtype%3Dgml/3.1.1&typename=" + 
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals( "FeatureCollection", dom.getDocumentElement().getLocalName() );
        assertNull( getFirstElementByTagName(dom, "gml:outerBoundaryIs"));
        assertNotNull( getFirstElementByTagName(dom, "gml:exterior")); 
    }
}
