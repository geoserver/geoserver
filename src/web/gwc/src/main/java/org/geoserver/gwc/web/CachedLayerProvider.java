/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geowebcache.layer.TileLayer;

/**
 * Provides a filtered, sorted view over GWC {@link TileLayer}s for {@link CachedLayersPage} using
 * {@link CachedLayerInfo} as data view.
 * 
 * @author groldan
 */
@SuppressWarnings("serial")
public class CachedLayerProvider extends GeoServerDataProvider<CachedLayerInfo> {

    static final Property<CachedLayerInfo> TYPE = new BeanProperty<CachedLayerInfo>("type", "type");

    static final Property<CachedLayerInfo> NAME = new BeanProperty<CachedLayerInfo>("name", "name");

    static final Property<CachedLayerInfo> QUOTA_LIMIT = new BeanProperty<CachedLayerInfo>(
            "quotaLimit", "quotaLimit");

    static final Property<CachedLayerInfo> QUOTA_USAGE = new BeanProperty<CachedLayerInfo>(
            "quotaUsed", "quotaUsed");

    static final Property<CachedLayerInfo> ENABLED = new BeanProperty<CachedLayerInfo>("enabled",
            "enabled");

    @SuppressWarnings("unchecked")
    static final List<Property<CachedLayerInfo>> PROPERTIES = Arrays.asList(TYPE, NAME,
            QUOTA_LIMIT, QUOTA_USAGE, ENABLED);

    /**
     * @see org.geoserver.web.wicket.GeoServerDataProvider#getItems()
     */
    @Override
    protected List<CachedLayerInfo> getItems() {
        GWC gwc = GWC.get();
        return CachedLayerDetachableModel.getItems(gwc);
    }

    /**
     * @see org.geoserver.web.wicket.GeoServerDataProvider#getProperties()
     */
    @Override
    protected List<Property<CachedLayerInfo>> getProperties() {
        return PROPERTIES;
    }

    /**
     * @see org.geoserver.web.wicket.GeoServerDataProvider#newModel(java.lang.Object)
     */
    public IModel<CachedLayerInfo> newModel(final Object cachedLayerInfo) {
        return new CachedLayerDetachableModel((CachedLayerInfo) cachedLayerInfo);
    }

    /**
     * @see org.geoserver.web.wicket.GeoServerDataProvider#getComparator
     */
    @Override
    protected Comparator<CachedLayerInfo> getComparator(SortParam sort) {
        return super.getComparator(sort);
    }
}
