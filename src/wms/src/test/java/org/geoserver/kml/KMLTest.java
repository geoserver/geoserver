/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.net.URL;
import java.util.Collections;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.wms.WMSTestSupport;
import org.w3c.dom.Document;

public class KMLTest extends WMSTestSupport {
    
        
    private static final QName STORM_OBS = new QName(MockData.CITE_URI, "storm_obs", MockData.CITE_PREFIX);

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
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("notthere", KMLTest.class.getResource("notthere.sld"));
        dataDirectory.addStyle("scaleRange", KMLTest.class.getResource("scaleRange.sld"));
        URL soProperty = KMLTest.class.getResource("storm_obs.properties");
        dataDirectory.addPropertiesType(STORM_OBS, soProperty, Collections.EMPTY_MAP);
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
        // print(doc);
        
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
    
    public void testEncodeTime() throws Exception {
        setupTemplate(STORM_OBS, "time.ftl", "${obs_datetime.value}");
        // AA: for the life of me I cannot make xpath work against this output, not sure why, so going
        // to test with strings instead...
        String doc = getAsString("wms?request=getmap&service=wms&version=1.1.1" + "&format="
                + KMLMapOutputFormat.MIME_TYPE + "&layers=" + getLayerId(STORM_OBS)
                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&featureId=storm_obs.1321870537475");
        assertTrue(doc.contains("<when>1994-07-0"));
    }

    public void testKmltitleFormatOption() throws Exception {
        final String kmlRequest = "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + getLayerId(MockData.BRIDGES) +  
            "&styles=notthere" + 
            "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326" +
            "&format_options=kmltitle:myCustomLayerTitle";
        
        Document doc = getAsDOM(kmlRequest);
        assertEquals("name", doc.getElementsByTagName("Document").item(0).getFirstChild().getNextSibling().getLocalName());
        assertEquals("myCustomLayerTitle", doc.getElementsByTagName("Document").item(0).getFirstChild().getNextSibling().getTextContent());
    }    

    public void testKmltitleFormatOptionWithMultipleLayers() throws Exception {
        final String kmlRequest = "wms?request=getmap&service=wms&version=1.1.1" + 
        "&format=" + KMLMapOutputFormat.MIME_TYPE + 
        "&layers=" + getLayerId(MockData.BRIDGES) + "," + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart() +
        "&styles=notthere" + "," + MockData.BASIC_POLYGONS.getLocalPart() +
        "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326" +
        "&format_options=kmltitle:myCustomLayerTitle";
        
        Document doc = getAsDOM(kmlRequest);
        assertEquals("name", doc.getElementsByTagName("Document").item(0).getFirstChild().getNextSibling().getLocalName());
        assertEquals(3, doc.getElementsByTagName("Document").getLength());
        assertEquals("myCustomLayerTitle", doc.getElementsByTagName("Document").item(0).getFirstChild().getNextSibling().getTextContent());
        assertEquals("cite:Bridges", doc.getElementsByTagName("Document").item(1).getFirstChild().getNextSibling().getTextContent());
        assertEquals("cite:BasicPolygons", doc.getElementsByTagName("Document").item(2).getFirstChild().getNextSibling().getTextContent());
    }
}