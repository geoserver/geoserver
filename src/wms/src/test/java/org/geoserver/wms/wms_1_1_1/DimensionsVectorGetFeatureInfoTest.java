/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.geoserver.wms.featureinfo.VectorRenderingLayerIdentifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DimensionsVectorGetFeatureInfoTest extends WMSDimensionsTestSupport {

    String baseFeatureInfo;

    XpathEngine xpath;

    String baseFeatureInfoStacked;

    @After
    public void cleanup() {
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
    }

    @Before
    public void setXpahEngine() throws Exception {

        baseFeatureInfo =
                "wms?service=WMS&version=1.1.1&request=GetFeatureInfo&bbox=-180,-90,180,90"
                        + "&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326&layers="
                        + getLayerId(V_TIME_ELEVATION)
                        + "&query_layers="
                        + getLayerId(V_TIME_ELEVATION)
                        + "&feature_count=50";

        baseFeatureInfoStacked =
                "wms?service=WMS&version=1.1.1&request=GetFeatureInfo&bbox=-180,-90,180,90"
                        + "&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326&layers="
                        + getLayerId(V_TIME_ELEVATION_STACKED)
                        + "&query_layers="
                        + getLayerId(V_TIME_ELEVATION_STACKED)
                        + "&feature_count=1";

        xpath = XMLUnit.newXpathEngine();
    }

    /**
     * Ensures there is at most one feature at the specified location, and returns its feature id
     *
     * @param baseFeatureInfo The GetFeatureInfo request, minus x and y
     */
    String getFeatureAt(String baseFeatureInfo, int x, int y) throws Exception {
        return getFeatureAt(baseFeatureInfo, x, y, "sf:TimeElevation");
    }

    /**
     * Ensures there is at most one feature at the specified location, and returns its feature id
     *
     * @param baseFeatureInfo The GetFeatureInfo request, minus x and y
     */
    String getFeatureAt(String baseFeatureInfo, int x, int y, String typeName) throws Exception {
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
        String sCount = xpath.evaluate("count(//" + typeName + ")", doc);
        int count = Integer.valueOf(sCount);

        if (count == 0) {
            return null;
        } else if (count == 1) {
            return xpath.evaluate("//" + typeName + "/@fid", doc);
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
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);

        // we should get only one square
        assertEquals("TimeElevation.0", getFeatureAt(baseFeatureInfo, 20, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 20, 30));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testElevationSingle() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        String base = baseFeatureInfo + "&elevation=1.0";

        // we should get only one square
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertNull(getFeatureAt(base, 60, 30));
    }

    @Test
    public void testElevationListMulti() throws Exception {
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
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
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
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
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
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
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        String base = baseFeatureInfo + "&elevation=1.0/4.0/2.0";

        // we should get only one square
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Test
    public void testTimeDefault() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);

        // we should get only one square
        assertNull(getFeatureAt(baseFeatureInfo, 20, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 10));
        assertNull(getFeatureAt(baseFeatureInfo, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testTimeCurrent() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=CURRENT";

        // we should get only one square
        assertNull(getFeatureAt(base, 20, 10));
        assertNull(getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testTimeSingle() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=2011-05-02";

        // we should get the second
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertNull(getFeatureAt(base, 60, 30));
    }

    @Test
    public void testTimeSingleNoNearestClose() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=2011-05-02T012:00:00Z";

        // we should get none, as there is no nearest treatment
        assertNull(getFeatureAt(base, 20, 10));
        assertNull(getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertNull(getFeatureAt(base, 60, 30));
    }

    @Test
    public void testTimeSingleNearestClose() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        String base = baseFeatureInfo + "&time=2011-05-02T01:00:00Z";

        // we should get the second, it's the nearest
        assertNull(getFeatureAt(base, 20, 10));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-02T00:00:00.000Z");
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-02T00:00:00.000Z");
        assertNull(getFeatureAt(base, 20, 30));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-02T00:00:00.000Z");
        assertNull(getFeatureAt(base, 60, 30));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-02T00:00:00.000Z");
    }

    @Test
    public void testTimeSingleNearestAfter() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        String base = baseFeatureInfo + "&time=2013-05-02";

        // we should get the last, it's the nearest
        assertNull(getFeatureAt(base, 20, 10));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-04T00:00:00.000Z");
        assertNull(getFeatureAt(base, 60, 10));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-04T00:00:00.000Z");
        assertNull(getFeatureAt(base, 20, 30));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-04T00:00:00.000Z");
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-04T00:00:00.000Z");
    }

    @Test
    public void testTimeSingleNearestBefore() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(V_TIME_ELEVATION, ResourceInfo.TIME, true);
        String base = baseFeatureInfo + "&time=1190-05-02";

        // we should get the first, it's the nearest
        assertEquals("TimeElevation.0", getFeatureAt(base, 20, 10));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-01T00:00:00.000Z");
        assertNull(getFeatureAt(base, 60, 10));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-01T00:00:00.000Z");
        assertNull(getFeatureAt(base, 20, 30));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-01T00:00:00.000Z");
        assertNull(getFeatureAt(base, 60, 30));
        assertWarningCount(1);
        assertNearestTimeWarning(getLayerId(V_TIME_ELEVATION), "2011-05-01T00:00:00.000Z");
    }

    @Test
    public void testTimeSingleNearestBeforeBasicIdentifier() throws Exception {
        // a test for the old identifier, which someone might still be using for performance
        // purposes
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        try {
            testTimeSingleNearestBefore();
        } finally {
            VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        }
    }

    @Test
    public void testTimeListMulti() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=2011-05-02,2011-05-04";

        // we should get the second and fourth
        assertNull(getFeatureAt(base, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(base, 60, 10));
        assertNull(getFeatureAt(base, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(base, 60, 30));
    }

    @Test
    public void testTimeListExtra() throws Exception {
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
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
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
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
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        String base = baseFeatureInfo + "&time=2011-05-01/2011-05-04/P2D";

        // first and third
        assertEquals("TimeElevation.0", getFeatureAt(base, 20, 10));
        assertNull(getFeatureAt(base, 60, 10));
        assertEquals("TimeElevation.2", getFeatureAt(base, 20, 30));
        assertNull(getFeatureAt(base, 60, 30));
    }

    @Test
    public void testElevationDefaultAsRange() throws Exception {
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("1/3");
        setupResourceDimensionDefaultValue(
                V_TIME_ELEVATION, ResourceInfo.ELEVATION, defaultValueSetting, "elevation");

        // the last three show up, the first does not
        assertNull(getFeatureAt(baseFeatureInfo, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(baseFeatureInfo, 60, 10));
        assertEquals("TimeElevation.2", getFeatureAt(baseFeatureInfo, 20, 30));
        assertEquals("TimeElevation.3", getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testTimeDefaultAsRange() throws Exception {
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("2011-05-02/2011-05-03");
        setupResourceDimensionDefaultValue(
                V_TIME_ELEVATION, ResourceInfo.TIME, defaultValueSetting, "time");

        // the last three show up, the first does not
        assertNull(getFeatureAt(baseFeatureInfo, 20, 10));
        assertEquals("TimeElevation.1", getFeatureAt(baseFeatureInfo, 60, 10));
        assertEquals("TimeElevation.2", getFeatureAt(baseFeatureInfo, 20, 30));
        assertNull(getFeatureAt(baseFeatureInfo, 60, 30));
    }

    @Test
    public void testSortTimeElevationAscending() throws Exception {
        // check consistency with the visual output of GetMap
        assertEquals(
                "TimeElevationStacked.3",
                getFeatureAt(
                        baseFeatureInfoStacked + "&sortBy=time,elevation",
                        20,
                        10,
                        "sf:TimeElevationStacked"));
    }

    @Test
    public void testSortTimeElevationDescending() throws Exception {
        // check consistency with the visual output of GetMap
        assertEquals(
                "TimeElevationStacked.0",
                getFeatureAt(
                        baseFeatureInfoStacked + "&sortBy=time D,elevation D",
                        20,
                        10,
                        "sf:TimeElevationStacked"));
    }

    @Test
    public void testSortTimeElevationAscendingLegacyIdentifier() throws Exception {
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        assertEquals(
                "TimeElevationStacked.3",
                getFeatureAt(
                        baseFeatureInfoStacked + "&sortBy=time,elevation",
                        20,
                        10,
                        "sf:TimeElevationStacked"));
    }

    @Test
    public void testSortTimeElevationDescendingLegacyIdentifier() throws Exception {
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        assertEquals(
                "TimeElevationStacked.0",
                getFeatureAt(
                        baseFeatureInfoStacked + "&sortBy=time D,elevation D",
                        20,
                        10,
                        "sf:TimeElevationStacked"));
    }
}
