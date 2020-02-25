/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.request.RequestFilter;
import org.geowebcache.layer.ExpirationRule;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.updatesource.UpdateSourceDefinition;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.util.GWCVars;

/**
 * {@link GeoServerTileLayerInfo} implementation.
 *
 * @author groldan
 */
public class GeoServerTileLayerInfoImpl implements Serializable, GeoServerTileLayerInfo {

    /** serialVersionUID */
    private static final long serialVersionUID = 8277055420849712230L;

    private static final Logger LOGGER = Logging.getLogger(GeoServerTileLayerInfoImpl.class);

    private String id;

    // // AbstractTileLayer mirror properties ////

    private boolean enabled;

    private Boolean inMemoryCached;

    private String name;

    private String blobStoreId;

    @SuppressWarnings("unused")
    private transient LayerMetaInformation metaInformation;

    private Set<String> mimeFormats;

    @SuppressWarnings("unused")
    @SuppressFBWarnings("SE_BAD_FIELD")
    private List<FormatModifier> formatModifiers;

    @SuppressFBWarnings("SE_BAD_FIELD")
    private Set<XMLGridSubset> gridSubsets;

    @SuppressWarnings("unused")
    private transient List<? extends UpdateSourceDefinition> updateSources;

    @SuppressWarnings("unused")
    private transient List<? extends RequestFilter> requestFilters;

    @SuppressWarnings("unused")
    private transient boolean useETags;

    private int[] metaWidthHeight;

    /**
     * @see GWCVars#CACHE_DISABLE_CACHE
     * @see GWCVars#CACHE_NEVER_EXPIRE
     * @see GWCVars#CACHE_USE_WMS_BACKEND_VALUE
     * @see GWCVars#CACHE_VALUE_UNSET
     */
    private int expireCache;

    @SuppressFBWarnings("SE_BAD_FIELD")
    private List<ExpirationRule> expireCacheList;

    private int expireClients;

    @SuppressWarnings("unused")
    private transient List<ExpirationRule> expireClientsList;

    @SuppressWarnings("unused")
    private transient Integer backendTimeout;

    @SuppressWarnings("unused")
    private transient Boolean cacheBypassAllowed;

    @SuppressWarnings("unused")
    private transient Boolean queryable;

    // The actual storage
    private transient Map<String, ParameterFilter> parameterFiltersMap;

    // Just used for serialize/deserialize to make xstream keep the same format it used to.
    private Set<ParameterFilter> parameterFilters;

    // //// GeoServerTileLayer specific properties //////
    private int gutter;

    // For backward compatibility with 2.2 and 2.3
    // FIXME  need to hide this when serializing back out
    private Boolean autoCacheStyles;

    public GeoServerTileLayerInfoImpl() {
        readResolve();
    }

    /**
     * XStream initialization of unset fields
     *
     * @return {@code this}
     */
    private final Object readResolve() {
        if (null == metaWidthHeight) {
            metaWidthHeight = new int[2];
        }
        gridSubsets = nonNull(gridSubsets);
        mimeFormats = nonNull(mimeFormats);

        // Convert the deserialized set into a map.
        parameterFilters = nonNull(parameterFilters);
        setParameterFilters(parameterFilters);

        // Apply the old autoCacheStyles flag if it was specified.
        if (autoCacheStyles != null) {
            if (autoCacheStyles) {
                if (!isAutoCacheStyles()) {
                    addParameterFilter(new StyleParameterFilter());
                }
            } else {
                if (isAutoCacheStyles()) {
                    this.removeParameterFilter("STYLES");
                }
            }
            autoCacheStyles = null;
        }
        return this;
    }

    private final Object writeReplace() {
        parameterFilters = getParameterFilters();
        return this;
    }

