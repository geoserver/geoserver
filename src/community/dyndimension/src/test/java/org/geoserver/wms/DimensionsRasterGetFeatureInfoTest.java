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

public class DimensionsRasterGetFeatureInfoTest extends WMSDynamicDimensionTestSupport {

    static final String CUSTOM = "CUSTOM";

    static final String CUSTOM_KEY = ResourceInfo.CUSTOM_DIMENSION_PREFIX + CUSTOM;

    static final double EPS = 1e-03;

    String baseFeatureInfo;

    String layerId;

    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }

    @Before
    public void setupRequest() {
        layerId = getLayerId(TIME_ELEVATION_CUSTOM);
        baseFeatureInfo =
                "wms?service=WMS&version=1.1.0&request=GetFeatureInfo"
                        + "&layers="
                        + layerId
                        + "&styles=&bbox=-180,-90,180,90&width=180&height=90"
                        + "&srs=EPSG:4326&format=image/png"
                        + "&query_layers="
                        + layerId
                        + "&feature_count=50";
    }

    /**
     * Ensures there is at most one feature at the specified location, and returns its feature id
     *
     * @param baseFeatureInfo The GetFeatureInfo request, minus x and y
     * @param layerName TODO
     */
    Double getValueAt(String baseFeatureInfo, int x, int y) throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        baseFeatureInfo
                                + "&info_format=application/vnd.ogc.gml&x="
                                + x
                                + "&y="
                                + y);
        assertEquals("application/vnd.ogc.gml", response.getContentType());
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        String sCount = xpath.evaluate("count(//" + layerId + ")", doc);
        int count = Integer.valueOf(sCount);

        if (count == 0) {
            return null;
        } else if (count == 1) {
            return Double.valueOf(xpath.evaluate("//" + layerId + "/sf:GRAY_INDEX", doc));
        } else {
            fail("Found more than one feature: " + count);
            return null; // just to make the compiler happy, fail throws an unchecked exception
        }
    }

    @Test
    public void testDefaultValues() throws Exception {
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                null,
                null);
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM, "custom", DimensionPresentation.LIST, null, null, null);

        // we should be getting nothing at all, the three defaults do not fit
        assertNull(getValueAt(baseFeatureInfo, 90, 45));
    }

    @Test
    public void testTimeCustomDomainRestriction() throws Exception {
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                null,
                null);
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM, CUSTOM_KEY, DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                TIME_ELEVATION_CUSTOM,
                new DefaultValueConfiguration(ResourceInfo.TIME, DefaultValuePolicy.LIMIT_DOMAIN),
                new DefaultValueConfiguration(CUSTOM, DefaultValuePolicy.LIMIT_DOMAIN));

        // elevation defaults to 0, the other two should follow
        assertEquals(0, getValueAt(baseFeatureInfo, 90, 45), 0d);

        // force elevation to 1
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1", 90, 45), 0d);

        // force elevation to 2
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2", 90, 45), 0d);

        // force both elevation and time, in both valid and invalid combinations
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1&time=2008-11-01", 90, 45), 0d);
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2&time=2008-11-02", 90, 45), 0d);
        assertNull(getValueAt(baseFeatureInfo + "&elevation=1&time=2008-11-02", 90, 45));

        // force both elevation and custom dimension, in both valid and invalid combinations
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1&dim_custom=AB2", 90, 45), 0d);
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2&dim_custom=AB3", 90, 45), 0d);
        assertNull(getValueAt(baseFeatureInfo + "&elevation=1&dim_custom=AB3", 90, 45));
    }

    @Test
    public void testTimeExpression() throws Exception {
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                null,
                null);
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM, CUSTOM_KEY, DimensionPresentation.LIST, null, null, null);
        String expression =
                "if_then_else(equalTo(CUSTOM, 'AB1'), '2008-10-31', Concatenate('2008-11-0', round(strSubstringStart(CUSTOM, 2) - 1)))";
        setupDynamicDimensions(
                TIME_ELEVATION_CUSTOM,
                new DefaultValueConfiguration(CUSTOM, DefaultValuePolicy.LIMIT_DOMAIN),
                new DefaultValueConfiguration(ResourceInfo.TIME, expression));

        // elevation defaults to 0, the other two should follow
        assertEquals(0, getValueAt(baseFeatureInfo, 90, 45), 0d);

        // force elevation to 1
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1", 90, 45), 0d);

        // force elevation to 2
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2", 90, 45), 0d);

        // force both elevation and time, in both valid and invalid combinations
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1&time=2008-11-01", 90, 45), 0d);
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2&time=2008-11-02", 90, 45), 0d);
        assertNull(getValueAt(baseFeatureInfo + "&elevation=1&time=2008-11-02", 90, 45));

        // force both elevation and custom dimension, in both valid and invalid combinations
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1&dim_custom=AB2", 90, 45), 0d);
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2&dim_custom=AB3", 90, 45), 0d);
        assertNull(getValueAt(baseFeatureInfo + "&elevation=1&dim_custom=AB3", 90, 45));
    }

    @Test
    public void testCustomDomainExpression() throws Exception {
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                null,
                null);
        setupRasterDimension(
                TIME_ELEVATION_CUSTOM, CUSTOM_KEY, DimensionPresentation.LIST, null, null, null);
        String expression = "Concatenate('AB', round(elevation + 1))";
        setupDynamicDimensions(
                TIME_ELEVATION_CUSTOM,
                new DefaultValueConfiguration(ResourceInfo.TIME, DefaultValuePolicy.LIMIT_DOMAIN),
                new DefaultValueConfiguration(CUSTOM, expression));

        // elevation defaults to 0, the other two should follow
        assertEquals(0, getValueAt(baseFeatureInfo, 90, 45), 0d);

        // force elevation to 1
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1", 90, 45), 0d);

        // force elevation to 2
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2", 90, 45), 0d);

        // force both elevation and time, in both valid and invalid combinations
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1&time=2008-11-01", 90, 45), 0d);
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2&time=2008-11-02", 90, 45), 0d);
        assertNull(getValueAt(baseFeatureInfo + "&elevation=1&time=2008-11-02", 90, 45));

        // force both elevation and custom dimension, in both valid and invalid combinations
        assertEquals(100, getValueAt(baseFeatureInfo + "&elevation=1&dim_custom=AB2", 90, 45), 0d);
        assertEquals(200, getValueAt(baseFeatureInfo + "&elevation=2&dim_custom=AB3", 90, 45), 0d);
        assertNull(getValueAt(baseFeatureInfo + "&elevation=1&dim_custom=AB3", 90, 45));
    }

    //
    // @Test
    // public void testElevation() throws Exception {
    // setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null,
    // UNITS, UNIT_SYMBOL);
    // setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null,
    // null);
    //
    // // this one should be the no-data
    // String url = BASE_URL + "&elevation=100";
    // assertEquals(-30000, getValueAt(url, 36, 31, "sf:watertemp"), EPS);
    // // and this one should be medium
    // assertEquals(14.492, getValueAt(url, 68, 72, "sf:watertemp"), EPS);
    // }
    //
    // @Test
    // public void testTime() throws Exception {
    // setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null,
    // UNITS, UNIT_SYMBOL);
    // setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null,
    // null);
    //
    // String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z";
    //
    // // should be similar to the default, but with different shades of color
    // assertEquals(14.592, getValueAt(url, 36, 31, "sf:watertemp"), EPS);
    // assertEquals(19.371, getValueAt(url, 68, 72, "sf:watertemp"), EPS);
    // }
    //
    // @Test
    // public void testTimeElevation() throws Exception {
    // setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null,
    // UNITS, UNIT_SYMBOL);
    // setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null,
    // null);
    //
    // String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z&elevation=100";
    // // this one should be the no-data
    // assertEquals(-30000, getValueAt(url, 36, 31, "sf:watertemp"), EPS);
    // // and this one should be medium
    // assertEquals(14.134, getValueAt(url, 68, 72, "sf:watertemp"), EPS);
    // }
    //
    // @Test
    // public void testTimeRange() throws Exception {
    // setupRasterDimension(TIMERANGES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null,
    // null);
    // setupRasterDimension(TIMERANGES, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null,
    // UNITS, UNIT_SYMBOL);
    // setupRasterDimension(TIMERANGES, "wavelength", DimensionPresentation.LIST, null, null, null);
    // setupRasterDimension(TIMERANGES, "date", DimensionPresentation.LIST, null, null, null);
    //
    // String layer = getLayerId(TIMERANGES);
    // String baseUrl = "wms?LAYERS=" + layer +
    // "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&SRS=EPSG:4326"
    // +
    // "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&query_layers="
    // + layer;
    //
    // // last range
    // String url = baseUrl + "&TIME=2008-11-05T00:00:00.000Z/2008-11-06T12:00:00.000Z";
    // assertEquals(-30000, getValueAt(url, 36, 31, layer), EPS);
    // assertEquals(14.782, getValueAt(url, 68, 72, layer), EPS);
    //
    // // in the middle hole, no data
    // url = baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z";
    // assertNull(getValueAt(url, 36, 31, layer));
    //
    // // first range
    // url = baseUrl + "&TIME=2008-10-31T12:00:00.000Z/2008-10-31T16:00:00.000Z";
    // assertEquals(-30000, getValueAt(url, 36, 31, layer), EPS);
    // assertEquals(20.027, getValueAt(url, 68, 72, layer), EPS);
    // }
    //

}
