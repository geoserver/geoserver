/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import org.apache.wicket.PageParameters;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.layer.TileLayer;

/**
 * Allows editing a specific {@link GeoServerTileLayerInfo}
 */
public class CachedLayerEditPage extends GeoServerSecuredPage {

    /**
     * Uses a "name" parameter to locate the layer
     * 
     * @param parameters
     */
    public CachedLayerEditPage(PageParameters parameters) {
        final String layerName = parameters.getString("name");
        final GWC gwc = GWC.get();
        final TileLayer tileLayer;
        try {
            tileLayer = gwc.getTileLayerByName(layerName);
        } catch (IllegalArgumentException iae) {
            error(new ParamResourceModel("CachedLayerEditPage.notFound", this, layerName)
                    .getString());
            setResponsePage(CachedLayersPage.class);
            return;
        }
    }
}
