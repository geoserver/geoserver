/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DimensionsRasterGetFeatureInfoTest extends WMSDimensionsTestSupport {
    
    static final String BASE_URL = "wms?service=WMS&version=1.1.0&request=GetFeatureInfo" +
        "&layers=watertemp&styles=&bbox=0.237,40.562,14.593,44.558&width=200&height=80" +
        "&srs=EPSG:4326&format=image/png" +
        "&query_layers=watertemp&feature_count=50";
    
    static final double EPS = 1e-03;
    
    private XpathEngine xpath;
    
    @Before
    public void setXpathEngine() throws Exception {            
        xpath = XMLUnit.newXpathEngine();
    };
    
    /**
     * Ensures there is at most one feature at the specified location, and returns its feature id
     * 
     * @param baseFeatureInfo The GetFeatureInfo request, minus x and y
     * @param x
     * @param y
     * @return
     */
    Double getFeatureAt(String baseFeatureInfo, int x, int y) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(baseFeatureInfo
                + "&info_format=application/vnd.ogc.gml&x=" + x + "&y=" + y);
        assertEquals("application/vnd.ogc.gml", response.getContentType());
        Document doc = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));
        String sCount = xpath.evaluate("count(//sf:watertemp)", doc);
        int count = Integer.valueOf(sCount);

        if (count == 0) {
            return null;
        } else if (count == 1) {
            return Double.valueOf(xpath.evaluate("//sf:watertemp/sf:GRAY_INDEX", doc));
        } else {
            fail("Found more than one feature: " + count);
            return null; // just to make the compiler happy, fail throws an unchecked exception
        }
    }
    
    @Test 
    public void testDefaultValues() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        // this one should be medium
        assertEquals(14.51, getFeatureAt(BASE_URL, 36, 31), EPS);
        // this one hot
        assertEquals(19.15, getFeatureAt(BASE_URL, 68, 72), EPS);
    }
    
    @Test 
    public void testElevation() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        // this one should be the no-data
        String url = BASE_URL + "&elevation=100";
        assertEquals(-30000, getFeatureAt(url, 36, 31), EPS);
        // and this one should be medium
        assertEquals(14.492, getFeatureAt(url, 68, 72), EPS);
    }
    
    @Test
    public void testTime() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z";

        // should be similar to the default, but with different shades of color
        assertEquals(14.592, getFeatureAt(url, 36, 31), EPS);
        assertEquals(19.371, getFeatureAt(url, 68, 72), EPS);
    }
    
    @Test
    public void testTimeElevation() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z&elevation=100";
        // this one should be the no-data
        assertEquals(-30000, getFeatureAt(url, 36, 31), EPS);
        // and this one should be medium
        assertEquals(14.134, getFeatureAt(url, 68, 72), EPS);
    }
    
    
}
