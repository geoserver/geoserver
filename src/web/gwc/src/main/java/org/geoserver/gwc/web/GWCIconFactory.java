/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.Serializable;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerBasePage;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;

/** Utility class used to lookup icons for various catalog objects */
@SuppressWarnings("serial")
public class GWCIconFactory implements Serializable {

    public static final PackageResourceReference UNKNOWN_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/error.png");

    public static final PackageResourceReference DISABLED_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/error.png");

    public static final PackageResourceReference ENABLED_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/tick.png");

    public static final PackageResourceReference ADD_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/add.png");

    public static final PackageResourceReference DELETE_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/delete.png");

    public static final PackageResourceReference GRIDSET =
            new PackageResourceReference(GWCSettingsPage.class, "gridset.png");

    public static final PackageResourceReference GWC =
            new PackageResourceReference(GWCSettingsPage.class, "geowebcache-16.png");

    /**
     * Enum of tile layer type to aid in presenting a type column in the UI without incurring in
     * heavy resource lookups such as loading feature types from the geoserver catalog.
     */
    public static enum CachedLayerType {
        VECTOR(PublishedType.VECTOR.getCode()),
        RASTER(PublishedType.RASTER.getCode()),
        REMOTE(PublishedType.REMOTE.getCode()),
        WMS(PublishedType.WMS.getCode()),
        GROUP(PublishedType.GROUP.getCode()),
        WMTS(PublishedType.WMTS.getCode()),
        GWC(-1),
        UNKNOWN(-2);

        private final Integer code;

        CachedLayerType(Integer code) {
            this.code = code;
        }

        public Integer getCode() {
            return code;
        }

        public static CachedLayerType valueOf(Integer code) {
            return values()[code.intValue()];
        }
    }

    private GWCIconFactory() {
        // private constructor, this is a singleton
    }

    /** Returns the appropriate icon for the specified layer type. */
    public static CachedLayerType getCachedLayerType(final TileLayer layer) {
        if (layer instanceof GeoServerTileLayer) {
            GeoServerTileLayer gsTileLayer = (GeoServerTileLayer) layer;
            PublishedInfo published = gsTileLayer.getPublishedInfo();
            PublishedType publishedType = published.getType();
            return CachedLayerType.valueOf(publishedType.getCode());
        }
        if (layer instanceof WMSLayer) {
            return CachedLayerType.GWC;
        }
        return CachedLayerType.UNKNOWN;
    }

    /** Returns the appropriate icon for the specified layer type. */
    public static PackageResourceReference getSpecificLayerIcon(final TileLayer layer) {
        if (layer instanceof GeoServerTileLayer) {
            GeoServerTileLayer gsTileLayer = (GeoServerTileLayer) layer;
            PublishedInfo published = gsTileLayer.getPublishedInfo();
            if (published instanceof LayerInfo) {
                return CatalogIconFactory.get().getSpecificLayerIcon((LayerInfo) published);
            }
            return CatalogIconFactory.GROUP_ICON;
        }
        if (layer instanceof WMSLayer) {
            return GWC;
        }
        return UNKNOWN_ICON;
    }

    /**
     * Returns a reference to a general purpose icon to indicate an enabled/properly configured
     * resource
     */
    public static PackageResourceReference getEnabledIcon() {
        return ENABLED_ICON;
    }

    /**
     * Returns a reference to a general purpose icon to indicate a
     * disabled/misconfigured/unreachable resource
     */
    public static PackageResourceReference getDisabledIcon() {
        return DISABLED_ICON;
    }

    public static PackageResourceReference getErrorIcon() {
        return UNKNOWN_ICON;
    }
}
