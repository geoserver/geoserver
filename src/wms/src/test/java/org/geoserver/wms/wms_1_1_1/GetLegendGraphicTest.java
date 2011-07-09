/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import java.awt.Color;
import java.awt.image.BufferedImage;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.Converters;

public class GetLegendGraphicTest extends WMSTestSupport {
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetLegendGraphicTest());
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("paramFill", GetLegendGraphicTest.class.getResource("paramFill.sld"));
        dataDirectory.addStyle("paramStroke", GetLegendGraphicTest.class.getResource("paramStroke.sld"));
        dataDirectory.addStyle("raster", GetLegendGraphicTest.class.getResource("raster.sld"));
        dataDirectory.addStyle("rasterScales", GetLegendGraphicTest.class.getResource("rasterScales.sld"));
    }
    
    /**
     * Tests GML output does not break when asking for an area that has no data with
     * GML feature bounding enabled
     * 
     * @throws Exception
     */
    public void testPlain() throws Exception {
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetLegendGraphic" +
        		"&layer=" + getLayerId(MockData.LAKES) + "&style=Lakes" +
        		"&format=image/png&width=20&height=20", "image/png");
        assertPixel(image, 10, 10, Converters.convert("#4040C0", Color.class));
    }
    
    /**
     * Tests GML output does not break when asking for an area that has no data with
     * GML feature bounding enabled
     * 
     * @throws Exception
     */
    public void testEnv() throws Exception {
        // no params, use fallback
        String base = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic" +
                        "&layer=" + getLayerId(MockData.LAKES) + "&style=paramFill" +
                        "&format=image/png&width=20&height=20";
        BufferedImage image = getAsImage(base, "image/png");
        assertPixel(image, 10, 10, Converters.convert("#FFFFFF", Color.class));
        
        // specify color explicitly
        image = getAsImage(base + "&env=color:#FF0000", "image/png");
        assertPixel(image, 10, 10, Converters.convert("#FF0000", Color.class));
    }
    
   
    
    
}
