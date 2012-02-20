/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.xml.namespace.QName;

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

        dataDirectory.addStyle("Population", GetLegendGraphicTest.class.getResource("Population.sld"));
        dataDirectory.addStyle("uom", GetLegendGraphicTest.class.getResource("uomStroke.sld"));
        dataDirectory.addPropertiesType(new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                getClass().getResource("states.properties"), null);
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
    
    /**
     * Tests an unscaled states legend
     */
    public void testStatesLegend() throws Exception {
        String base = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic" +
                        "&layer=sf:states&style=Population" +
                        "&format=image/png&width=20&height=20";
        BufferedImage image = getAsImage(base, "image/png");
        
        // check RGB is in the expected positions
        assertPixel(image, 10, 10, Color.RED);
        assertPixel(image, 10, 30, Color.GREEN);
        assertPixel(image, 10, 50, Color.BLUE);
    }
    
    /**
     * Tests a dpi rescaled legend
     */
    public void testStatesLegendDpiRescaled() throws Exception {
        String base = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic" +
                        "&layer=sf:states&style=Population" +
                        "&format=image/png&width=20&height=20&legend_options=dpi:180";
        BufferedImage image = getAsImage(base, "image/png");
        
        assertPixel(image, 20, 20, Color.RED);
        assertPixel(image, 20, 60, Color.GREEN);
        assertPixel(image, 20, 100, Color.BLUE);
        Color linePixel = getPixelColor(image, 20, 140);
        assertTrue(linePixel.getRed() < 10);
        assertTrue(linePixel.getGreen() < 10);
        assertTrue(linePixel.getBlue() < 10);
    }
    
    /**
     * Tests a uom rescaled legend
     */
    public void testStatesLegendUomRescaled() throws Exception {
        String base = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                + "&layer=sf:states&style=uom"
                + "&format=image/png&width=20&height=20&scale=1000000";
        BufferedImage image = getAsImage(base, "image/png");

        assertPixel(image, 10, 10, Color.BLUE);
        assertPixel(image, 5, 10, Color.WHITE);
        assertPixel(image, 1, 10, Color.WHITE);

        // halve the scale denominator, we're zooming in, the thickness should double
        base = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                + "&layer=sf:states&style=uom"
                + "&format=image/png&width=20&height=20&scale=500000";
        image = getAsImage(base, "image/png");

        assertPixel(image, 10, 10, Color.BLUE);
        assertPixel(image, 5, 10, Color.BLUE);
        assertPixel(image, 1, 10, Color.WHITE);
    }
    
    /**
     * Tests a dpi _and_ uom rescaled image
     */
    public void testStatesLegendDpiUomRescaled() throws Exception {
        // halve the scale denominator, we're zooming in, the thickness should double
        String base = "wms?service=WMS&version=1.1.1&request=GetLegendGraphic"
                + "&layer=sf:states&style=uom"
                + "&format=image/png&width=20&height=20&scale=1000000&&legend_options=dpi:180";
        BufferedImage image = getAsImage(base, "image/png");
        
        assertPixel(image, 30, 10, Color.BLUE);
        assertPixel(image, 20, 20, Color.BLUE);
        assertPixel(image, 10, 30, Color.BLUE);
        assertPixel(image, 1, 20, Color.WHITE);
    }
    
}
