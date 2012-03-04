/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.io.Serializable;
import java.util.Set;

import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;

import com.google.common.collect.ImmutableSet;

public interface GeoServerTileLayerInfo extends Serializable, Cloneable {

    public abstract String getId();

    public abstract void setId(String id);

    public abstract String getName();

    public abstract void setName(String name);

    public abstract int getMetaTilingX();

    public abstract int getMetaTilingY();

    public abstract void setMetaTilingY(int metaTilingY);

    public abstract void setMetaTilingX(int metaTilingX);

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

    /**
     * @return the parameterFilters
     */
    public abstract Set<ParameterFilter> getParameterFilters();

    public abstract void setParameterFilters(Set<ParameterFilter> parameterFilters);

    public abstract GeoServerTileLayerInfo clone();

}