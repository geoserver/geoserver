/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestletException;
import org.geotools.util.logging.Logging;
import org.restlet.Finder;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class CatalogReloader extends Finder {

    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");
    GeoServer geoServer;

    public CatalogReloader(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        if (!(request.getMethod() == Method.POST || request.getMethod() == Method.PUT)) {
            response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
            return null;
        }
        return new Resource() {
            @Override
            public boolean allowPost() {
                return true;
            }
            
            @Override
            public boolean allowPut() {
                return true;
            }
            
            @Override
            public void handlePost() {
                try {   
                    reloadCatalog();
                } catch (Exception e) {
                    throw new RestletException("Error reloading catalog", Status.SERVER_ERROR_INTERNAL, e);
                }
            }
            
            @Override
            public void handlePut() {
                handlePost();
            }
        };
    }

    /**
     * Method to reload the catalog
     */
    protected void reloadCatalog() throws Exception {
        geoServer.reload();
    }
}
