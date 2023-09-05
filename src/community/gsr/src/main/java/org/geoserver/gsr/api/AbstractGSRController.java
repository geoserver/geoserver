/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.GSRServiceInfo;
import org.geoserver.ogcapi.APIService;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;

/** Parent to all gsr controllers. Provides access to {@link GeoServer} and {@link Catalog} */
@APIService(
        service = "GSR",
        version = "10.51",
        landingPage = "gsr/services",
        core = false,
        serviceClass = GSRServiceInfo.class)
public class AbstractGSRController {

    protected GeoServer geoServer;
    protected Catalog catalog;
    protected static final FilterFactory FILTERS = CommonFactoryFinder.getFilterFactory();

    public AbstractGSRController(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
    }
}
