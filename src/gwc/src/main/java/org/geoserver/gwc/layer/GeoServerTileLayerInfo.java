/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.geowebcache.config.BlobStoreConfig;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.layer.ExpirationRule;

public interface GeoServerTileLayerInfo extends Serializable, Cloneable {

    public abstract String getId();

    public abstract void setId(String id);

    public abstract String getName();

    public abstract void setName(String name);

    /**
     * @return The {@link BlobStoreConfig#getId() blob store id} for this layer's tiles, or {@code
     *     null} if whatever the default blob store is shall be used
     */
    @Nullable
    public abstract String getBlobStoreId();

    /**
     * @param blobStoreId the {@link BlobStoreConfig#getId() blob store id} for this layer's tiles,
     *     or {@code null} if whatever the default blob store is shall be used
     */
    public abstract void setBlobStoreId(@Nullable String blobStoreId);

    public abstract int getMetaTilingX();

    public abstract int getMetaTilingY();

    public abstract void setMetaTilingY(int metaTilingY);

    public abstract void setMetaTilingX(int metaTilingX);

    public abstract int getExpireCache();

    public abstract void setExpireCache(int expireCache);

    public abstract List<ExpirationRule> getExpireCacheList();

    public abstract void setExpireCacheList(List<ExpirationRule> expireCacheList);

    public abstract int getExpireClients();

    public abstract void setExpireClients(int seconds);

    /**
     * Derived property from {@link #getParameterFilters()}, returns the configured allowable values
     * for a parameter filter over the {@code STYLE} key, if exists, or the empty set.
     *
     * <p>The returned set is immutable and dettached from this object's internal state
     *
     * <p>The returned set shall not return the default style for the layer
     */
    public abstract ImmutableSet<String> cachedStyles();

    public abstract Set<String> getMimeFormats();

    public abstract Set<XMLGridSubset> getGridSubsets();

    public abstract void setGridSubsets(Set<XMLGridSubset> gridSubsets);

    public abstract void setEnabled(boolean enabled);

    public abstract boolean isEnabled();

    public abstract void setGutter(int gutter);

    public abstract int getGutter();

    public abstract boolean isAutoCacheStyles();

    public abstract void setAutoCacheStyles(boolean autoCacheStyles);

    /** @return the parameterFilters */
    public abstract Set<ParameterFilter> getParameterFilters();

    /**
     * Replace the set of parameter filters
     *
     * @param parameterFilters
     */
    public abstract void setParameterFilters(Set<ParameterFilter> parameterFilters);

    /**
     * Add a parameter filter, replacing any existing filter with the same key.
     *
     * @param parameterFilter
     * @return true if an existing filter was replaced, false otherwise.
     */
    public abstract boolean addParameterFilter(ParameterFilter parameterFilter);

    /**
     * Remove the filter with the specified key
     *
     * @param key
     * @return true if the filter existed, false otherwise
     */
    public abstract boolean removeParameterFilter(String key);

    public abstract GeoServerTileLayerInfo clone();

    /**
     * Get the ParameterFilter with the specified key
     *
     * @param key
     */
    public abstract ParameterFilter getParameterFilter(String key);

    public abstract boolean isInMemoryCached();

    public abstract void setInMemoryCached(boolean inMemoryCached);
}
