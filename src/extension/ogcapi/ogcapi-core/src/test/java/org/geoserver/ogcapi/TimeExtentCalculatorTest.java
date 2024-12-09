/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geoserver.catalog.DimensionPresentation.LIST;
import static org.geoserver.catalog.ResourceInfo.TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.Converters;
import org.geotools.util.DateRange;
import org.junit.Before;
import org.junit.Test;

public class TimeExtentCalculatorTest extends GeoServerSystemTestSupport {

    protected QName V_TIME_ELEVATION =
            new QName(MockData.SF_URI, "TimeElevation", MockData.SF_PREFIX);

    protected QName V_TIME_ELEVATION_EMPTY =
            new QName(MockData.SF_URI, "TimeElevationEmpty", MockData.SF_PREFIX);

    protected static QName TIMESERIES =
            new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        Catalog catalog = getCatalog();

        testData.addVectorLayer(
                V_TIME_ELEVATION,
                null,
                "TimeElevation.properties",
                TimeExtentCalculatorTest.class,
                catalog);

        testData.addVectorLayer(
                V_TIME_ELEVATION_EMPTY,
                null,
                "TimeElevationEmpty.properties",
                TimeExtentCalculatorTest.class,
                catalog);
    }

    @Before
    public void cleanupDimensions() throws Exception {

        getTestData()
                .addRasterLayer(
                        TIMESERIES,
                        "timeseries.zip",
                        null,
                        null,
                        SystemTestData.class,
                        getCatalog());

        Catalog catalog = getCatalog();
        List<ResourceInfo> resources = catalog.getResources(ResourceInfo.class);
        for (ResourceInfo resource : resources) {
            if (resource.getMetadata().containsKey(TIME)) {
                resource.getMetadata().remove(TIME);
                catalog.save(resource);
            }
        }
    }

    @Test
    public void testVectorTimeMissing() throws Exception {
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(V_TIME_ELEVATION));
        assertNull(TimeExtentCalculator.getTimeExtent(ft));
    }

    @Test
    public void testRasterTimeMissing() throws Exception {
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(TIMESERIES));
        assertNull(TimeExtentCalculator.getTimeExtent(ci));
    }

    @Test
    public void testVectorTimeDisabled() throws Exception {
        setupVectorDimension(getLayerId(V_TIME_ELEVATION), TIME, "time", LIST, null, null, null);
        Catalog catalog = getCatalog();
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(getLayerId(V_TIME_ELEVATION));
        DimensionInfo di = ft.getMetadata().get(TIME, DimensionInfo.class);
        di.setEnabled(false);
        catalog.save(ft);
        assertNull(TimeExtentCalculator.getTimeExtent(ft));
    }

    @Test
    public void testRasterTimeDisabled() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, LIST, null, null, null);
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TIMESERIES));
        DimensionInfo di = ci.getMetadata().get(TIME, DimensionInfo.class);
        di.setEnabled(false);
        catalog.save(ci);
        assertNull(TimeExtentCalculator.getTimeExtent(ci));
    }

    @Test
    public void testVectorTimeEmpty() throws Exception {
        setupVectorDimension(
                getLayerId(V_TIME_ELEVATION_EMPTY), TIME, "time", LIST, null, null, null);
        Catalog catalog = getCatalog();
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(getLayerId(V_TIME_ELEVATION_EMPTY));
        catalog.save(ft);
        assertNull(TimeExtentCalculator.getTimeExtent(ft));
    }

    @Test
    public void testVectorTime() throws Exception {
        setupVectorDimension(getLayerId(V_TIME_ELEVATION), TIME, "time", LIST, null, null, null);
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(V_TIME_ELEVATION));
        DateRange extent = TimeExtentCalculator.getTimeExtent(ft);
        assertEquals(Converters.convert("2011-05-01Z", Date.class), extent.getMinValue());
        assertEquals(Converters.convert("2011-05-04Z", Date.class), extent.getMaxValue());
    }

    @Test
    public void testRasterTime() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, LIST, null, null, null);
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(TIMESERIES));
        DateRange extent = TimeExtentCalculator.getTimeExtent(ci);
        assertEquals(Converters.convert("2014-01-01Z", Date.class), extent.getMinValue());
        assertEquals(Converters.convert("2019-01-01Z", Date.class), extent.getMaxValue());
    }
}
