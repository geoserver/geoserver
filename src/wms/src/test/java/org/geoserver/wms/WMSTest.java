/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.renderer.style.ImageGraphicFactory;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class WMSTest extends WMSTestSupport {

    static final QName TIME_WITH_START_END =
            new QName(MockData.SF_URI, "TimeWithStartEnd", MockData.SF_PREFIX);
    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addVectorLayer(
                TIME_WITH_START_END,
                Collections.emptyMap(),
                "TimeElevationWithStartEnd.properties",
                getClass(),
                getCatalog());
    }

    protected void setupStartEndTimeDimension(
            String featureTypeName, String dimension, String start, String end) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(featureTypeName);
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(start);
        di.setEndAttribute(end);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(dimension, di);
        getCatalog().save(info);
    }

    @Before
    public void setWMS() throws Exception {
        wms = new WMS(getGeoServer());
    }

    @Test
    public void testGetTimeElevationToFilterStartEndDate() throws Exception {

        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "time", "startTime", "endTime");
        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "elevation", "startElevation", "endElevation");

        /* Reference for test assertions
        TimeElevationWithStartEnd.0=0|2012-02-11Z|2012-02-12Z|1.0|2.0|POLYGON((-180 90, 0 90, 0 0, -180 0, -180 90))
        TimeElevationWithStartEnd.1=1|2012-02-12Z|2012-02-13Z|2.0|3.0|POLYGON((0 90, 180 90, 180 0, 0 0, 0 90))
        TimeElevationWithStartEnd.2=2|2012-02-11Z|2012-02-14Z|1.0|3.0|POLYGON((-180 -90, 0 -90, 0 0, -180 0, -180 -90))
         */
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        doTimeElevationFilter(format.parse("2012-02-10"), null);
        doTimeElevationFilter(format.parse("2012-02-11"), null, 0, 2);
        doTimeElevationFilter(format.parse("2012-02-12"), null, 0, 1, 2);
        doTimeElevationFilter(format.parse("2012-02-13"), null, 1, 2);
        doTimeElevationFilter(format.parse("2012-02-15"), null);

        // Test start and end before all ranges.
        doTimeElevationFilter(
                new DateRange(format.parse("2012-02-09"), format.parse("2012-02-10")), null);
        // Test start before and end during a range.
        doTimeElevationFilter(
                new DateRange(format.parse("2012-02-09"), format.parse("2012-02-11")), null, 0, 2);
        // Test start on and end after or during a range.
        doTimeElevationFilter(
                new DateRange(format.parse("2012-02-11"), format.parse("2012-02-13")),
                null,
                0,
                1,
                2);
        // Test start before and end after all ranges.
        doTimeElevationFilter(
                new DateRange(format.parse("2012-02-09"), format.parse("2012-02-14")),
                null,
                0,
                1,
                2);
        // Test start during and end after a range.
        doTimeElevationFilter(
                new DateRange(format.parse("2012-02-13"), format.parse("2012-02-14")), null, 1, 2);
        // Test start during and end during a range.
        doTimeElevationFilter(
                new DateRange(format.parse("2012-02-12"), format.parse("2012-02-13")),
                null,
                0,
                1,
                2);
        // Test start and end after all ranges.
        doTimeElevationFilter(
                new DateRange(format.parse("2012-02-15"), format.parse("2012-02-16")), null);

        doTimeElevationFilter(null, 0);
        doTimeElevationFilter(null, 1, 0, 2);
        doTimeElevationFilter(null, 2, 0, 1, 2);
        doTimeElevationFilter(null, 3, 1, 2);
        doTimeElevationFilter(null, 4);

        doTimeElevationFilter(null, new NumberRange<>(Integer.class, -1, 0));
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, -1, 1), 0, 2);
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, 1, 3), 0, 1, 2);
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, -1, 4), 0, 1, 2);
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, 3, 4), 1, 2);
        doTimeElevationFilter(null, new NumberRange<>(Integer.class, 4, 5));

        // combined date/elevation - this should be an 'and' filter
        doTimeElevationFilter(format.parse("2012-02-12"), 2, 0, 1, 2);
        // disjunct verification
        doTimeElevationFilter(format.parse("2012-02-11"), 3, 2);
    }

    public void doTimeElevationFilter(Object time, Object elevation, Integer... expectedIds)
            throws Exception {

        FeatureTypeInfo timeWithStartEnd =
                getCatalog().getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        FeatureSource fs = timeWithStartEnd.getFeatureSource(null, null);

        List<Object> times = time == null ? null : Arrays.asList(time);
        List<Object> elevations = elevation == null ? null : Arrays.asList(elevation);

        Filter filter = wms.getTimeElevationToFilter(times, elevations, timeWithStartEnd);
        FeatureCollection features = fs.getFeatures(filter);

        Set<Integer> results = new HashSet<>();
        try (FeatureIterator it = features.features()) {
            while (it.hasNext()) {
                results.add((Integer) it.next().getProperty("id").getValue());
            }
        }
        assertTrue(
                "expected " + Arrays.toString(expectedIds) + " but got " + results,
                results.containsAll(Arrays.asList(expectedIds)));
        assertTrue(
                "expected " + Arrays.toString(expectedIds) + " but got " + results,
                Arrays.asList(expectedIds).containsAll(results));
    }

    @Test
    public void testWMSLifecycleHandlerGraphicCacheReset() throws Exception {

        Iterator<ExternalGraphicFactory> it =
                DynamicSymbolFactoryFinder.getExternalGraphicFactories();
        Map<URL, BufferedImage> imageCache = null;
        while (it.hasNext()) {
            ExternalGraphicFactory egf = it.next();
            if (egf instanceof ImageGraphicFactory) {
                Field cache = egf.getClass().getDeclaredField("imageCache");
                cache.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<URL, BufferedImage> cast = (Map) cache.get(egf);
                imageCache = cast;
                URL u = new URL("http://boundless.org");
                BufferedImage b = new BufferedImage(6, 6, 8);
                imageCache.put(u, b);
            }
        }
        assertNotEquals(0, imageCache.size());
        getGeoServer().reload();
        assertEquals(0, imageCache.size());
    }

    @Test
    public void testCacheConfiguration() {
        assertFalse(wms.isRemoteStylesCacheEnabled());

        WMSInfo info = wms.getServiceInfo();
        info.setCacheConfiguration(new CacheConfiguration(true));
        getGeoServer().save(info);
        assertTrue(wms.isRemoteStylesCacheEnabled());
    }

    @Test(expected = ServiceException.class)
    public void testCheckMaxDimensionsTime() throws Exception {
        WMSInfo info = wms.getServiceInfo();
        info.setMaxRequestedDimensionValues(1);
        getGeoServer().save(info);
        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "time", "startTime", "endTime");
        String name = TIME_WITH_START_END.getLocalPart();
        MapLayerInfo mapLayerInfo = new MapLayerInfo(getCatalog().getLayerByName(name));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        DateRange dateRange =
                new DateRange(formatter.parse("2012-02-09"), formatter.parse("2012-02-20"));
        List<Object> times = Arrays.asList(dateRange);
        wms.checkMaxDimensions(mapLayerInfo, times, null, false);
    }

    @Test(expected = ServiceException.class)
    public void testCheckMaxDimensionsElevation() throws Exception {
        WMSInfo info = wms.getServiceInfo();
        info.setMaxRequestedDimensionValues(1);
        getGeoServer().save(info);
        setupStartEndTimeDimension(
                TIME_WITH_START_END.getLocalPart(), "elevation", "startElevation", "endElevation");
        String name = TIME_WITH_START_END.getLocalPart();
        MapLayerInfo mapLayerInfo = new MapLayerInfo(getCatalog().getLayerByName(name));
        NumberRange elevationRange = NumberRange.create(0, 99);
        List<Object> elevations = Arrays.asList(elevationRange);
        wms.checkMaxDimensions(mapLayerInfo, null, elevations, false);
    }

    @Test
    public void testProjectionDensification() {
        assertFalse(wms.isAdvancedProjectionDensificationEnabled());

        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.ADVANCED_PROJECTION_DENSIFICATION_KEY, true);
        getGeoServer().save(info);
        assertTrue(wms.isAdvancedProjectionDensificationEnabled());
    }

    @Test
    public void testWrappingHeuristic() {
        assertFalse(wms.isDateLineWrappingHeuristicDisabled());

        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.DATELINE_WRAPPING_HEURISTIC_KEY, true);
        getGeoServer().save(info);
        assertTrue(wms.isDateLineWrappingHeuristicDisabled());
    }

    @Test
    public void testRootLayerInCapabilitiesEanbled() {
        assertTrue(wms.isRootLayerInCapabilitesEnabled());

        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY, false);
        getGeoServer().save(info);
        assertFalse(wms.isRootLayerInCapabilitesEnabled());
    }
}
