package org.geoserver.wfs;

import junit.framework.Test;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class MaxFeaturesTest extends WFSTestSupport {

    private static Catalog catalog;
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new MaxFeaturesTest());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        // set global max to 5
        GeoServer gs = getGeoServer();
        
        WFSInfo wfs = getWFS();
        wfs.setMaxFeatures( 5 );
        gs.save( wfs );
        
        catalog = getCatalog();
    }

    public void testGlobalMax() throws Exception {
        // fifteen has 15 elements, but global max is 5
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen" +
        		"&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(5, featureMembers.getLength());
    }
    
    public void testLocalMax() throws Exception {
        // setup different max on local
        FeatureTypeInfo info = getFeatureTypeInfo(MockData.FIFTEEN );
        info.setMaxFeatures(3);
        catalog.save( info );
        
        // fifteen has 15 elements, but global max is 5 and local is 3
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen" +
        		"&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(3, featureMembers.getLength());
    }
    
    public void testLocalMaxBigger() throws Exception {
        // setup different max on local
        FeatureTypeInfo info = getFeatureTypeInfo(MockData.FIFTEEN);
        info.setMaxFeatures(10);
        catalog.save( info );
        
        // fifteen has 15 elements, but global max is 5 and local is 10
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen" +
        		"&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(5, featureMembers.getLength());
    }
    
    public void testCombinedLocalMaxes() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(MockData.FIFTEEN);
        info.setMaxFeatures(2);
        catalog.save( info );
        
        info = getFeatureTypeInfo(MockData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        catalog.save( info );
        
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen,cite:BasicPolygons" +
        		"&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(4, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(2, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(2, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }
    
    public void testCombinedLocalMaxesBigger() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(MockData.FIFTEEN);
        info.setMaxFeatures(4);
        catalog.save( info );
        
        info = getFeatureTypeInfo(MockData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        catalog.save( info );
        
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen,cite:BasicPolygons" +
        		"&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(5, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(4, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(1, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }
    
    public void testCombinedLocalMaxesBiggerRequestOverride() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(MockData.FIFTEEN);
        info.setMaxFeatures(3);
        catalog.save(info);
        
        info = getFeatureTypeInfo(MockData.BASIC_POLYGONS);
        catalog.save(info);
        
        info.setMaxFeatures(2);
        
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen,cite:BasicPolygon" +
        		"s&version=1.0.0&service=wfs&maxFeatures=4");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(4, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(3, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(1, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }
    
    public void testMaxFeaturesBreak() throws Exception {
        // see http://jira.codehaus.org/browse/GEOS-1489
        FeatureTypeInfo info = getFeatureTypeInfo(MockData.FIFTEEN);
        info.setMaxFeatures(3);
        catalog.save( info );
        
        info = getFeatureTypeInfo(MockData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        catalog.save(info);
        
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen,cite:BasicPolygon" +
                "s&version=1.0.0&service=wfs&maxFeatures=3");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(3, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(3, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(0, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }
    
    
}
