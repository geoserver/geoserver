/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.io.Serializable;

import org.apache.wicket.ResourceReference;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerBasePage;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;

/**
 * Utility class used to lookup icons for various catalog objects
 */
@SuppressWarnings("serial")
public class GWCIconFactory implements Serializable {

    public static final ResourceReference UNKNOWN_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/error.png");

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

    public static final ResourceReference GWC = new ResourceReference(GWCSettingsPage.class,
            "geowebcache-16.png");

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
        if (layer instanceof GeoServerTileLayer) {
            GeoServerTileLayer gsTileLayer = (GeoServerTileLayer) layer;
            LayerInfo layerInfo = gsTileLayer.getLayerInfo();
            if (layerInfo != null) {
                return CatalogIconFactory.get().getSpecificLayerIcon(layerInfo);
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
    public static ResourceReference getEnabledIcon() {
        return ENABLED_ICON;
    }

    /**
     * Returns a reference to a general purpose icon to indicate a
     * disabled/misconfigured/unreachable resource
     */
    public static ResourceReference getDisabledIcon() {
        return DISABLED_ICON;
    }

    public static ResourceReference getErrorIcon() {
        return UNKNOWN_ICON;
    }
}
