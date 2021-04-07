/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.geowebcache.layer.TileLayer;

/**
 * Interface that can be implemented to customize the Icon management machinery performed by {@link
 * GWCIconFactory} dealing with {@link TileLayer}s
 */
public interface GWCTileLayerIconCustomizer {

    /**
     * Returns a suitable {@link org.geoserver.gwc.web.GWCIconFactory.CachedLayerType} for the
     * specified layer. Returns {@link org.geoserver.gwc.web.GWCIconFactory.CachedLayerType#UNKNOWN}
     * if unknown so that another customizer implementation could try finding a proper one.
     */
    GWCIconFactory.CachedLayerType getCachedLayerType(TileLayer layer);

    /**
     * Returns the appropriate icon for the specified layer type. Returns {@link
     * GWCIconFactory#UNKNOWN_ICON} if unknown so that another customizer implementation could try
     * finding a proper one.
     */
    PackageResourceReference getLayerIcon(TileLayer layer);
}
