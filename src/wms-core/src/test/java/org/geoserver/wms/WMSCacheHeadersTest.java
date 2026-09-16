/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.junit.Test;

/** Unit test suite for {@link WMS#cacheMaxAge} and {@link WMS#cacheControlHeaders}. */
public class WMSCacheHeadersTest {

    private static int nextName = 0;

    /** A {@code MapLayerInfo} backed by real catalog objects, since {@code MapLayerInfo} is {@code final}. */
    private MapLayerInfo layer(boolean cachingEnabled, int maxAge) {
        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setPrefix("test");
        ns.setURI("http://example.com");

        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl((org.geoserver.catalog.Catalog) null);
        resource.setName("layer" + nextName++);
        resource.setNamespace(ns);
        resource.getMetadata().put(ResourceInfo.CACHING_ENABLED, cachingEnabled);
        resource.getMetadata().put(ResourceInfo.CACHE_AGE_MAX, maxAge);

        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setResource(resource);
        layer.setType(PublishedType.VECTOR);
        return new MapLayerInfo(layer);
    }

    @Test
    public void testCacheMaxAgeAbsentOnPost() {
        OptionalInt maxAge = WMS.cacheMaxAge(false, List.of(layer(true, 60)));
        assertFalse(maxAge.isPresent());
    }

    @Test
    public void testCacheMaxAgeAbsentWhenAnyLayerDisablesCaching() {
        OptionalInt maxAge = WMS.cacheMaxAge(true, List.of(layer(true, 60), layer(false, 120)));
        assertFalse(maxAge.isPresent());
    }

    @Test
    public void testCacheMaxAgeAbsentWhenNoLayers() {
        OptionalInt maxAge = WMS.cacheMaxAge(true, List.of());
        assertFalse(maxAge.isPresent());
    }

    @Test
    public void testCacheMaxAgeIsSmallestAmongLayers() {
        OptionalInt maxAge = WMS.cacheMaxAge(true, List.of(layer(true, 120), layer(true, 60), layer(true, 300)));
        assertTrue(maxAge.isPresent());
        assertEquals(60, maxAge.getAsInt());
    }

    @Test
    public void testCacheControlHeadersFormat() {
        Map<String, String> headers = WMS.cacheControlHeaders(60);
        assertEquals("max-age=60, must-revalidate", headers.get("Cache-Control"));
        assertTrue(headers.containsKey("Expires"));
    }
}
