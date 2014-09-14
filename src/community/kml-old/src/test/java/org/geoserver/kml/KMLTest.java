/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class KMLTest extends WMSTestSupport {
    
        
    private static final QName STORM_OBS = new QName(MockData.CITE_URI, "storm_obs", MockData.CITE_PREFIX);

    
    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("notthere","notthere.sld",getClass(),catalog);
        testData.addStyle("scaleRange","scaleRange.sld",getClass(),catalog);
        testData.addVectorLayer(STORM_OBS,Collections.EMPTY_MAP, 
                "storm_obs.properties",getClass(),catalog);
    }
    
    
    @Test
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
    
    @Test
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
   
    @Test
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
    
    @Test
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
    @Test
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
    
    @Test
    public void testContentDisposition() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse(
                "wms?request=getmap&service=wms&version=1.1.1"
                + "&format=" + KMZMapOutputFormat.MIME_TYPE
                + "&layers=" + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart()
                + "&styles=" + MockData.BASIC_POLYGONS.getLocalPart()
                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
            );
        assertEquals("attachment; filename=cite-BasicPolygons.kmz",resp.getHeader("Content-Disposition"));
    }
    
    @Test
    public void testEncodeTime() throws Exception {
        setupTemplate(STORM_OBS, "time.ftl", "${obs_datetime.value}");
        // AA: for the life of me I cannot make xpath work against this output, not sure why, so going
        // to test with strings instead...
        String doc = getAsString("wms?request=getmap&service=wms&version=1.1.1" + "&format="
                + KMLMapOutputFormat.MIME_TYPE + "&layers=" + getLayerId(STORM_OBS)
                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&featureId=storm_obs.1321870537475");
        assertTrue(doc.contains("<when>1994-07-0"));
    }

    @Test
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

    @Test
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
