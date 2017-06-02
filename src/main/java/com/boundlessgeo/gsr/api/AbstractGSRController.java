package com.boundlessgeo.gsr.api;


import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;

/**
 * Parent to all gsr controllers.
 * Provides access to {@link GeoServer} and {@link Catalog}
 */
public class AbstractGSRController {

    protected GeoServer geoServer;
    protected Catalog catalog;

    public AbstractGSRController(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
    }
}
