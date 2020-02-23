/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.dimension.DefaultValueConfiguration;
import org.geoserver.wms.dimension.DefaultValueConfiguration.DefaultValuePolicy;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DimensionsVectorGetFeatureInfoTest extends WMSDynamicDimensionTestSupport {

    String baseFeatureInfo;

    @Before
    public void setup() throws Exception {
        baseFeatureInfo =
                "wms?service=WMS&version=1.1.1&request=GetFeatureInfo&bbox=-180,-90,180,90"
                        + "&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326&layers="
                        + getLayerId(V_TIME_ELEVATION)
                        + "&query_layers="
                        + getLayerId(V_TIME_ELEVATION)
                        + "&feature_count=50";
    }

    /**
     * Ensures there is at most one feature at the specified location, and returns its feature id
     *
     * @param baseFeatureInfo The GetFeatureInfo request, minus x and y
     */
    String getFeatureAt(String baseFeatureInfo, int x, int y) throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        baseFeatureInfo
                                + "&info_format=application/vnd.ogc.gml&x="
                                + x
                                + "&y="
                                + y);
        assertEquals("application/vnd.ogc.gml", response.getContentType());
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        // print(doc);
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
    public void testBothDimensionsStaticDefaults() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);

        assertNull(getFeatureAt(baseFeatureInfo, 20, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 20, 30));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testTimeDynamicRestriction() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(ResourceInfo.TIME, DefaultValuePolicy.LIMIT_DOMAIN));

        String request = baseFeatureInfo + "&elevation=1.0";

        assertNull(getFeatureAt(request, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(request, 60, 10));
        assertNull(getFeatureAt(request, 20, 30));
        assertNull(getFeatureAt(request, 60, 30));
    }

    @Test
    public void testTimeExpressionFull() throws Exception {
        // setup both dimensions, there is no equalSstrpimrinmatch records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN),
                new DefaultValueConfiguration(
                        ResourceInfo.TIME, "Concatenate('2011-05-0', round(elevation + 1))"));

        assertEquals("TimeElevation.0", getFeatureAt(baseFeatureInfo, 20, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 20, 30));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testTimeExpressionSingleElevation() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.TIME, "Concatenate('2011-05-0', round(elevation + 1))"));

        String request = baseFeatureInfo + "&elevation=1.0";

        assertNull(getFeatureAt(request, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(request, 60, 10));
        assertNull(getFeatureAt(request, 20, 30));
        assertNull(getFeatureAt(request, 60, 30));
    }

    @Test
    public void testElevationDynamicRestriction() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN));

        String request = baseFeatureInfo + "&time=2011-05-02";

        assertNull(getFeatureAt(request, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(request, 60, 10));
        assertNull(getFeatureAt(request, 20, 30));
        assertNull(getFeatureAt(request, 60, 30));
    }

    @Test
    public void testExplicitDefaultTime() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(ResourceInfo.TIME, DefaultValuePolicy.LIMIT_DOMAIN));

        String request = baseFeatureInfo + "&elevation=1.0&time=current";

        assertNull(getFeatureAt(request, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(request, 60, 10));
        assertNull(getFeatureAt(request, 20, 30));
        assertNull(getFeatureAt(request, 60, 30));
    }

    @Test
    public void testExplicitDefaultElevation() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN));

        String request = baseFeatureInfo + "&elevation=&time=2011-05-03";

        assertNull(getFeatureAt(request, 20, 10));
        assertNull(getFeatureAt(request, 60, 10));
        assertEquals("TimeElevation.2", getFeatureAt(request, 20, 30));
        assertNull(getFeatureAt(request, 60, 30));
    }
}
