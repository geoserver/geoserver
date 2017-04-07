package org.geoserver.rest;

import org.geoserver.config.GeoServer;
import org.geoserver.rest.RestBaseController;

/**
 * Base controller implementation for geoserver info requests
 */
public abstract class AbstractGeoServerController extends RestBaseController {
    protected final GeoServer geoServer;

    public AbstractGeoServerController(GeoServer geoServer) {
        super();
        this.geoServer = geoServer;
    }
}
