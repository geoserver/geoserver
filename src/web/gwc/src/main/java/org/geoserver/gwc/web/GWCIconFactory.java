/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.CatalogIconFactory;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;

/** Utility class used to lookup icons for various catalog objects */
@SuppressWarnings("serial")
public class GWCIconFactory implements Serializable {

    public static final String UNKNOWN_ICON = "gs-icon-error";

    public static final String DISABLED_ICON = "gs-icon-error";

    public static final String ENABLED_ICON = "gs-icon-tick";

    public static final String ADD_ICON = "gs-icon-add";

    public static final String DELETE_ICON = "gs-icon-delete";

    public static final ResourceReference GRIDSET = new PackageResourceReference(GWCSettingsPage.class, "gridset.png");

    public static final ResourceReference GWC =
            new PackageResourceReference(GWCSettingsPage.class, "geowebcache-16.png");

    /**
     * Enum of tile layer type to aid in presenting a type column in the UI without incurring in heavy resource lookups
     * such as loading feature types from the geoserver catalog.
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

    public static CachedLayerType getCachedLayerType(final TileLayer layer) {
        if (layer instanceof GeoServerTileLayer gsTileLayer) {
            PublishedInfo published = gsTileLayer.getPublishedInfo();
            PublishedType publishedType = published.getType();
            return CachedLayerType.valueOf(publishedType.getCode());
        }
        if (layer instanceof WMSLayer) {
            return CachedLayerType.GWC;
        }

        List<GWCTileLayerIconCustomizer> iconCustomizers =
                GeoServerExtensions.extensions(GWCTileLayerIconCustomizer.class);
        for (GWCTileLayerIconCustomizer iconCustomizer : iconCustomizers) {
            CachedLayerType cachedLayerType = iconCustomizer.getCachedLayerType(layer);
            // Stop scanning through the registered customizers as soon as the
            // suggested type is not UNKNOWN
            if (cachedLayerType != null && cachedLayerType != CachedLayerType.UNKNOWN) {
                return cachedLayerType;
            }
        }

        return CachedLayerType.UNKNOWN;
    }

    /** Returns the appropriate icon for the specified layer type. */
    public static String getSpecificLayerIcon(final TileLayer layer) {
        if (layer instanceof GeoServerTileLayer gsTileLayer) {
            PublishedInfo published = gsTileLayer.getPublishedInfo();
            if (published instanceof LayerInfo info) {
                return CatalogIconFactory.get().getSpecificLayerIcon(info);
            }
            return CatalogIconFactory.GROUP_ICON;
        }
        if (layer instanceof WMSLayer) {
            return "gs-icon-map";
        }

        List<GWCTileLayerIconCustomizer> iconCustomizers =
                GeoServerExtensions.extensions(GWCTileLayerIconCustomizer.class);
        for (GWCTileLayerIconCustomizer iconCustomizer : iconCustomizers) {
            // Stop scanning through the registered customizers as soon as the
            // suggested icon is not the UNKNOWN_ICON
            String ref = iconCustomizer.getLayerIcon(layer);
            if (ref != null && !UNKNOWN_ICON.equals(ref)) {
                return ref;
            }
        }

        return UNKNOWN_ICON;
    }

    /** Returns a reference to a general purpose icon to indicate an enabled/properly configured resource */
    public static String getEnabledIcon() {
        return ENABLED_ICON;
    }

    /** Returns a reference to a general purpose icon to indicate a disabled/misconfigured/unreachable resource */
    public static String getDisabledIcon() {
        return DISABLED_ICON;
    }

    public static String getErrorIcon() {
        return UNKNOWN_ICON;
    }
}
