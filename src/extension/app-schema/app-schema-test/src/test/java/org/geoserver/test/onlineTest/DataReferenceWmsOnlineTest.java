/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.geoserver.test.onlineTest.support.AbstractDataReferenceWfsTest;
import org.w3c.dom.Document;

public abstract class DataReferenceWmsOnlineTest extends AbstractDataReferenceWfsTest {

    public DataReferenceWmsOnlineTest() throws Exception {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void __testMappedFeature() {
        String path = "wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature&featureid=gsml.mappedfeature.191907";
       // validateGet(path);
        Document doc = getAsDOM(path);
        LOGGER.info(prettyString(doc));
        
        
    }
    
    public void testGetMapPositionalAccuracy() throws Exception
    {
        InputStream is = getBinary("wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=simplelithology&BBOX=0,0,50,50&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
       // BufferedImage imageBuffer = ImageIO.read(is);
        
       // assertNotBlank("app-schema test getmap positional accuracy", imageBuffer, Color.WHITE);
        
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File("/home/niels/Desktop/mappedfeature_positionalaccuracy.jpg"))));
        int data;
        while((data = is.read()) >= 0) {
                out.writeByte(data);
        }
        is.close();
        out.close();
    }  

    
}
