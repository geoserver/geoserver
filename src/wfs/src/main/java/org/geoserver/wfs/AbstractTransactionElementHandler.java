/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.config.GeoServer;

/**
 * Base class for transaction element handlers.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class AbstractTransactionElementHandler implements TransactionElementHandler {

    protected GeoServer geoServer;

    protected AbstractTransactionElementHandler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    protected WFSInfo getInfo() {
        return geoServer.getService(WFSInfo.class);
    }
}
