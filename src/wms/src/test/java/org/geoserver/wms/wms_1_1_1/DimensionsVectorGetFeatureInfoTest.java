/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DimensionsVectorGetFeatureInfoTest extends WMSDimensionsTestSupport {

    String baseFeatureInfo;

    XpathEngine xpath;

    @Before
    public void setXpahEngine() throws Exception {

        baseFeatureInfo = "wms?service=WMS&version=1.1.1&request=GetFeatureInfo&bbox=-180,-90,180,90"
                + "&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326&layers="
                + getLayerId(V_TIME_ELEVATION)
                + "&query_layers="
                + getLayerId(V_TIME_ELEVATION)
                + "&feature_count=50";

        xpath = XMLUnit.newXpathEngine();
    }

    /**
     * Ensures there is at most one feature at the specified location, and returns its feature id
     * 
     * @param baseFeatureInfo The GetFeatureInfo request, minus x and y
     * @param x
     * @param y
     * @return
     */
    String getFeatureAt(String baseFeatureInfo, int x, int y) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(baseFeatureInfo
                + "&info_format=application/vnd.ogc.gml&x=" + x + "&y=" + y);
        assertEquals("application/vnd.ogc.gml", response.getContentType());
        Document doc = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));
        String sCount = xpath.evaluate("count(//sf:TimeElevation)", doc);
        int count = Integer.valueOf(sCount);

        if (count == 0) {
            return null;
        } else if (count == 1) {
            return xpath.evaluate("//sf:TimeElevation/@fid", doc);
        } else {
            fail("Found more than one feature: " + count);
            return null; // just to make the compiler happy, fail throws an unchecked exception
        }
    }

    @Test
    public void testNoDimension() throws Exception {
        assertEquals("TimeElevation.0", getFeatureAt(baseFeatureInfo, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(baseFeatureInfo, 60, 10));
        assertEquals("TimeElevation.2", getFeatureAt(baseFeatureInfo, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testElevationDefault() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);

        // we should get only one square
        assertEquals("TimeElevation.0", getFeatureAt(baseFeatureInfo, 20, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 20, 30));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testElevationSingle() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        String base = baseFeatureInfo + "&elevation=1.0";

        // we should get only one square
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertNull(getFeatureAt(base, 60, 30));

    }

    @Test
    public void testElevationListMulti() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        String base = baseFeatureInfo + "&elevation=1.0,3.0";

        // we should get second and last
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Test
    public void testElevationListExtra() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        String base = baseFeatureInfo + "&elevation=1.0,3.0,5.0";

        // we should get second and last
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Test
    public void testElevationInterval() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        String base = baseFeatureInfo + "&elevation=1.0/3.0";

        // we should get all but the first
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertEquals("TimeElevation.2", getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Test
    public void testElevationIntervalResolution() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, 
                null, UNITS, UNIT_SYMBOL);
        String base = baseFeatureInfo + "&elevation=1.0/4.0/2.0";

        // we should get only one square
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Test
    public void testTimeDefault() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);

        // we should get only one square
        assertNull(getFeatureAt(baseFeatureInfo, 20, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testTimeCurrent() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=CURRENT";

        // we should get only one square
        assertNull(getFeatureAt(base, 20, 10));
        assertNull(getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testTimeSingle() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=2011-05-02";

        // we should get the second
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertNull(getFeatureAt(base, 60, 30));

    }

    @Test
    public void testTimeListMulti() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=2011-05-02,2011-05-04";

        // we should get the second and fourth
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Test
    public void testTimeListExtra() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        // adding a extra elevation that is simply not there, should not break
        String base = baseFeatureInfo + "&time=2011-05-02,2011-05-04,2011-05-10";

        // we should get the second and fourth
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Test
    public void testTimeInterval() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=2011-05-02/2011-05-05";

        // last three squares
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertEquals("TimeElevation.2", getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Ignore
    public void testTimeIntervalResolution() throws Exception {
        // adding a extra elevation that is simply not there, should not break
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=2011-05-01/2011-05-04/P2D";

        // first and third
        assertEquals("TimeElevation.0", getFeatureAt(base, 20, 10));
        assertNull(getFeatureAt(base, 60, 10));
        assertEquals("TimeElevation.2", getFeatureAt(base, 20, 30));
        assertNull(getFeatureAt(base, 60, 30));
    }

}
