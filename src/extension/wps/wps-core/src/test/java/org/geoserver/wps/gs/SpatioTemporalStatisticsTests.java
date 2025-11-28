/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

public class SpatioTemporalStatisticsTests extends WPSTestSupport {

    public static final double DELTA = 0.0001;
    private static QName ZONES = new QName(MockData.SF_URI, "zones", MockData.SF_PREFIX);
    private static QName ZONES2 = new QName(MockData.SF_URI, "zones2", MockData.SF_PREFIX);
    private static QName TEMPERATURES = new QName(MockData.SF_URI, "tempstat", MockData.SF_PREFIX);
    private static QName TEMPERATURES2 = new QName(MockData.SF_URI, "tempstatnan", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Map<SystemTestData.LayerProperty, Object> props = new HashMap<>();
        props.put(
                SystemTestData.LayerProperty.ENVELOPE,
                new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326", true)));
        testData.addVectorLayer(ZONES, props, "zones.properties", getClass(), getCatalog());
        // The sample temperature layers covers whole world with 40x20 pixels images
        // Each image is divided in 4 quadrants (matching the zones) where data has
        // been randomly pre-generated with a gradient of values within different range values.
        // Zone1: 0-30
        // Zone2: 10-20
        // Zone3: 0-20
        // Zone4: 10-40
        testData.addRasterLayer(TEMPERATURES, "temperatures.zip", null, null, getClass(), catalog);
        setupRasterDimension(TEMPERATURES, ResourceInfo.TIME, DimensionPresentation.LIST, null, "ISO8601", null);

        testData.addVectorLayer(ZONES2, props, "zones2.properties", getClass(), getCatalog());
        testData.addRasterLayer(TEMPERATURES2, "temperatures2.zip", null, null, getClass(), catalog);
        setupRasterDimension(TEMPERATURES2, ResourceInfo.TIME, DimensionPresentation.LIST, null, "ISO8601", null);
    }

    @Test
    public void testSingleStatistic() throws IOException {
        try (SimpleFeatureIterator features = executeProcess(
                "2025-04-16T00:00:00Z,2025-04-18T00:00:00Z,2025-04-20T00:00:00Z", "min", "sf:tempstat", "zones")) {

            SimpleFeature sf = features.next();
            assertNotNull(sf.getAttribute("min"));
            assertNull(sf.getAttribute("max"));
            assertNull(sf.getAttribute("sum"));
            assertNull(sf.getAttribute("median"));
        }
    }

    @Test
    public void testTimeRangeStatistics() throws IOException {
        try (SimpleFeatureIterator features = executeProcess(
                "2025-04-16T00:00:00Z/2025-04-20T00:00:00Z", "min,max,mean,median,sum", "sf:tempstat", "zones")) {
            assertZoneFeature(
                    features.next(),
                    1L,
                    "0-30",
                    1000,
                    0.0,
                    30.0,
                    14985.889781348407,
                    14.985889781348407,
                    14.955186462402343);

            assertZoneFeature(
                    features.next(),
                    2L,
                    "10-20",
                    1000,
                    10.0,
                    20.0,
                    14985.921039581299,
                    14.985921039581298,
                    14.988693809509277);

            assertZoneFeature(
                    features.next(),
                    3L,
                    "0-20",
                    1000,
                    0.0,
                    20.0,
                    9986.017562545836,
                    9.986017562545838,
                    9.957139682769775);

            assertZoneFeature(
                    features.next(),
                    4L,
                    "10-40",
                    1000,
                    10.0,
                    40.0,
                    24985.693196296692,
                    24.98569319629669,
                    25.014085006713866);
        }
    }

    @Test
    public void testTimeRangeStatisticsWithNaN() throws IOException {
        // Data for time 2025-10-17 and 2025-10-18 only contains NoData within Zone1
        // and a mix of NoData and valid pixels for Zone2
        // Data for time 2025-10-19 has valid pixels for both zones

        try (SimpleFeatureIterator features = executeProcess(
                "2025-10-17T00:00:00Z/2025-10-20T00:00:00Z", "min,max,mean,median,sum", "sf:tempstatnan", "zones2")) {
            assertZoneFeature(features.next(), 1L, "5-255", 21, 5.0, 213, 2167.0, 103.19047619047618, 102.0);

            assertZoneFeature(
                    features.next(), 2L, "5-255", 50, 47.0, 172, 5292.0, 106.15555555555555, 102.33333333333333);
        }

        // Requesting stats on a time with all NoData in Zone1 will return NaN stats
        try (SimpleFeatureIterator features =
                executeProcess("2025-10-18T00:00:00Z", "min,max,mean,median,sum", "sf:tempstatnan", "zones2")) {
            assertZoneFeature(
                    features.next(), 1L, "5-255", 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        // Requesting stats on a single time that doesn't contain any NoData will return same stats
        // as the time range including the NoData pixels (being the NoData values ignored)
        try (SimpleFeatureIterator features =
                executeProcess("2025-10-19T00:00:00Z", "min,max,mean,median,sum", "sf:tempstatnan", "zones2")) {
            assertZoneFeature(features.next(), 1L, "5-255", 21, 5.0, 213, 2167.0, 103.19047619047618, 102.0);
        }
    }

    @Test
    public void testTimeListStatistics() throws IOException {
        try (SimpleFeatureIterator features = executeProcess(
                "2025-04-16T00:00:00Z,2025-04-18T00:00:00Z,2025-04-20T00:00:00Z",
                "min,max,mean,median,sum",
                "sf:tempstat",
                "zones")) {

            assertZoneFeature(
                    features.next(),
                    1L,
                    "0-30",
                    600,
                    0.0,
                    30.0,
                    8993.916089296341,
                    14.989860148827232,
                    14.95425542195638);

            assertZoneFeature(
                    features.next(),
                    2L,
                    "10-20",
                    600,
                    10.0,
                    20.0,
                    8994.913537979126,
                    14.991522563298542,
                    15.062634627024332);

            assertZoneFeature(
                    features.next(),
                    3L,
                    "0-20",
                    600,
                    0.0,
                    20.0,
                    5994.0438846200705,
                    9.990073141033454,
                    9.9647749265035);

            assertZoneFeature(
                    features.next(),
                    4L,
                    "10-40",
                    600,
                    10.0,
                    40.0,
                    14994.685691833496,
                    24.991142819722494,
                    25.15604305267334);
        }
    }

    @Test
    public void testTimeStatisticsOnMissingTimes() throws IOException {
        SimpleFeature sf;
        try (SimpleFeatureIterator allTimesFeatures = executeProcess(
                "2025-04-16T00:00:00Z,2025-04-17T00:00:00Z,2025-04-18T00:00:00Z,2025-04-19T00:00:00Z,2025-04-20T00:00:00Z",
                "min",
                "sf:tempstat",
                "zones")) {
            sf = allTimesFeatures.next();
            // Each zone covers 20x10 valid pixels so with 5 times, we get an aggregated count of 1000 (200x5)
            assertEquals("Feature count", 1000, ((Number) sf.getAttribute("count")).intValue());
        }
        try (SimpleFeatureIterator missingTimesFeatures = executeProcess(
                "2025-04-16T00:00:00Z,2025-04-24T00:00:00Z,2025-04-26T00:00:00Z,2025-04-27T00:00:00Z,2025-04-29T00:00:00Z",
                "min",
                "sf:tempstat",
                "zones")) {
            sf = missingTimesFeatures.next();
            // Although we have specified 5 times, we only get a count of 200 due to the only matching time (2025-04-16)
            assertEquals("Feature count", 200, ((Number) sf.getAttribute("count")).intValue());
        }
    }

    private SimpleFeatureIterator executeProcess(String timeRange, String stats, String layerName, String zoneName)
            throws IOException {
        SpatioTemporalZonalStatistics process = applicationContext.getBean(SpatioTemporalZonalStatistics.class);
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName("sf", zoneName);
        SimpleFeatureSource featureSource = (SimpleFeatureSource) featureType.getFeatureSource(null, null);
        SimpleFeatureCollection zones = featureSource.getFeatures();
        SimpleFeatureCollection collection = process.execute(layerName, timeRange, zones, stats);
        return collection.features();
    }

    private void assertZoneFeature(
            SimpleFeature feature,
            long expectedZoneId,
            String expectedRange,
            int expectedCount,
            double expectedMin,
            double expectedMax,
            double expectedSum,
            double expectedMean,
            double expectedMedian) {
        assertNotNull("Feature should not be null", feature);
        Geometry geom = (Geometry) feature.getAttribute("z_the_geom");
        assertNotNull("Geometry should be present", geom);
        assertEquals("Expected geometry type", "Polygon", geom.getGeometryType());
        assertEquals("Zone ID", expectedZoneId, ((Number) feature.getAttribute("z_zone")).longValue());
        assertEquals("Range label", expectedRange, feature.getAttribute("z_range"));
        assertEquals("Feature count", expectedCount, ((Number) feature.getAttribute("count")).intValue());
        assertEquals("Min value", expectedMin, ((Number) feature.getAttribute("min")).doubleValue(), DELTA);
        assertEquals("Max value", expectedMax, ((Number) feature.getAttribute("max")).doubleValue(), DELTA);
        assertEquals("Sum", expectedSum, ((Number) feature.getAttribute("sum")).doubleValue(), DELTA);
        assertEquals("Mean", expectedMean, ((Number) feature.getAttribute("mean")).doubleValue(), DELTA);
        assertEquals("Median", expectedMedian, ((Number) feature.getAttribute("median")).doubleValue(), DELTA);
    }
}
