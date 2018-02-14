/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.layer.ExpirationRule;

import com.google.common.collect.ImmutableSet;

public interface GeoServerTileLayerInfo extends Serializable, Cloneable {

    String getId();

    void setId(String id);

    String getName();

    void setName(String name);

    /**
     * @return The {@link BlobStoreInfo#getId() blob store id} for this layer's tiles, or
     *         {@code null} if whatever the default blob store is shall be used
     */
    @Nullable
    String getBlobStoreId();

    /**
     * @param blobStoreId the {@link BlobStoreInfo#getId() blob store id} for this layer's tiles,
     *        or {@code null} if whatever the default blob store is shall be used
     */
    void setBlobStoreId(@Nullable String blobStoreId);

    int getMetaTilingX();

    int getMetaTilingY();

    void setMetaTilingY(int metaTilingY);

    void setMetaTilingX(int metaTilingX);
    
    int getExpireCache();
    
    void setExpireCache(int expireCache);
    
    List<ExpirationRule> getExpireCacheList();
    
    void setExpireCacheList(List<ExpirationRule> expireCacheList);
    
    int getExpireClients();
    
    void setExpireClients(int seconds);

    /**
     * Derived property from {@link #getParameterFilters()}, returns the configured allowable values
     * for a parameter filter over the {@code STYLE} key, if exists, or the empty set.
     * <p>
     * The returned set is immutable and dettached from this object's internal state
     * </p>
     * <p>
     * The returned set shall not return the default style for the layer
     * </p>
     */
    ImmutableSet<String> cachedStyles();

    Set<String> getMimeFormats();

    Set<XMLGridSubset> getGridSubsets();

    void setGridSubsets(Set<XMLGridSubset> gridSubsets);

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void setGutter(int gutter);

    int getGutter();

    boolean isAutoCacheStyles();

    void setAutoCacheStyles(boolean autoCacheStyles);

    /**
     * @return the parameterFilters
     */
    Set<ParameterFilter> getParameterFilters();

    /**
     * Replace the set of parameter filters
     * @param parameterFilters
     */
    void setParameterFilters(Set<ParameterFilter> parameterFilters);
    
    /**
     * Add a parameter filter, replacing any existing filter with the same key.
     * @param parameterFilter
     * @return true if an existing filter was replaced, false otherwise.
     */
    boolean addParameterFilter(ParameterFilter parameterFilter);
    
    /**
     * Remove the filter with the specified key
     * @param key
     * @return true if the filter existed, false otherwise
     */
    boolean removeParameterFilter(String key);

    GeoServerTileLayerInfo clone();

    /**
     * Get the ParameterFilter with the specified key
     * @param key
     *
     */
    ParameterFilter getParameterFilter(String key);

    boolean isInMemoryCached();
    
    void setInMemoryCached(boolean inMemoryCached);

}
