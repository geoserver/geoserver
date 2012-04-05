/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import junit.framework.Test;

import org.geoserver.test.NamespaceTestData;

public class WmsGetLegendGraphicTest extends AbstractAppSchemaWfsTestSupport {

    public WmsGetLegendGraphicTest() throws Exception {
        super();
    }

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        try {
            return new OneTimeTestSetup(new WmsGetLegendGraphicTest());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected NamespaceTestData buildTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("outcropcharacter", "styles/outcropcharacter.sld");
        return mockData;
    }
     
    public void testGetLegendGraphicAll() throws Exception
    {
        InputStream is = getBinary("wms?request=GetLegendGraphic&SRS=EPSG:4326&layer=gsml:MappedFeature&style=outcropcharacter&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        
        BufferedImage imageBuffer = ImageIO.read(is);        
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);      
        assertPixel(imageBuffer, 10, 10, new Color(2,2,240));
        assertPixel(imageBuffer, 10, 30, new Color(247,2,10));
        assertPixel(imageBuffer, 10, 50, new Color(0,255,5));
    }
       
    public void testGetLegendGraphicBlueRule() throws Exception
    {
        InputStream is = getBinary("wms?request=GetLegendGraphic&rule=xrule&SRS=EPSG:4326&layer=gsml:MappedFeature&style=outcropcharacter&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        
        BufferedImage imageBuffer = ImageIO.read(is);        
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        assertPixel(imageBuffer, 10, 10, new Color(2,2,240));
    }    
    
    public void testGetLegendGraphicRedRule() throws Exception
    {
        InputStream is = getBinary("wms?request=GetLegendGraphic&rule=yrule&SRS=EPSG:4326&layer=gsml:MappedFeature&style=outcropcharacter&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        
        BufferedImage imageBuffer = ImageIO.read(is);        
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        assertPixel(imageBuffer, 10, 10, new Color(243,4,7));
    }
    
    public void testGetLegendGraphicGreenRule() throws Exception
    {
        InputStream is = getBinary("wms?request=GetLegendGraphic&rule=zrule&SRS=EPSG:4326&layer=gsml:MappedFeature&style=outcropcharacter&X=0&Y=0&width=20&height=20&FORMAT=image/jpeg");
        
        BufferedImage imageBuffer = ImageIO.read(is);        
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        assertPixel(imageBuffer, 10, 10, new Color(6,252,3));
    }

}
