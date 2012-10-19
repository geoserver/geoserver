/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.geoserver.wms.map.GIFMapResponse;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DimensionsVectorGetMapTest extends WMSDimensionsTestSupport {

    @Test
    public void testNoDimension() throws Exception {
        BufferedImage image = getAsImage(
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90"
                        + "&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326&layers="
                        + getLayerId(V_TIME_ELEVATION), "image/png");

        // we should get everything black, all four squares
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test 
    public void testElevationDefault() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION), "image/png");

        // we should get only the first
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testElevationSingle() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&elevation=1.0", "image/png");

        // we should get only the second
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testElevationListMulti() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&elevation=1.0,3.0", "image/png");

        // we should get second and third
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testElevationListExtra() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&elevation=1.0,3.0,5.0", "image/png");

        // we should get only second and third
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testElevationInterval() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&elevation=1.0/3.0", "image/png");

        // we should get last three
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test 
    public void testElevationIntervalResolution() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&elevation=0.0/4.0/2.0", "image/png");

        // we should get second and fourth
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeDefault() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION), "image/png");

        // we should get only the last one (current)
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeCurrent() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=CURRENT", "image/png");

        // we should get only the last one (current)
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }
    
    public void testTimeCurrentForEmptyLayer() throws Exception {
        setupVectorDimension("TimeElevationEmpty", ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION_EMPTY) + "&time=CURRENT", "image/png");

        // we should get only the last one (current)
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeSingle() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=2011-05-02", "image/png");

        // we should get only the second
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeListMulti() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=2011-05-02,2011-05-04",
                "image/png");

        // we should get only second and fourth
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeListExtra() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION)
                + "&time=2011-05-02,2011-05-04,2011-05-10", "image/png");

        // we should get only second and fourth
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }
    
    @Test
    public void testTimeListAnimated() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION)
                + "&time=2011-05-02,2011-05-04,2011-05-10&format=" + GIFMapResponse.IMAGE_GIF_SUBTYPE_ANIMATED);

        // check we did not get a service exception
        assertEquals("image/gif", response.getContentType());
        // check it is a animated gif withthree frames
        ByteArrayInputStream bis = getBinaryInputStream(response);
        ImageInputStream iis = ImageIO.createImageInputStream(bis);
        ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
        reader.setInput(iis);
        assertEquals(3, reader.getNumImages(true));
    }

    @Test
    public void testTimeInterval() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=2011-05-02/2011-05-05",
                "image/png");

        // last three
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeIntervalResolution() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=2011-05-01/2011-05-04/P2D",
                "image/png");

        // first and third
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.WHITE);
    }

}
