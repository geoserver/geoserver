/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.geoserver.config.GeoServer;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class AbstractGeoServerFinder extends AbstractCatalogFinder {

    protected GeoServer geoServer;

    protected AbstractGeoServerFinder(GeoServer geoServer) {
        super(geoServer.getCatalog());
        this.geoServer = geoServer;
    }

}
