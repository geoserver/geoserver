/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
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
    boolean forceReset;

    /**
     * Would normally do a reload, unless the force reset flag is enabled, in that case a reset is done
     * @param geoServer
     * @param forceReset
     */
    public CatalogReloader(GeoServer geoServer, boolean forceReset) {
        this.geoServer = geoServer;
        this.forceReset = forceReset;
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
        if(forceReset) {
            geoServer.reset();
        } else {
            geoServer.reload();
        }
    }
}
