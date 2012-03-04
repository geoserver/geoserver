/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.filter.request.RequestFilter;
import org.geowebcache.layer.ExpirationRule;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.updatesource.UpdateSourceDefinition;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.util.GWCVars;

import com.google.common.collect.ImmutableSet;

/**
 * @author groldan
 * 
 */
public class GeoServerTileLayerInfoImpl implements Serializable, GeoServerTileLayerInfo {

    public static final long serialVersionUID = -3664183627578933094L;

    private String id;

    // // AbstractTileLayer mirror properties ////

    private boolean enabled;

    private String name;

    @SuppressWarnings("unused")
    transient private LayerMetaInformation metaInformation;

    private Set<String> mimeFormats;

    @SuppressWarnings("unused")
    private List<FormatModifier> formatModifiers;

    private Set<XMLGridSubset> gridSubsets;

    @SuppressWarnings("unused")
    transient private List<? extends UpdateSourceDefinition> updateSources;

    @SuppressWarnings("unused")
    transient private List<? extends RequestFilter> requestFilters;

    @SuppressWarnings("unused")
    transient private boolean useETags;

    private int[] metaWidthHeight;

    /**
     * @see GWCVars#CACHE_DISABLE_CACHE
     * @see GWCVars#CACHE_NEVER_EXPIRE
     * @see GWCVars#CACHE_USE_WMS_BACKEND_VALUE
     * @see GWCVars#CACHE_VALUE_UNSET
     */
    @SuppressWarnings("unused")
    transient private int expireCache;

    @SuppressWarnings("unused")
    transient private List<ExpirationRule> expireCacheList;

    @SuppressWarnings("unused")
    transient private int expireClients;

    @SuppressWarnings("unused")
    transient private List<ExpirationRule> expireClientsList;

    @SuppressWarnings("unused")
    transient private Integer backendTimeout;

    @SuppressWarnings("unused")
    transient private Boolean cacheBypassAllowed;

    @SuppressWarnings("unused")
    transient private Boolean queryable;

    private Set<ParameterFilter> parameterFilters;

    // //// GeoServerTileLayer specific properties //////
    private int gutter;

    private boolean autoCacheStyles;

    public GeoServerTileLayerInfoImpl() {
        readResolve();
    }

    /**
     * XStream initialization of unset fields
     * 
     * @return {@code this}
     */
    private final GeoServerTileLayerInfo readResolve() {
        if (null == metaWidthHeight) {
            metaWidthHeight = new int[2];
        }
        gridSubsets = nonNull(gridSubsets);
        mimeFormats = nonNull(mimeFormats);
        parameterFilters = nonNull(parameterFilters);
        return this;
    }

    /**
     * @see java.lang.Object#clone()
     */
    @Override
    public GeoServerTileLayerInfoImpl clone() {
        GeoServerTileLayerInfoImpl clone;
        try {
            clone = (GeoServerTileLayerInfoImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw propagate(e);
        }
        clone.metaWidthHeight = metaWidthHeight.clone();
        clone.gridSubsets = nonNull(null);
        for (XMLGridSubset gs : gridSubsets) {
            clone.gridSubsets.add(gs.clone());
        }
        clone.mimeFormats = nonNull(null);
        clone.mimeFormats.addAll(mimeFormats);
        clone.parameterFilters = nonNull(null);
        for (ParameterFilter pf : parameterFilters) {
            clone.parameterFilters.add(pf.clone());
        }
        return clone;
    }

    private <T> Set<T> nonNull(Set<T> set) {
        return set == null ? new HashSet<T>() : set;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setId(java.lang.String)
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getMetaTilingX()
     */
    @Override
    public int getMetaTilingX() {
        return metaWidthHeight[0];
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getMetaTilingY()
     */
    @Override
    public int getMetaTilingY() {
        return metaWidthHeight[1];
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setMetaTilingY(int)
     */
    @Override
    public void setMetaTilingY(int metaTilingY) {
        checkArgument(metaTilingY > 0);
        metaWidthHeight[1] = metaTilingY;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setMetaTilingX(int)
     */
    @Override
    public void setMetaTilingX(int metaTilingX) {
        checkArgument(metaTilingX > 0);
        metaWidthHeight[0] = metaTilingX;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#cachedStyles()
     */
    @Override
    public ImmutableSet<String> cachedStyles() {

        ParameterFilter styleQualifier = TileLayerInfoUtil.findParameterFilter("STYLES",
                getParameterFilters());

        if (styleQualifier != null) {
            if (styleQualifier instanceof StringParameterFilter) {
                StringParameterFilter sp = (StringParameterFilter) styleQualifier;
                return ImmutableSet.copyOf(sp.getLegalValues());
            }
        }
        return ImmutableSet.of();
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getMimeFormats()
     */
    @Override
    public Set<String> getMimeFormats() {
        return mimeFormats;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getGridSubsets()
     */
    @Override
    public Set<XMLGridSubset> getGridSubsets() {
        return gridSubsets;
    }

    @Override
    public void setGridSubsets(Set<XMLGridSubset> gridSubsets) {
        this.gridSubsets = nonNull(gridSubsets);
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setGutter(int)
     */
    @Override
    public void setGutter(int gutter) {
        this.gutter = gutter;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getGutter()
     */
    @Override
    public int getGutter() {
        return gutter;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#isAutoCacheStyles()
     */
    @Override
    public boolean isAutoCacheStyles() {
        return autoCacheStyles;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#setAutoCacheStyles(boolean)
     */
    @Override
    public void setAutoCacheStyles(boolean autoCacheStyles) {
        this.autoCacheStyles = autoCacheStyles;
    }

    /**
     * @see org.geoserver.gwc.layer.GeoServerTileLayerInfo#getParameterFilters()
     */
    @Override
    public Set<ParameterFilter> getParameterFilters() {
        return parameterFilters;
    }

    @Override
    public void setParameterFilters(Set<ParameterFilter> parameterFilters) {
        this.parameterFilters = nonNull(parameterFilters);
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

}
