/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Test;

import org.geotools.data.DataUtilities;
import org.geotools.image.test.ImageAssert;
import org.w3c.dom.Document;


/**
 * 
 * @author Niels Charlier
 * 
 */
public class WmsGetMapTest extends AbstractAppSchemaTestSupport {

    public WmsGetMapTest() throws Exception {
        super();
    }
    
    @Override
    protected WmsSupportMockData createTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("outcropcharacter", "styles/outcropcharacter.sld");
        mockData.addStyle("positionalaccuracy", "styles/positionalaccuracy.sld");
        return mockData;
    }
     
    @Test
    public void testGetMapOutcropCharacter() throws Exception
    {
        InputStream is = getBinary("wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=outcropcharacter&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        BufferedImage imageBuffer = ImageIO.read(is);
                
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);   
        ImageAssert.assertEquals(DataUtilities.urlToFile(getClass().getResource("/test-data/img/outcrop.tiff")), imageBuffer, 250);
    }
    
    @Test
    public void testGetMapOutcropCharacterReprojection() throws Exception
    {
        InputStream is = getBinary("wms?request=GetMap&SRS=EPSG:4283&layers=gsml:MappedFeature&styles=outcropcharacter&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        BufferedImage imageBuffer = ImageIO.read(is);
                
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);   
        ImageAssert.assertEquals(DataUtilities.urlToFile(getClass().getResource("/test-data/img/outcrop.tiff")), imageBuffer, 250);

    }
    
    @Test
    public void testGetMapPositionalAccuracy() throws Exception
    {
        InputStream is = getBinary("wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=positionalaccuracy&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        BufferedImage imageBuffer = ImageIO.read(is);
        
        assertNotBlank("app-schema test getmap positional accuracy", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(DataUtilities.urlToFile(getClass().getResource("/test-data/img/posacc.tiff")), imageBuffer, 250);
        
        /*DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File("/home/niels/Desktop/outcrop.jpg"))));
        int data;
        while((data = is.read()) >= 0) {
                out.writeByte(data);
        }
        is.close();
        out.close();*/
    }  
    
   
    @Test
    public void testGetMapAfterWFS() throws Exception
    {
        Document doc = getAsDOM("wfs?version=1.1.0&request=getFeature&typeName=gsml:MappedFeature&maxFeatures=1");
        LOGGER.info(prettyString(doc));
        
        InputStream is = getBinary("wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=outcropcharacter&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        BufferedImage imageBuffer = ImageIO.read(is);
                
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);   
        ImageAssert.assertEquals(DataUtilities.urlToFile(getClass().getResource("/test-data/img/outcrop.tiff")), imageBuffer, 250);

    }

}
