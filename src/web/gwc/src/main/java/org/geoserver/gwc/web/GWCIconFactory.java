/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.Serializable;
import java.util.logging.Logger;

import org.apache.wicket.ResourceReference;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.web.GeoServerBasePage;
import org.geotools.util.logging.Logging;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;

/**
 * Utility class used to lookup icons for various catalog objects
 */
@SuppressWarnings("serial")
public class GWCIconFactory implements Serializable {

    private static final Logger LOGGER = Logging.getLogger(GWCIconFactory.class);

    public static enum TYPE {
        VECTOR, RASTER, LAYERGROUP, WMS, OTHER;
    }

    public static final ResourceReference RASTER_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/geosilk/raster.png");

    public static final ResourceReference VECTOR_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/geosilk/vector.png");

    public static final ResourceReference MAP_ICON = new ResourceReference(GeoServerBasePage.class,
            "img/icons/geosilk/map.png");

    public static final ResourceReference MAP_STORE_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/geosilk/server_map.png");

    public static final ResourceReference POINT_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/bullet_blue.png");

    public static final ResourceReference LINE_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/line_blue.png");

    public static final ResourceReference POLYGON_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/shape_square_blue.png");

    public static final ResourceReference GEOMETRY_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/geosilk/vector.png");

    public static final ResourceReference UNKNOWN_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/error.png");

    public static final ResourceReference GROUP_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/layers.png");

    public static final ResourceReference DISABLED_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/error.png");

    public static final ResourceReference ENABLED_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/tick.png");

    public static final ResourceReference ADD_ICON = new ResourceReference(GeoServerBasePage.class,
            "img/icons/silk/add.png");

    public static final ResourceReference DELETE_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/delete.png");

    public static final ResourceReference GRIDSET = new ResourceReference(GWCSettingsPage.class,
            "gridset.png");

    static final GWCIconFactory INSTANCE = new GWCIconFactory();

    private GWCIconFactory() {
        // private constructor, this is a singleton
    }

    /**
     * Returns the appropriate icon for the specified layer type.
     * 
     * @param info
     * @return
     */
    public static ResourceReference getSpecificLayerIcon(final TileLayer layer) {
        final TYPE type = getType(layer);
        switch (type) {
        case RASTER:
            return RASTER_ICON;
        case VECTOR:
            return VECTOR_ICON;
        case LAYERGROUP:
            return GROUP_ICON;
        case WMS:
            return MAP_ICON;
        default:
            return UNKNOWN_ICON;
        }
    }

    public static TYPE getType(final TileLayer layer) {
        if (layer instanceof WMSLayer) {
            return TYPE.WMS;
        } else if (layer instanceof GeoServerTileLayer) {
            GeoServerTileLayer gtl = (GeoServerTileLayer) layer;
            LayerInfo li;
            if (null != (li = gtl.getLayerInfo())) {
                ResourceInfo resource = li.getResource();
                if (resource instanceof FeatureTypeInfo) {
                    return TYPE.VECTOR;
                } else if (resource instanceof CoverageInfo) {
                    return TYPE.RASTER;
                } else if (resource instanceof WMSLayerInfo) {
                    return TYPE.WMS;
                }
            } else if (null != gtl.getLayerGroupInfo()) {
                return TYPE.LAYERGROUP;
            }
        }
        LOGGER.info("Unknown TileLayer type, returning OTHER: " + layer.getClass().getName());
        return TYPE.OTHER;
    }

    /**
     * Returns a reference to a general purpose icon to indicate an enabled/properly configured
     * resource
     */
    public static ResourceReference getEnabledIcon() {
        return ENABLED_ICON;
    }

    /**
     * Returns a reference to a general purpose icon to indicate a
     * disabled/missconfigured/unreachable resource
     */
    public static ResourceReference getDisabledIcon() {
        return DISABLED_ICON;
    }

    public static ResourceReference getErrorIcon() {
        return UNKNOWN_ICON;
    }
}