    /** @see java.lang.Object#clone() */
    @Override
    public GeoServerTileLayerInfoImpl clone() {
        GeoServerTileLayerInfoImpl clone;
        try {
            clone = (GeoServerTileLayerInfoImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.metaWidthHeight = metaWidthHeight.clone();
        clone.gridSubsets = nonNull((Set<XMLGridSubset>) null);
        for (XMLGridSubset gs : gridSubsets) {
            clone.gridSubsets.add(gs.clone());
        }
        clone.mimeFormats = nonNull((Set<String>) null);
        clone.mimeFormats.addAll(mimeFormats);
        clone.parameterFiltersMap = nonNull((Map<String, ParameterFilter>) null);
        for (ParameterFilter pf : parameterFiltersMap.values()) {
            clone.addParameterFilter(pf.clone());
        }
        return clone;
    }

    private <T> Set<T> nonNull(Set<T> set) {
        return set == null ? new HashSet<T>() : set;
    }

    private <K, T> Map<K, T> nonNull(Map<K, T> set) {
        return set == null ? new HashMap<K, T>() : set;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getId() */
    @Override
    public String getId() {
        return id;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setId(java.lang.String) */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getName() */
    @Override
    public String getName() {
        return name;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setName(java.lang.String) */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Nullable
    public String getBlobStoreId() {
        return blobStoreId;
    }

    @Override
    public void setBlobStoreId(@Nullable String blobStoreId) {
        this.blobStoreId = blobStoreId;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getMetaTilingX() */
    @Override
    public int getMetaTilingX() {
        return metaWidthHeight[0];
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getMetaTilingY() */
    @Override
    public int getMetaTilingY() {
        return metaWidthHeight[1];
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setMetaTilingY(int) */
    @Override
    public void setMetaTilingY(int metaTilingY) {
        checkArgument(metaTilingY > 0);
        metaWidthHeight[1] = metaTilingY;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setMetaTilingX(int) */
    @Override
    public void setMetaTilingX(int metaTilingX) {
        checkArgument(metaTilingX > 0);
        metaWidthHeight[0] = metaTilingX;
    }

    /** @see GeoServerTileLayerInfo#getExpireCache() */
    @Override
    public int getExpireCache() {
        return expireCache;
    }

    /** @see GeoServerTileLayerInfo#setExpireCache(int) */
    @Override
    public void setExpireCache(int expireCache) {
        this.expireCache = expireCache;
    }

    /** @see GeoServerTileLayerInfo#getExpireCacheList() */
    @Override
    public List<ExpirationRule> getExpireCacheList() {
        return expireCacheList;
    }

    /** @see GeoServerTileLayerInfo#setExpireCacheList(List) */
    @Override
    public void setExpireCacheList(List<ExpirationRule> expireCacheList) {
        this.expireCacheList = expireCacheList;
    }

    /** @see GeoServerTileLayerInfo#getExpireClients() */
    @Override
    public int getExpireClients() {
        return expireClients;
    }

    /** @see GeoServerTileLayerInfo#setExpireClients(int) */
    @Override
    public void setExpireClients(int seconds) {
        expireClients = seconds;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#cachedStyles() */
    @Override
    public ImmutableSet<String> cachedStyles() {
        ParameterFilter styleQualifier = getParameterFilter("STYLES");
        try {
            if (styleQualifier != null) {
                List<String> styles = styleQualifier.getLegalValues();
                if (styles != null) {
                    return ImmutableSet.copyOf(styles);
                }
            }
        } catch (IllegalStateException ex) {
            LOGGER.log(Level.WARNING, "StyleParameterFilter was not initialized properly", ex);
        }
        return ImmutableSet.of();
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getMimeFormats() */
    @Override
    public Set<String> getMimeFormats() {
        return mimeFormats;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getGridSubsets() */
    @Override
    public Set<XMLGridSubset> getGridSubsets() {
        return gridSubsets;
    }

    @Override
    public void setGridSubsets(Set<XMLGridSubset> gridSubsets) {
        this.gridSubsets = nonNull(gridSubsets);
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setEnabled(boolean) */
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#isEnabled() */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setGutter(int) */
    @Override
    public void setGutter(int gutter) {
        this.gutter = gutter;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getGutter() */
    @Override
    public int getGutter() {
        return gutter;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#isAutoCacheStyles() */
    @Override
    public boolean isAutoCacheStyles() {
        ParameterFilter filter = getParameterFilter("STYLES");
        return filter != null
                && filter instanceof StyleParameterFilter
                && ((StyleParameterFilter) filter).getStyles() == null;
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setAutoCacheStyles(boolean) */
    @Override
    public void setAutoCacheStyles(boolean autoCacheStyles) {
        if (autoCacheStyles) {
            // Add a default StyleParameterFilter.
            ParameterFilter newFilter = new StyleParameterFilter();
            addParameterFilter(newFilter);
        } else {
            ParameterFilter filter = getParameterFilter("STYLES");
            if (filter != null && filter instanceof StyleParameterFilter) {
                parameterFilters.remove(filter);
            }
        }
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getParameterFilters() */
    @Override
    public Set<ParameterFilter> getParameterFilters() {
        return new HashSet<ParameterFilter>(parameterFiltersMap.values());
    }

    /** @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setParameterFilters(Set) */
    @Override
    public void setParameterFilters(Set<ParameterFilter> parameterFilters) {
        parameterFiltersMap = new HashMap<String, ParameterFilter>();
        for (ParameterFilter pf : parameterFilters) {
            addParameterFilter(pf);
        }
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        // return GeoServerTileLayerInfoLoader.marshalJson(this);
    }

    @Override
    public boolean addParameterFilter(ParameterFilter parameterFilter) {
        return parameterFiltersMap.put(parameterFilter.getKey().toUpperCase(), parameterFilter)
                != null;
    }

    @Override
    public boolean removeParameterFilter(String key) {
        return parameterFiltersMap.remove(key.toUpperCase()) != null;
    }

    @Override
    public ParameterFilter getParameterFilter(String key) {
        return parameterFiltersMap.get(key.toUpperCase());
    }

    @Override
    public boolean isInMemoryCached() {
        return inMemoryCached != null ? inMemoryCached : true;
    }

    @Override
    public void setInMemoryCached(boolean inMemoryCached) {
        this.inMemoryCached = inMemoryCached;
    }
}
