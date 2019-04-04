/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.config.GeoServer;

/** Base controller implementation for geoserver info requests */
public abstract class AbstractGeoServerController extends RestBaseController {
    protected final GeoServer geoServer;

    public AbstractGeoServerController(GeoServer geoServer) {
        super();
        this.geoServer = geoServer;
    }
}
