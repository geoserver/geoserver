/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.map.GIFMapResponse;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

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
    public void testElevationIntervalResolutionTooManyDefault() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST,
                null, UNITS, UNIT_SYMBOL);
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&elevation=0.0/4.0/0.01");

        assertEquals("application/vnd.ogc.se_xml", response.getContentType());
        Document dom = dom(response, true);
        // print(dom);
        String text = checkLegacyException(dom, ServiceException.INVALID_PARAMETER_VALUE, "elevation");
        assertThat(text, containsString("More than " + DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES + " elevations"));        
    }

    @Test
    public void testElevationIntervalResolutionTooManyCustom() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setMaxRequestedDimensionValues(2);
        gs.save(wms);
        try {
            // adding a extra elevation that is simply not there, should not break
            setupVectorDimension(
                    ResourceInfo.ELEVATION,
                    "elevation",
                    DimensionPresentation.LIST,
                    null,
                    UNITS,
                    UNIT_SYMBOL);
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wms?service=WMS&version=1.1.1&request=GetMap"
                                    + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                    + "&layers="
                                    + getLayerId(V_TIME_ELEVATION)
                                    + "&elevation=0.0/4.0/0.01");

            assertEquals("application/vnd.ogc.se_xml", response.getContentType());
            Document dom = dom(response, true);
            // print(dom);
            String text =
                    checkLegacyException(
                            dom, ServiceException.INVALID_PARAMETER_VALUE, "elevation");
            assertThat(
                    text,
                    containsString("More than 2 elevations"));
        } finally {
            wms.setMaxRequestedDimensionValues(
                    DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES);
            gs.save(wms);
        }
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
    
    @Test
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
    public void testTimeSingleNoNearestClose() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, ResourceInfo.TIME_UNIT, null);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=2011-05-02T01:00:00Z", "image/png");

        // not an exact match, should not get anything
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeSingleNearestClose() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, ResourceInfo.TIME_UNIT, null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=2011-05-02T01:00:00Z", "image/png");
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-02T00:00:00.000Z");

        // we should get only the second (nearest match)
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeSingleNearestAcceptableRange() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, ResourceInfo.TIME_UNIT, null);

        String baseURL = "wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION);

        // big enough range
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true, "P1D");
        getAsImage(baseURL + "&time=2011-05-02T01:00:00Z", "image/png");
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-02T00:00:00.000Z");

        // too small range, won't match
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true, "PT1M");
        getAsImage(baseURL + "&time=2011-05-02T01:00:00Z", "image/png");
        assertWarningCount(1);
        assertNoNearestWarning(getLayerId(V_TIME_ELEVATION), ResourceInfo.TIME);

        // big enough towards future
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true, "PT0M/P1D");
        getAsImage(baseURL + "&time=2011-05-02T01:00:00Z", "image/png");
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-03T00:00:00.000Z");
    }

    @Test
    public void testTimeSingleNearestAfter() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, ResourceInfo.TIME_UNIT, null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=2013-05-02", "image/png");

        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-04T00:00:00.000Z");

        // we should get only the last 
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testTimeSingleNearestBefore() throws Exception {
        // check it works the same if we enable nearest match
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, ResourceInfo.TIME_UNIT, null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=1990-05-02", "image/png");

        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-01T00:00:00.000Z");

        // we should get only the first
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeListMulti() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, ResourceInfo.TIME_UNIT, null);
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
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, ResourceInfo.TIME_UNIT, null);
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
                + "&bbox=-180,-90,180,90&styles=&width=80&height=40&srs=EPSG:4326"
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
    public void testTimeListAnimatedNonTransparent() throws Exception {
        // testing NON transparency in animated gif, with RED bgcolor
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION)
                + "&time=2011-05-02,2011-05-04,2011-05-10&format=" + GIFMapResponse.IMAGE_GIF_SUBTYPE_ANIMATED 
                + "&TRANSPARENT=false&BGCOLOR=0xff0000");

        // check we did not get a service exception
        assertEquals("image/gif", response.getContentType());
        
        // check it is a animated gif with three frames
        ByteArrayInputStream bis = getBinaryInputStream(response);
        ImageInputStream iis = ImageIO.createImageInputStream(bis);
        ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
        reader.setInput(iis);
        assertEquals(3, reader.getNumImages(true));
        
        // creating film strip to be able to test different frames of animated gif
        // http://stackoverflow.com/questions/18908217/losing-transparency-when-using-imageinputstream-and-bufferedimage-to-create-png
        int h = reader.getHeight(0);
        int w = reader.getWidth(0);
        int n = reader.getNumImages(true);
        BufferedImage image = new BufferedImage(w * n, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();       
        for (int i = 0; i < n; i++) {
            BufferedImage img = reader.read(i);
            g.drawImage(img, w * i, 0, null);     
            // want to see the individual frame images and the filmstrip? uncomment below
            //File outputfile = new File("/tmp/geoserveranimatednontransparentframe"+i+".gif");
            //ImageIO.write(img, "gif", outputfile);
        }
        //File outputfile = new File("/tmp/geoserveranimatedstripnontransparent.gif");
        //ImageIO.write(image, "gif", outputfile);
        
        // actual check for NON transparency and colored background
        assertPixel(image, 20, 10, Color.RED);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 100, 10, Color.RED);
        assertPixel(image, 140, 30, Color.BLACK);
    }
    
    @Test
    public void testTimeListAnimatedTransparent() throws Exception {
    	// testing transparency in animated gif
    	// note only by truly visual test you can test if animated gif is truly transparent
    	// note that in this test BGCOLOR should be white, else ALL is transparent
    	// note by uncommenting lines below you can see actual output
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION)
                + "&time=2011-05-02,2011-05-04,2011-05-10&format=" + GIFMapResponse.IMAGE_GIF_SUBTYPE_ANIMATED 
                + "&TRANSPARENT=true&BGCOLOR=0xfffff");

        // check we did not get a service exception
        assertEquals("image/gif", response.getContentType());
        
        // check it is a animated gif with three frames
        ByteArrayInputStream bis = getBinaryInputStream(response);
        ImageInputStream iis = ImageIO.createImageInputStream(bis);
        ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
        reader.setInput(iis);
        assertEquals(3, reader.getNumImages(true));
        
        // creating film strip to be able to test different frames of animated gif
        // http://stackoverflow.com/questions/18908217/losing-transparency-when-using-imageinputstream-and-bufferedimage-to-create-png
        int h = reader.getHeight(0);
        int w = reader.getWidth(0);
        int n = reader.getNumImages(true);
        BufferedImage image = new BufferedImage(w * n, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();       
        for (int i = 0; i < n; i++) {
            BufferedImage img = reader.read(i);
            g.drawImage(img, w * i, 0, null);     
            // want to see the individual frame images and the filmstrip? uncomment below
            //File outputfile = new File("/tmp/geoserveranimatedtransparentframe"+i+".gif");
           //ImageIO.write(img, "gif", outputfile);
        }
        //File outputfile = new File("/tmp/geoserveranimatedstriptransparent.gif");
        //ImageIO.write(image, "gif", outputfile);
               
        ColorModel cm = image.getColorModel();
        assertTrue(cm.hasAlpha());
        assertEquals(3, cm.getNumColorComponents()); 
        
        // actual check for transparency and color
        assertPixelIsTransparent(image, 20, 10);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixelIsTransparent(image, 100, 10);
        assertPixel(image, 140, 30, Color.BLACK);
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

    @Test
    public void testTimeIntervalResolutionTooManyDefault() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        MockHttpServletResponse response = getAsServletResponse("wms?service=WMS&version=1.1.1&request=GetMap"
                        + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                        + "&layers=" + getLayerId(V_TIME_ELEVATION) + "&time=2011-05-01/2011-06-01/PT1H");
        assertEquals("application/vnd.ogc.se_xml", response.getContentType());
        Document dom = dom(response, true);
        // print(dom);
        String text = checkLegacyException(dom, ServiceException.INVALID_PARAMETER_VALUE, "time");
        assertThat(text, containsString("More than " + DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES + " times"));
    }

    @Test
    public void testTimeIntervalResolutionTooManyCustom() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setMaxRequestedDimensionValues(2);
        gs.save(wms);
        try {
            setupVectorDimension(
                    ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wms?service=WMS&version=1.1.1&request=GetMap"
                                    + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                                    + "&layers="
                                    + getLayerId(V_TIME_ELEVATION)
                                    + "&time=2011-05-01/2011-05-04/P1D",
                            "image/png");

            assertEquals("application/vnd.ogc.se_xml", response.getContentType());
            Document dom = dom(response, true);
            // print(dom);
            String text =
                    checkLegacyException(dom, ServiceException.INVALID_PARAMETER_VALUE, "time");
            assertThat(text, containsString("More than 2 times"));
        } finally {
            wms.setMaxRequestedDimensionValues(
                    DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES);
            gs.save(wms);
        }
    }

    @Test 
    public void testElevationDefaultAsRange() throws Exception {
        // setup a default 
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("1/3");
        setupResourceDimensionDefaultValue(V_TIME_ELEVATION, ResourceInfo.ELEVATION, defaultValueSetting, "elevation");
        
        // request with default values
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION), "image/png");
        
        // RenderedImageBrowser.showChain(image);

        // the last three show up, the first does not
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    
    @Test 
    public void testTimeDefaultAsRange() throws Exception {
        // setup a default 
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("2011-05-02/2011-05-03");
        setupResourceDimensionDefaultValue(V_TIME_ELEVATION, ResourceInfo.TIME, defaultValueSetting, "time");
        
        // request with default values
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION), "image/png");
        
        // RenderedImageBrowser.showChain(image);

        // the last three show up, the first does not
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.WHITE);
    }
    
    @Test
    public void testSortAllAscending() throws Exception {
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION_STACKED) + "&sortBy=time, elevation",
                "image/png");

        // all blue
        assertPixel(image, 20, 10, Color.BLUE);
    }
    
    @Test
    public void testSortTimeAElevationD() throws Exception {
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION_STACKED) + "&sortBy=time, elevation D",
                "image/png");

        // all blue
        assertPixel(image, 20, 10, Color.GREEN);
    }
    
    @Test
    public void testSortTimeDElevationA() throws Exception {
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION_STACKED) + "&sortBy=time D, elevation",
                "image/png");

        // all blue
        assertPixel(image, 20, 10, Color.RED);
    }
    
    @Test
    public void testSortDescending() throws Exception {
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION_STACKED) + "&sortBy=time D,elevation D",
                "image/png");

        // all black
        assertPixel(image, 20, 10, Color.BLACK);
    }
    
    @Test
    public void testSortInvalidAttribute() throws Exception {
        Document dom = getAsDOM("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(V_TIME_ELEVATION_STACKED) + "&sortBy=foo");
        // print(dom);
        XpathEngine xp = XMLUnit.newXpathEngine();
        assertEquals(ServiceException.INVALID_PARAMETER_VALUE, xp.evaluate("/ServiceExceptionReport/ServiceException/@code", dom));
        assertEquals("sortBy", xp.evaluate("/ServiceExceptionReport/ServiceException/@locator", dom));
        assertThat(xp.evaluate("/ServiceExceptionReport/ServiceException", dom), containsString("'foo'"));
    }
    
    @Test
    public void testSortDescendingMultiLayer() throws Exception {
        BufferedImage image = getAsImage("wms?service=WMS&version=1.1.1&request=GetMap"
                + "&bbox=-180,-90,180,90&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326"
                + "&layers=" + getLayerId(SystemTestData.LAKES) + "," + getLayerId(V_TIME_ELEVATION_STACKED) + "&sortBy=()(time D,elevation D)",
                "image/png");

        // all black
        assertPixel(image, 20, 10, Color.BLACK);
    }
}
