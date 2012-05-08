/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import junit.framework.Test;
import org.geoserver.test.NamespaceTestData;
import org.geotools.image.test.ImageAssert;
import java.io.File;

public class WmsGetMapTest extends AbstractAppSchemaWfsTestSupport {

    public WmsGetMapTest() throws Exception {
        super();
    }
    
    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        try {
            return new OneTimeTestSetup(new WmsGetMapTest());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected NamespaceTestData buildTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("outcropcharacter", "styles/outcropcharacter.sld");
        mockData.addStyle("positionalaccuracy", "styles/positionalaccuracy.sld");
        return mockData;
    }
     
    public void testGetMapOutcropCharacter() throws Exception
    {
        InputStream is = getBinary("wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=outcropcharacter&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        BufferedImage imageBuffer = ImageIO.read(is);
                
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);   
<<<<<<< HEAD
        ImageAssert.assertEquals(new File(getClass().getResource("/test-data/img/outcrop.tiff").getFile()), imageBuffer, -1);

=======
        //ImageAssert.assertEquals(new File(getClass().getResource("outcrop.jpg").getFile()), imageBuffer, -1);
        
        /*DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File("/home/niels/Desktop/outcrop.jpg"))));
        int data;
        while((data = is.read()) >= 0) {
                out.writeByte(data);
        }
        is.close();
        out.close();*/
>>>>>>> wmstests
    }
    
    public void testGetMapPositionalAccuracy() throws Exception
    {
        InputStream is = getBinary("wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=positionalaccuracy&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        BufferedImage imageBuffer = ImageIO.read(is);
        
        assertNotBlank("app-schema test getmap positional accuracy", imageBuffer, Color.WHITE);
<<<<<<< HEAD
        ImageAssert.assertEquals(new File(getClass().getResource("/test-data/img/posacc.tiff").getFile()), imageBuffer, -1);
=======
        //ImageAssert.assertEquals(new File(getClass().getResource("posacc.jpg").getFile()), imageBuffer, -1);
>>>>>>> wmstests
        
       /* DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File("/home/niels/Desktop/posacc.jpg"))));
        int data;
        while((data = is.read()) >= 0) {
                out.writeByte(data);
        }
        is.close();
        out.close();*/
    }  

}
