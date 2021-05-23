/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.mbtiles;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.gwc.web.GWCTileLayerIconCustomizer;
import org.geoserver.web.CatalogIconFactory;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mbtiles.layer.MBTilesLayer;

/**
 * The {@link GWCTileLayerIconCustomizer} implementation for MBTilesLayer (that can be both raster
 * or vector)
 */
public class MBTilesLayerIconCustomizer implements GWCTileLayerIconCustomizer {

    @Override
    public GWCIconFactory.CachedLayerType getCachedLayerType(TileLayer layer) {
        if (layer instanceof MBTilesLayer) {
            return GWCIconFactory.CachedLayerType.GWC;
        }
        return GWCIconFactory.CachedLayerType.UNKNOWN;
    }

    @Override
    public PackageResourceReference getLayerIcon(TileLayer layer) {
        if (layer instanceof MBTilesLayer) {
            MBTilesLayer mbTilesLayer = (MBTilesLayer) layer;
            if (mbTilesLayer.isVectorTiles()) {
                return CatalogIconFactory.GEOMETRY_ICON;
            } else {
                return CatalogIconFactory.RASTER_ICON;
            }
        }
        return CatalogIconFactory.UNKNOWN_ICON;
    }
}
