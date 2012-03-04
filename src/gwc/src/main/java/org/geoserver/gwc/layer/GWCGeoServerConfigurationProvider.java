/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import org.geowebcache.config.XMLConfigurationProvider;

import com.thoughtworks.xstream.XStream;

public class GWCGeoServerConfigurationProvider implements XMLConfigurationProvider {

    @Override
    public XStream getConfiguredXStream(XStream xs) {
        xs.alias("GeoServerTileLayer", GeoServerTileLayerInfo.class);
        xs.addDefaultImplementation(GeoServerTileLayerInfoImpl.class, GeoServerTileLayerInfo.class);
        return xs;
    }
}
