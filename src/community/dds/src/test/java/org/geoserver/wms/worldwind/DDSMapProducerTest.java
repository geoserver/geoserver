/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapOutputFormatTest;
import org.junit.Before;
import org.junit.Test;

public class DDSMapProducerTest extends RenderedImageMapOutputFormatTest {

	private String mapFormat = "image/dds";
	
	public static String WMS_PREFIX = "wms";
    public static String WMS_URI = "http://www.opengis.net/wms/1.3.0";
    public static QName AUS_DEM = new QName(WMS_URI, "Ausdem", WMS_PREFIX);
    
	protected RenderedImageMapOutputFormat rasterMapProducer;

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new RenderedImageMapOutputFormat(this.mapFormat, getWMS());
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addStyle("raster", "raster.sld", DDSMapProducerTest.class, getCatalog());
        testData.addRasterLayer(AUS_DEM, "aus_dem.tif", "tiff", null, DDSMapProducerTest.class, getCatalog());
        
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);
    }
    
    @Test
	public void testStandardRequest() throws Exception {
	    String layer = getLayerId(AUS_DEM);
	    
	    String request = "wms?service=wms&request=GetMap&version=1.3.0" +
	    		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=512&height=512" + 
	    		"&format=image/dds&srs=EPSG:4326";
	    
	    String response  = getAsString(request);
	    // strange result, should be 512 * 512 (1 byte / px) * 133/100 (mipmap) = 348650 + 448 (header)
	    // request response tested on real geoserver are coherent with the statement above
	    assertTrue("testStandartRequest",response.getBytes().length==874009);
	    
	    request = "wms?service=wms&request=GetMap&version=1.3.0" +
	    		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=512&height=512" + 
	    		"&format=image/dds; format=DXT1&srs=EPSG:4326";
	    response  = getAsString(request);
	    
	    // strange result, should be 512 * 512 / 2 (0,5 byte / px) * 133/100 (mipmap) = 174325 + 448 (header)
	    // request response tested on real geoserver are coherent with the statement above
	    assertTrue("testStandartRequest",response.getBytes().length==349681);
	    
	    request = "wms?service=wms&request=GetMap&version=1.3.0" +
	    		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=512&height=512" + 
	    		"&format=image/dds; format=DXT3&srs=EPSG:4326";
	    
	    response  = getAsString(request);
	    // strange result, should be 512 * 512 (1 byte / px) * 133/100 (mipmap) = 348650 + 448 (header)
	    // request response tested on real geoserver are coherent with the statement above
	    assertTrue("testStandartRequest",response.getBytes().length==874009);
	    
	    request = "wms?service=wms&request=GetMap&version=1.3.0" +
	    		"&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=512&height=512" + 
	    		"&format=image/dds; format=ETC1&srs=EPSG:4326";
	    
	    response  = getAsString(request);
	    
	    // strange result, should be 512 * 512 / 2 (0,5 byte / px) * 133/100 (mipmap) = 174325 + 448 (header)
	    // request response tested on real geoserver are coherent with the statement above
	    assertTrue("testStandartRequest",response.getBytes().length==305985);
	}

    @Before
    public void setUpInternal() throws Exception {
	    this.rasterMapProducer = this.getProducerInstance();
	}
    
	public String getMapFormat()
	{
		return this.mapFormat;
	}

	protected void copySchemaFile(String file) throws IOException {
        File f = new File("../../web/app/src/main/webapp/schemas/" + file);
        FileUtils.copyFile(f, getResourceLoader().createFile("WEB-INF/schemas/"+file));
    }
}
