/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;

public class NetCDFOutInitializer implements GeoServerInitializer {

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        // Add a new Element to the metadata map
        GeoServerInfo global = geoServer.getGlobal();
        MetadataMap metadata = global.getSettings().getMetadata();
        if (!metadata.containsKey(NetCDFSettingsContainer.NETCDFOUT_KEY)) {
            metadata.put(NetCDFSettingsContainer.NETCDFOUT_KEY, new NetCDFSettingsContainer());
            geoServer.save(global);
        }
    }
}
