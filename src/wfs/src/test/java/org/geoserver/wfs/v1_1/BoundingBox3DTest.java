package org.geoserver.wfs.v1_1;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.geoserver.wfs.WFSTestSupport;

/**
 * 
 * Test for 3D Bounding Box with Simple Features
 * 
 * @author Niels Charlier
 *
 */
public class BoundingBox3DTest extends WFSTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    /*public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureTest());
    }*/
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        // add extra types
        dataDirectory.addPropertiesType( 
                new QName( MockData.SF_URI, "With3D", MockData.SF_PREFIX ), 
                org.geoserver.wfs.v1_1.GetFeatureTest.class.getResource("With3D.properties"), 
                Collections.EMPTY_MAP);
    }
        
    public void testBBox1() throws Exception {
        Document doc = getAsDOM( "wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:With3D&bbox=-200,-200,0,200,200,50");
        
        NodeList features = doc.getElementsByTagName("sf:With3D");
        assertEquals( 1, features.getLength() );
        
        assertEquals(features.item(0).getAttributes().getNamedItem("gml:id").getNodeValue(), "fid1");        
    }
    
    public void testBBox2() throws Exception {
        Document doc = getAsDOM( "wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:With3D&bbox=-200,-200,50,200,200,100");
        
        NodeList features = doc.getElementsByTagName("sf:With3D");
        assertEquals( 1, features.getLength() );
        
        assertEquals(features.item(0).getAttributes().getNamedItem("gml:id").getNodeValue(), "fid2");    
        
    }

}
