/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.junit.After;
import org.junit.Before;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class WMSDimensionsTestSupport extends WMSTestSupport {

    protected static final long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;
    protected static final long MILLIS_IN_MINUTE = 60000;

    protected QName V_TIME_ELEVATION =
            new QName(MockData.SF_URI, "TimeElevation", MockData.SF_PREFIX);
    protected QName V_TIME_ELEVATION_EMPTY =
            new QName(MockData.SF_URI, "TimeElevationEmpty", MockData.SF_PREFIX);
    protected QName V_TIME_ELEVATION_STACKED =
            new QName(MockData.SF_URI, "TimeElevationStacked", MockData.SF_PREFIX);
    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static QName TIMERANGES =
            new QName(MockData.SF_URI, "timeranges", MockData.SF_PREFIX);
    protected static QName TIMESERIES =
            new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);

    protected static final String UNITS = "foot";
    protected static final String UNIT_SYMBOL = "ft";

    CoverageInfo wattemp;
    FeatureTypeInfo te, teEmpty;
    private CoverageInfo timeranges;

    @Before
    public void saveOriginalInfoObjects() throws Exception {
        wattemp = getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        timeranges = getCatalog().getCoverageByName(TIMERANGES.getLocalPart());
        te = getCatalog().getFeatureTypeByName(V_TIME_ELEVATION.getLocalPart());
        teEmpty = getCatalog().getFeatureTypeByName(V_TIME_ELEVATION_EMPTY.getLocalPart());
    }

    @After
    public void restoreOriginalInfoObjects() throws Exception {
        wattemp.getMetadata().clear();
        getCatalog().save(wattemp);
        timeranges.getMetadata().clear();
        getCatalog().save(timeranges);
        te.getMetadata().clear();
        getCatalog().save(te);
        teEmpty.getMetadata().clear();
        getCatalog().save(teEmpty);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("", "http://www.opengis.net/wms");
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
        namespaces.put("ows", "http://www.opengis.net/kml/2.2");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);

        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        getGeoServer().save(wms);

        // vector time-elevation
        Map map = new HashMap();
        map.put(MockData.KEY_STYLE, "TimeElevation");
        Catalog catalog = getCatalog();
        testData.addStyle(
                "TimeElevation", "TimeElevation.sld", WMSDimensionsTestSupport.class, catalog);
        testData.addVectorLayer(
                V_TIME_ELEVATION,
                map,
                "TimeElevation.properties",
                WMSDimensionsTestSupport.class,
                catalog);

        // vector time-elevation, emtpy
        testData.addVectorLayer(
                V_TIME_ELEVATION_EMPTY,
                map,
                "TimeElevationEmpty.properties",
                WMSDimensionsTestSupport.class,
                catalog);

        // vector time-elevation, stacked (all polys covering the whole planet)
        map.put(MockData.KEY_STYLE, "TimeElevationStacked");
        testData.addStyle(
                "TimeElevationStacked",
                "TimeElevationStacked.sld",
                WMSDimensionsTestSupport.class,
                catalog);
        testData.addVectorLayer(
                V_TIME_ELEVATION_STACKED,
                map,
                "TimeElevationStacked.properties",
                WMSDimensionsTestSupport.class,
                catalog);

        testData.addStyle(
                "temperature", "temperature.sld", WMSDimensionsTestSupport.class, catalog);
        Map propertyMap = new HashMap();
        propertyMap.put(LayerProperty.STYLE, "temperature");
        // a raster layer with times
        testData.addRasterLayer(
                TIMESERIES, "timeseries.zip", null, null, SystemTestData.class, catalog);
        // a raster layer with time and elevation
        testData.addRasterLayer(
                WATTEMP, "watertemp.zip", null, propertyMap, SystemTestData.class, catalog);
        // a raster layer with time, elevation and custom dimensions as ranges
        testData.addRasterLayer(
                TIMERANGES, "timeranges.zip", null, null, SystemTestData.class, catalog);
    }

    /** Checks two dates are the same, within a given tolerance. */
    protected static void assertDateEquals(java.util.Date d1, java.util.Date d2, long tolerance) {
        long difference = Math.abs(d1.getTime() - d2.getTime());
        assertTrue(difference <= tolerance);
    }

    protected void setupVectorDimension(
            String metadata,
            String attribute,
            DimensionPresentation presentation,
            Double resolution,
            String units,
            String unitSymbol) {
        setupVectorDimension(
                "TimeElevation", metadata, attribute, presentation, resolution, units, unitSymbol);
    }

    protected void setupResourceDimensionDefaultValue(
            QName name,
            String dimensionName,
            DimensionDefaultValueSetting defaultValue,
            String... startEndAttribute) {
        ResourceInfo info = getCatalog().getResourceByName(name.getLocalPart(), ResourceInfo.class);
        if (info == null) {
            throw new RuntimeException("Unable to get resource by name " + name.getLocalPart());
        }
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(DimensionPresentation.LIST);
        di.setDefaultValue(defaultValue);
        if (startEndAttribute != null && startEndAttribute.length > 0) {
            di.setAttribute(startEndAttribute[0]);
            if (startEndAttribute.length > 1) {
                di.setEndAttribute(startEndAttribute[1]);
            }
        }
        info.getMetadata().put(dimensionName, di);
        getCatalog().save(info);
    }

    /** Checks that the last HTTP response had the expected number of "Warning" headers */
    protected void assertWarningCount(int expectedValue) {
        MockHttpServletResponse response = getLastResponse();
        List<Object> values = response.getHeaderValues(HttpHeaders.WARNING);
        assertNotNull(values);
        assertEquals(
                "Expected to find a different number of warnings", expectedValue, values.size());
    }

    /**
     * Asserts that the specified nearest value has been used and check the corresponding HTTP
     * warning
     */
    protected void assertNearestTimeWarning(String layerId, String expectedValue) {
        String expected =
                "99 Nearest value used: time="
                        + expectedValue
                        + " "
                        + ResourceInfo.TIME_UNIT
                        + " ("
                        + layerId
                        + ")";

        MockHttpServletResponse response = getLastResponse();
        List<Object> values = response.getHeaderValues(HttpHeaders.WARNING);
        Object found = values.stream().filter(v -> expected.equals(v)).findFirst().orElse(null);

        assertNotNull(
                "Could not find\n" + expected + "\n among the following warnings:\n" + values,
                found);
    }

    /**
     * Asserts that the specified nearest value has been used and check the corresponding HTTP
     * warning
     */
    protected void assertNoNearestWarning(String layerId, String dimension) {
        String expected = "99 No nearest value found on " + layerId + ": " + dimension;

        MockHttpServletResponse response = getLastResponse();
        List<Object> values = response.getHeaderValues(HttpHeaders.WARNING);
        Object found = values.stream().filter(v -> expected.equals(v)).findFirst().orElse(null);

        assertNotNull(
                "Could not find\n" + expected + "\n among the following warnings:\n" + values,
                found);
    }
}
