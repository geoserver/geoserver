/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tile;

import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.v1.tiles.TilesService;
import org.geoserver.ogcapi.v1.tiles.TilesServiceInfo;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;

public class TilesServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<TilesServiceInfo, TilesService> {

    public TilesServiceDescriptionProvider(GeoServer gs) {
        super(gs, "WMTS", "Tiles", "Tiles");
    }
}
