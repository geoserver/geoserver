/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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

import com.mockrunner.mock.web.MockHttpServletResponse;

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

    private final int width = 64;
    private final int height = 64;

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
	public void testBil() throws Exception {
        byte[] response = getStandardRequest("application/bil");

        int expected = width * height * 2; // Native encoding, 2 bytes/pixel
        assertEquals("testStandardRequest", expected, response.length);
    }

    @Test
    public void testBil8() throws Exception {
        byte[] response = getStandardRequest("application/bil8");

        int expected = width * height; // 1 byte/pixel
        assertEquals("testStandardRequest", expected, response.length);
    }

    @Test
    public void testBil16() throws Exception {
        byte[] response = getStandardRequest("application/bil16");

        int expected = width * height * 2; // 2 bytes/pixel
        assertEquals("testStandardRequest", expected, response.length);
    }

    @Test
    public void testBil32() throws Exception {
        byte[] response = getStandardRequest("application/bil32");

        int expected = width * height * 4; // 4 bytes/pixel
        assertEquals("testStandardRequest", expected, response.length);
	}

    private byte[] getStandardRequest(String mimeType) throws Exception {
        String layer = getLayerId(AUS_DEM);
        String request = "wms?service=wms&request=GetMap&version=1.1.1" +
                "&layers=" + layer + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=64&height=64" +
                "&format=" + mimeType + "&srs=EPSG:4326";

      MockHttpServletResponse servletResponse = getAsServletResponse(request);
      return getBinary(servletResponse);
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
