/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;

public class DimensionsRasterGetMapTest extends WMSDimensionsTestSupport {
    
    final static String BASE_URL = "wms?service=WMS&version=1.1.0" +
        "&request=GetMap&layers=watertemp&styles=" +
        "&bbox=0.237,40.562,14.593,44.558&width=200&height=80" +
        "&srs=EPSG:4326&format=image/png";
    final static String MIME = "image/png";
    
    public void testNoDimension() throws Exception {
        BufferedImage image = getAsImage(BASE_URL, MIME);
        
        // the result is really just the result of how the tiles are setup in the mosaic, 
        // but since they overlap with each other we just want to check the image is not
        // empty
        assertNotBlank("water temperature", image);
    }

    public void testDefaultValues() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        BufferedImage image = getAsImage(BASE_URL, "image/png");

        // should be light red pixel and the first pixel is there only at the default elevation
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 185, 185));
    }
    
    public void testElevation() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        BufferedImage image = getAsImage(BASE_URL + "&elevation=100", "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0,0,0));
        // and this one a light blue
        assertPixel(image, 68, 72, new Color(246, 246, 255));
    }
    
    public void testTime() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        BufferedImage image = getAsImage(BASE_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));
    }
    
    public void testTimeTwice() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        BufferedImage image = getAsImage(BASE_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));
        
        image = getAsImage(BASE_URL + "&time=2008-10-31T00:00:00.000Z", "image/png");

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));

    }
    
    public void testTimeElevation() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        BufferedImage image = getAsImage(BASE_URL + "&time=2008-10-31T00:00:00.000Z&elevation=100", "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0,0,0));
        // and this one a light blue, but slightly darker than before
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }
    
    
}
