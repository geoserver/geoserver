/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.image.ImageWorker;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetMapIntegrationTest extends WMSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs10RasterLayers();
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog=getCatalog();
        testData.addStyle("indexed","indexed.sld",getClass(),catalog);
 
        Map props = new HashMap();
        props.put(LayerProperty.STYLE, "indexed");
        
        testData.addRasterLayer(new QName(MockData.SF_URI, "indexed", MockData.SF_PREFIX), 
                "indexed.tif", "tif", props,getClass(),catalog);

        props.put(LayerProperty.STYLE, "raster");
        
        testData.addRasterLayer(new QName(MockData.SF_URI, "paletted", MockData.SF_PREFIX), 
                "paletted.tif", "tif", props,getClass(),catalog);

        testData.addRasterLayer(new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX), 
                "raster-filter-test.zip",null, props,SystemTestData.class, catalog);

        testData.addRasterLayer(new QName(MockData.SF_URI, "fourbits", MockData.SF_PREFIX), 
                "fourbits.zip", null, props,SystemTestData.class,catalog);
        
    }
    
    @Test
    public void testIndexed() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?LAYERS=sf:indexed&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=100,78,104,80&WIDTH=300&HEIGHT=150");

        assertEquals("image/png", response.getContentType());

        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        image = new ImageWorker(image).forceComponentColorModel().getRenderedImage();
        ImageAssert.assertEquals(new File(
                "src/test/resources/org/geoserver/wms/map/indexed-expected.png"), image, 0);
    }

    @Test
    public void testIndexedBlackBG() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bgcolor=0x000000&LAYERS=sf:indexed&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=100,78,104,80&WIDTH=300&HEIGHT=150&transparent=false");

        assertEquals("image/png", response.getContentType());

        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        ImageAssert.assertEquals(new File(
                "src/test/resources/org/geoserver/wms/map/indexed-bg-expected.png"), image, 0);
    }
    
    @Test
    public void testRasterFilterRed() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
        "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false&CQL_FILTER=location like 'red%25'");
        
        assertEquals("image/png", response.getContentType());
        
        // check we got the 
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(255, pixel[0]);
        assertEquals(0, pixel[1]);
        assertEquals(0, pixel[2]);
    }
    
    @Test
    public void testRasterFilterGreen() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
        "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false&CQL_FILTER=location like 'green%25'");
        
        assertEquals("image/png", response.getContentType());
        
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(0, pixel[0]);
        assertEquals(255, pixel[1]);
        assertEquals(0, pixel[2]);
    }
    
    @Test
    public void testMosaicTwice() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
        "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false");
        
        assertEquals("image/png", response.getContentType());
        
        response = getAsServletResponse("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
        "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false");
        
        assertEquals("image/png", response.getContentType());
    }
    
    @Test
    public void testIndexedTransparency() throws Exception {
        String request = "wms?LAYERS=sf:paletted&STYLES=&FORMAT=image%2Fpng&SERVICE=WMS" +
        		"&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A3174" +
        		"&BBOX=-3256153.625,826440.25,-2756153.625,1326440.25" +
        		"&WIDTH=256&HEIGHT=256&transparent=true";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());
        
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        assertTrue(image.getColorModel().hasAlpha());

        int[] rgba = new int[4];
        // transparent pixel in the top left corner
        image.getData().getPixel(0, 0, rgba);
        assertEquals(0, (int) rgba[3]);
        // solid pixel in the lower right corner
        image.getData().getPixel(255, 255, rgba);
        assertEquals(255, (int) rgba[3]);
    }
    
    @Test
    public void testFourBits() throws Exception {
        String request = "wms?LAYERS=sf:fourbits&STYLES=&FORMAT=image/png" +
        		"&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4269" +
        		"&BBOX=-118.58930224611,45.862378906251,-118.33030957033,45.974688476563" +
        		"&WIDTH=761&HEIGHT=330";
        
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());
    }
    
    /**
     * http://jira.codehaus.org/browse/GEOS-4893, make meta-tiler work
     * with WMS 1.3 as well
     * @throws Exception 
     */
    @Test
    public void testMetaWMS13() throws Exception {
        String wms11 = "wms?LAYERS=cite%3ALakes&STYLES=&FORMAT=image%2Fpng&TILED=true&TILESORIGIN=0.0006%2C-0.0018" +
        		"&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&BBOX=0.0006,-0.0018,0.0031,0.0007&WIDTH=256&HEIGHT=256";
        String wms13 = "wms?LAYERS=cite%3ALakes&STYLES=&FORMAT=image%2Fpng&TILED=true&TILESORIGIN=-0.0018%2C0.0006" +
        		"&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS=EPSG%3A4326&BBOX=-0.0018,0.0006,0.0007,0.0031&WIDTH=256&HEIGHT=256";

        BufferedImage image11 = getAsImage(wms11, "image/png");
        BufferedImage image13 = getAsImage(wms13, "image/png");
        
        // compare the general structure
        assertEquals(image11.getWidth(), image13.getWidth());
        assertEquals(image11.getHeight(), image13.getHeight());
        assertEquals(image11.getColorModel(), image13.getColorModel());
        assertEquals(image11.getSampleModel(), image13.getSampleModel());
        // compare the actual data
        DataBufferByte db11 = (DataBufferByte) image11.getData().getDataBuffer();
        DataBufferByte db13 = (DataBufferByte) image13.getData().getDataBuffer();
        byte[][] bankData11 = db11.getBankData();
        byte[][] bankData13 = db13.getBankData();
        for (int i = 0; i < bankData11.length; i++) {
            assertTrue(Arrays.equals(bankData11[i], bankData13[i]));
        }
    }
    
    @Test
    public void testOpenLayersProxy() throws Exception {
        NamespaceContext oldContext = XMLUnit.getXpathNamespaceContext();
        try {
            Map<String, String> namespaces = new HashMap<String, String>();
            namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
            registerNamespaces(namespaces);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
            
            // setup a proxy base url
            GeoServerInfo global = getGeoServer().getGlobal();
            global.getSettings().setProxyBaseUrl("http://www.geoserver.org:1234/gs");
            getGeoServer().save(global);
            
            Document dom = getAsDOM("wms?LAYERS=sf:indexed&STYLES=&FORMAT=application/openlayers&SERVICE=WMS&VERSION=1.1.1"
                    + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=100,78,104,80&WIDTH=300&HEIGHT=150");
    
            assertXpathEvaluatesTo("http://www.geoserver.org:1234/gs/openlayers/OpenLayers.js", 
                    "//xhtml:script[contains(@src, 'OpenLayers.js')]/@src", dom);
        } finally {
            XMLUnit.setXpathNamespaceContext(oldContext);
        }
    }
}
