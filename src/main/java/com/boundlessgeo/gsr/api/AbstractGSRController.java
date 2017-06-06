/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.api;


import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory2;

/**
 * Parent to all gsr controllers.
 * Provides access to {@link GeoServer} and {@link Catalog}
 */
public class AbstractGSRController {

    protected GeoServer geoServer;
    protected Catalog catalog;
    protected static final FilterFactory2 FILTERS = CommonFactoryFinder.getFilterFactory2();

    public AbstractGSRController(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
    }
}
