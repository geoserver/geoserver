/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.gwc.GWC;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.filter.Filter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

/**
 * Verifies that {@link org.geoserver.security.CachingResourceAccessManager} actually reduces inner-RAM invocations
 * within a single GWC tile request. The {@link CountingResourceAccessManager} sits beneath the caching wrapper and
 * counts real delegate calls; the caching wrapper bounds inner RAM calls to one per distinct method overload per
 * request scope, not one per catalog check.
 */
public class CachingRAMIntegrationTest extends WMSTestSupport {

    /**
     * Inner RAM that counts {@link #getAccessLimits} invocations. Registered as a Spring bean so
     * {@code lookupResourceAccessManager()} wraps it with {@code CachingResourceAccessManager}.
     */
    public static class CountingResourceAccessManager extends TestResourceAccessManager {

        /** Shared counter, reset by the test before each request under examination. */
        public static final AtomicInteger CALL_COUNT = new AtomicInteger();

        @Override
        public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
            CALL_COUNT.incrementAndGet();
            return super.getAccessLimits(user, layer);
        }
    }

    static final QName MOSAIC = new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX);

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/gwc/security/CachingRAMContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GWC.get().getConfig().setSecurityEnabled(true);

        testData.addStyle("raster", "raster.sld", SystemTestData.class, getCatalog());
        testData.addRasterLayer(
                MOSAIC,
                "raster-filter-test.zip",
                null,
                Collections.singletonMap(LayerProperty.STYLE, "raster"),
                SystemTestData.class,
                getCatalog());

        Catalog catalog = getCatalog();
        CountingResourceAccessManager ram =
                (CountingResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        LayerInfo layer = catalog.getLayerByName(getLayerId(MOSAIC));
        ram.putLimits("cite", layer, new DataAccessLimits(CatalogMode.HIDE, Filter.INCLUDE));
    }

    @Before
    public void setUp() {
        CountingResourceAccessManager.CALL_COUNT.set(0);
    }

    @Test
    public void testCachingReducesInnerRAMCallsOnTileMiss() throws Exception {
        login("cite", "cite", "ROLE_DUMMY");
        String path = "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png"
                + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326"
                + "&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";

        // tile-miss: WMS sub-dispatch triggers additional catalog access checks, but
        // the caching wrapper ensures each distinct (kind, user, target) is computed once
        CountingResourceAccessManager.CALL_COUNT.set(0);
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
        int missCalls = CountingResourceAccessManager.CALL_COUNT.get();

        // tile-hit: GWC serves from cache; only computeSecurityKey() needs the inner RAM
        CountingResourceAccessManager.CALL_COUNT.set(0);
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
        int hitCalls = CountingResourceAccessManager.CALL_COUNT.get();

        // Both requests produce 2 inner-RAM calls: one for computeSecurityKey() and one for the
        // catalog security check. In the tile-miss case the WMS sub-dispatch runs on the same thread
        // and inherits the same request scope, so the catalog check is already cached when the
        // sub-dispatch runs -- no extra inner call. Without caching every catalog access in both
        // the outer request and the sub-dispatch would invoke the inner RAM independently,
        // bringing the total to 5+.
        assertEquals("cached tile: inner RAM calls", 2, hitCalls);
        assertEquals("tile miss: inner RAM calls with caching", 2, missCalls);
    }
}
