/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.junit.Before;
import org.junit.Test;

public class MapLayerInfoTest {

    public static final int MAX_AGE_VALUE = 100;

    @Before
    public void setUp() throws Exception {}

    private MetadataMap getEnabledCacheMetadata(int maxAgeValue, Boolean cachingEnabled) {
        Map<String, Serializable> mapItems = new HashMap<>();
        mapItems.put(ResourceInfo.CACHING_ENABLED, cachingEnabled);
        mapItems.put(ResourceInfo.CACHE_AGE_MAX, maxAgeValue);
        return new MetadataMap(mapItems);
    }

    private LayerInfo getLayerInfo(PublishedType type) {
        LayerInfo layerInfo = new LayerInfoImpl();

        Catalog catalog = new CatalogImpl();
        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(catalog);
        resource.setName("name");
        NamespaceInfoImpl namespaceInfo = new NamespaceInfoImpl();
        namespaceInfo.setPrefix("prefix");
        resource.setNamespace(namespaceInfo);
        layerInfo.setResource(resource);
        layerInfo.setType(type);

        return layerInfo;
    }

    /** check is caching disabled remote layer, overridden caching */
    @Test
    public void isCachingDisabledRemoteWFSLayer() {
        MapLayerInfo info =
                new MapLayerInfo(
                        getLayerInfo(PublishedType.REMOTE),
                        getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));

        assertFalse(info.isCachingEnabled());
    }

    /** check if caching disabled remote layer, no overridden caching */
    @Test
    public void isCachingDisabledRemoteWFSLayerNoOverridden() {
        MapLayerInfo info = new MapLayerInfo(getLayerInfo(PublishedType.REMOTE));

        assertFalse(info.isCachingEnabled());
    }

    /** Check if caching disabled, no overridden caching, no layer caching */
    @Test
    public void isCachingDisabled() {
        MapLayerInfo info = new MapLayerInfo(getLayerInfo(PublishedType.VECTOR));

        assertFalse(info.isCachingEnabled());
    }

    /** Check if caching enabled, no overridden caching, layer caching */
    @Test
    public void isCachingLayerInfoEnabled() {
        LayerInfo layerInfo = getLayerInfo(PublishedType.VECTOR);
        ResourceInfoImpl resource = (ResourceInfoImpl) layerInfo.getResource();
        resource.setMetadata(getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));
        MapLayerInfo info = new MapLayerInfo(layerInfo);

        assertTrue(info.isCachingEnabled());
    }

    /** Check if caching enabled, overriden caching, no layer caching */
    @Test
    public void isCachingEnabledWithOverrideMetadata() {
        MapLayerInfo info =
                new MapLayerInfo(
                        getLayerInfo(PublishedType.VECTOR),
                        getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));

        assertTrue(info.isCachingEnabled());
    }

    /** Check if caching enabled, overridden caching, layer caching */
    @Test
    public void isCachingLayerInfoEnabledBothDefined() {
        LayerInfo layerInfo = getLayerInfo(PublishedType.VECTOR);
        ResourceInfoImpl resource = (ResourceInfoImpl) layerInfo.getResource();
        resource.setMetadata(getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));
        MapLayerInfo info =
                new MapLayerInfo(layerInfo, getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));

        assertTrue(info.isCachingEnabled());
    }

    /** Max age is 0, no overridden caching, no layer caching */
    @Test
    public void getCacheMaxAgeIfDisabled() {
        MapLayerInfo info = new MapLayerInfo(getLayerInfo(PublishedType.VECTOR));

        assertEquals(0, info.getCacheMaxAge());
    }

    /** Max age is taken from layer, no override caching, layer caching */
    @Test
    public void getCacheMaxAgeLayerInfo() {
        MapLayerInfo info = new MapLayerInfo(getLayerInfo(PublishedType.VECTOR));
        ResourceInfoImpl resource = (ResourceInfoImpl) info.getResource();
        resource.setMetadata(getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));

        assertEquals(MAX_AGE_VALUE, info.getCacheMaxAge());
    }

    /** Max age is taken from overridden, override caching, no layer caching. */
    @Test
    public void getCacheMaxAgeLayerInfoOverridden() {
        MapLayerInfo info =
                new MapLayerInfo(
                        getLayerInfo(PublishedType.VECTOR),
                        getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));

        assertEquals(MAX_AGE_VALUE, info.getCacheMaxAge());
    }

    /**
     * Max age is taken from layer, override caching, layer caching Overridden with higher value.
     */
    @Test
    public void getCacheMaxAgeLayerInfoBothDefined() {
        MapLayerInfo info =
                new MapLayerInfo(
                        getLayerInfo(PublishedType.VECTOR),
                        getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));
        ResourceInfoImpl resource = (ResourceInfoImpl) info.getResource();
        resource.setMetadata(getEnabledCacheMetadata(MAX_AGE_VALUE - 1, Boolean.TRUE));

        assertEquals(MAX_AGE_VALUE, info.getCacheMaxAge());
    }

    /** Max age is taken from layer, override caching, layer caching Overridden with lower value. */
    @Test
    public void getCacheMaxAgeLayerInfoBothDefinedOverridenLower() {
        MapLayerInfo info =
                new MapLayerInfo(
                        getLayerInfo(PublishedType.VECTOR),
                        getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.TRUE));
        ResourceInfoImpl resource = (ResourceInfoImpl) info.getResource();
        resource.setMetadata(getEnabledCacheMetadata(MAX_AGE_VALUE + 1, Boolean.TRUE));

        assertEquals(MAX_AGE_VALUE, info.getCacheMaxAge());
    }

    /**
     * Max age is defined in override metadata, but cache is not enabled in override metadata. e.g.
     * layer group has defined max age but is not enabled.
     */
    @Test
    public void getCacheMaxAgeGroupDisabledButDefined() {
        MapLayerInfo info =
                new MapLayerInfo(
                        getLayerInfo(PublishedType.VECTOR),
                        getEnabledCacheMetadata(MAX_AGE_VALUE, Boolean.FALSE));
        ResourceInfoImpl resource = (ResourceInfoImpl) info.getResource();
        resource.setMetadata(getEnabledCacheMetadata(MAX_AGE_VALUE + 1, Boolean.TRUE));

        assertEquals(MAX_AGE_VALUE + 1, info.getCacheMaxAge());
    }
}
