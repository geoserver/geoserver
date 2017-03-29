package org.geoserver.catalog.rest;

import org.geoserver.config.GeoServer;

/**
 * Base controller implementation for geoserver info requests
 */
public class GeoServerController {
    protected final GeoServer geoServer;

    public GeoServerController(GeoServer geoServer) {
        super();
        this.geoServer = geoServer;
    }
}
