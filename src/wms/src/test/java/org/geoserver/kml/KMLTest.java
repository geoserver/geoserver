/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.wms.WMSTestSupport;
import org.w3c.dom.Document;

public class KMLTest extends WMSTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new KMLTest());
    }
    
    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("notthere", KMLTest.class.getResource("notthere.sld"));
        dataDirectory.addStyle("scaleRange", KMLTest.class.getResource("scaleRange.sld"));
    }
    
    public void testVector() throws Exception {
        Document doc = getAsDOM(
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart() +
            "&styles=" + MockData.BASIC_POLYGONS.getLocalPart() + 
            "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326" 
        );
        
        assertEquals( getFeatureSource(MockData.BASIC_POLYGONS).getFeatures().size(), 
            doc.getElementsByTagName("Placemark").getLength()
        );
    }
    
    public void testVectorScaleRange() throws Exception {
        Document doc = getAsDOM(
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart() +
            "&styles=scaleRange&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326" 
        );
        
        assertEquals( getFeatureSource(MockData.BASIC_POLYGONS).getFeatures().size(), 
            doc.getElementsByTagName("Placemark").getLength()
        );
    }
   
    public void testVectorWithFeatureId() throws Exception {
        Document doc = getAsDOM(
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart() +
            "&styles=" + MockData.BASIC_POLYGONS.getLocalPart() + 
            "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326" +  
            "&featureid=BasicPolygons.1107531493643"
        );
        
        assertEquals( 1, doc.getElementsByTagName("Placemark").getLength());
    }
    
    public void testVectorWithRemoteLayer() throws Exception {
        if(!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER))
            return;
        
        Document doc = getAsDOM(
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=topp:states" + 
            "&styles=Default" + 
            "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326" +
            "&remote_ows_type=wfs" +
            "&remote_ows_url=" + RemoteOWSTestSupport.WFS_SERVER_URL +
            "&cql_filter=PERSONS>20000000"
        );
        print(doc);
        
        assertEquals( 1, doc.getElementsByTagName("Placemark").getLength());
    }
   
    // see GEOS-1948
    public void testMissingGraphic() throws Exception {
        Document doc = getAsDOM(
                "wms?request=getmap&service=wms&version=1.1.1" + 
                "&format=" + KMLMapOutputFormat.MIME_TYPE + 
                "&layers=" + getLayerId(MockData.BRIDGES) +  
                "&styles=notthere" + 
                "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
            );
        assertEquals( 1, doc.getElementsByTagName("Placemark").getLength());
    }
}
