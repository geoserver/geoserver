/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import org.geowebcache.config.XMLConfigurationProvider;

import com.thoughtworks.xstream.XStream;

/**
 * GWC xml configuration {@link XMLConfigurationProvider contributor} so that GWC knows how to
 * (x)stream instances of {@link GeoServerTileLayerInfo} for the configuration storage subsystem.
 * <p>
 * Note this provider is to store the tile layer configuration representations, which is different
 * from the {@link GWCGeoServerRESTConfigurationProvider REST provider}, which helps in marshaling
 * and unmarshaling {@link GeoServerTileLayer} objects for the GWC REST API.
 * 
 */
public class GWCGeoServerConfigurationProvider implements XMLConfigurationProvider {

    @Override
    public XStream getConfiguredXStream(XStream xs) {
        xs.alias("GeoServerTileLayer", GeoServerTileLayerInfo.class);
        xs.addDefaultImplementation(GeoServerTileLayerInfoImpl.class, GeoServerTileLayerInfo.class);
        return xs;
    }
}
