/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Date;
import java.util.*;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
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
                Collections.EMPTY_MAP,
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
        TimeElevation.0=0|2012-02-11|2012-02-12|1|2
        TimeElevation.1=1|2012-02-12|2012-02-13|2|3
        TimeElevation.2=2|2012-02-11|2012-02-14|1|3
         */

        doTimeElevationFilter(Date.valueOf("2012-02-10"), null);
        doTimeElevationFilter(Date.valueOf("2012-02-11"), null, 0, 2);
        doTimeElevationFilter(Date.valueOf("2012-02-12"), null, 0, 1, 2);
        doTimeElevationFilter(Date.valueOf("2012-02-13"), null, 1, 2);
        doTimeElevationFilter(Date.valueOf("2012-02-15"), null);

        // Test start and end before all ranges.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-10")), null);
        // Test start before and end during a range.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-11")), null, 0, 2);
        // Test start on and end after or during a range.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-11"), Date.valueOf("2012-02-13")),
                null,
                0,
                1,
                2);
        // Test start before and end after all ranges.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-14")),
                null,
                0,
                1,
                2);
        // Test start during and end after a range.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-13"), Date.valueOf("2012-02-14")), null, 1, 2);
        // Test start during and end during a range.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-12"), Date.valueOf("2012-02-13")),
                null,
                0,
                1,
                2);
        // Test start and end after all ranges.
        doTimeElevationFilter(
                new DateRange(Date.valueOf("2012-02-15"), Date.valueOf("2012-02-16")), null);

        doTimeElevationFilter(null, 0);
        doTimeElevationFilter(null, 1, 0, 2);
        doTimeElevationFilter(null, 2, 0, 1, 2);
        doTimeElevationFilter(null, 3, 1, 2);
        doTimeElevationFilter(null, 4);

        doTimeElevationFilter(null, new NumberRange(Integer.class, -1, 0));
        doTimeElevationFilter(null, new NumberRange(Integer.class, -1, 1), 0, 2);
        doTimeElevationFilter(null, new NumberRange(Integer.class, 1, 3), 0, 1, 2);
        doTimeElevationFilter(null, new NumberRange(Integer.class, -1, 4), 0, 1, 2);
        doTimeElevationFilter(null, new NumberRange(Integer.class, 3, 4), 1, 2);
        doTimeElevationFilter(null, new NumberRange(Integer.class, 4, 5));

        // combined date/elevation - this should be an 'and' filter
        doTimeElevationFilter(Date.valueOf("2012-02-12"), 2, 0, 1, 2);
        // disjunct verification
        doTimeElevationFilter(Date.valueOf("2012-02-11"), 3, 2);
    }

    public void doTimeElevationFilter(Object time, Object elevation, Integer... expectedIds)
            throws Exception {

        FeatureTypeInfo timeWithStartEnd =
                getCatalog().getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        FeatureSource fs = timeWithStartEnd.getFeatureSource(null, null);

        List times = time == null ? null : Arrays.asList(time);
        List elevations = elevation == null ? null : Arrays.asList(elevation);

        Filter filter = wms.getTimeElevationToFilter(times, elevations, timeWithStartEnd);
        FeatureCollection features = fs.getFeatures(filter);

        Set<Integer> results = new HashSet<Integer>();
        FeatureIterator it = features.features();
        try {
            while (it.hasNext()) {
                results.add((Integer) it.next().getProperty("id").getValue());
            }
        } finally {
            it.close();
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
                imageCache = (Map) cache.get(egf);
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
