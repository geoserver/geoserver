/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.Serializable;

import org.apache.wicket.ResourceReference;
import org.geoserver.web.GeoServerBasePage;

/**
 * Utility class used to lookup icons for various catalog objects
 */
@SuppressWarnings("serial")
public class GWCIconFactory implements Serializable {

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

    static final GWCIconFactory INSTANCE = new GWCIconFactory();

    public static final GWCIconFactory get() {
        return INSTANCE;
    }

    private GWCIconFactory() {
        // private constructor, this is a singleton
    }

    /**
     * Returns the appropriate icon for the specified layer type.
     * 
     * @param info
     * @return
     */
    public ResourceReference getSpecificLayerIcon(final CachedLayerInfo.TYPE type) {
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

    /**
     * Returns a reference to a general purpose icon to indicate an enabled/properly configured
     * resource
     */
    public ResourceReference getEnabledIcon() {
        return ENABLED_ICON;
    }

    /**
     * Returns a reference to a general purpose icon to indicate a
     * disabled/missconfigured/unreachable resource
     */
    public ResourceReference getDisabledIcon() {
        return DISABLED_ICON;
    }

    public ResourceReference getErrorIcon() {
        return UNKNOWN_ICON;
    }
}
