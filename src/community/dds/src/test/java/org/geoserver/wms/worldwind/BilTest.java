/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.junit.Test;


/**
 * Test case for producing Raw bil images out of an elevation model.
 * 
 * @author Tishampati Dhar
 * @since 2.0.x
 * 
 */

public class BilTest extends WMSTestSupport {
	/**
     * This is a READ ONLY TEST so we can use one time setup
     */
	
	public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName AUS_DEM = new QName(WCS_URI, "Ausdem", WCS_PREFIX);
    
    private RenderedImageMapOutputFormat rasterMapProducer;
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addStyle("raster", "raster.sld", BilTest.class, getCatalog());
        testData.addRasterLayer(AUS_DEM, "aus_dem.tiff", "tiff", null, BilTest.class, getCatalog());
        
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);
    }

    
    @Test
	public void testStandardRequest() throws Exception {
	    String layer = getLayerId(AUS_DEM);
	    
	    String request = "wms?service=wms&request=GetMap&version=1.1.1" +
		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=64&height=64" + 
		"&format=application/bil&srs=EPSG:4326";
		String response = getAsString(request);
		// Check response length in bytes
		assertEquals("testStandardRequest",9377,response.getBytes().length);
	    
	    
	    request = "wms?service=wms&request=GetMap&version=1.1.1" +
	    		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=64&height=64" + 
	    		"&format=application/bil8&srs=EPSG:4326";
	    response = getAsString(request);
	    // Check response length in bytes
	    assertEquals("testStandardRequest",4097,response.getBytes().length);
	    
	    request = "wms?service=wms&request=GetMap&version=1.1.1" +
		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=64&height=64" + 
		"&format=application/bil16&srs=EPSG:4326";
	    response = getAsString(request);
	    // Check response length in bytes
	    assertEquals("testStandardRequest",8193,response.getBytes().length);
	    
	    request = "wms?service=wms&request=GetMap&version=1.1.1" +
		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=64&height=64" + 
		"&format=application/bil32&srs=EPSG:4326";
	    response = getAsString(request);
	    // Check response length in bytes
	    assertEquals("testStandardRequest",16385,response.getBytes().length);
	}
	
    @Test
	public void testLargeRequest() throws Exception {
	    String layer = getLayerId(AUS_DEM);
	    String request = "wms?service=wms&request=GetMap&version=1.1.1" +
	    		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=600&height=600" + 
	    		"&format=image/bil&srs=EPSG:4326";
	    
	    String exceptstr  = getAsString(request);
	    assertTrue("testLargeRequest",exceptstr.contains("512x512"));
	}
	
	/**
	 * Need to override since we are in the community folder
	 */
	protected void copySchemaFile(String file) throws IOException {
        File f = new File("../../web/app/src/main/webapp/schemas/" + file);
        FileUtils.copyFile(f, getResourceLoader().createFile("WEB-INF/schemas/"+file));
    }
}
