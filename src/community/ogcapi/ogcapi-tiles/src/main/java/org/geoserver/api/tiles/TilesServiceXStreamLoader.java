/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class TilesServiceXStreamLoader extends XStreamServiceLoader<TilesServiceInfo> {

    public TilesServiceXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "tiles");
    }

    @Override
    protected TilesServiceInfo createServiceFromScratch(GeoServer gs) {
        TilesServiceInfoImpl info = new TilesServiceInfoImpl();
        info.setName("tiles");
        info.setTitle("Tiles server");
        return info;
    }

    @Override
    public Class<TilesServiceInfo> getServiceClass() {
        return TilesServiceInfo.class;
    }
}
