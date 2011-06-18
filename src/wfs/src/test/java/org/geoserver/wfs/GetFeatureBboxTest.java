package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import junit.framework.Test;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;

public class GetFeatureBboxTest extends WFSTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureBboxTest());
    }
    
    public void testFeatureBoudingOn() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding( true );
        getGeoServer().save( wfs );
        
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=" + getLayerId(MockData.BUILDINGS) + "&version=1.0.0&service=wfs&propertyName=ADDRESS");
        // print(doc);
        
        // check it's a feature collection
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        // check the collection has non null bounds
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection/gml:boundedBy/gml:Box)", doc);
        // check that each feature has non null bounds
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//cite:Buildings/gml:boundedBy/gml:Box", doc).getLength() > 0);
        
    }
    
    public void testFeatureBoudingOff() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding( false );
        getGeoServer().save( wfs );
        
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=" + getLayerId(MockData.BUILDINGS) + "&version=1.0.0&service=wfs&propertyName=ADDRESS");
//        print(doc);
        
        // check it's a feature collection
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        // check the collection does not have bounds
        assertXpathEvaluatesTo("0", "count(//wfs:FeatureCollection/gml:boundedBy/gml:Box)", doc);
        // check that each feature has non null bounds
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(0, xpath.getMatchingNodes("//cite:Buildings/gml:boundedBy/gml:Box", doc).getLength());
    }
    

}
