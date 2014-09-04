/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class DataStoreFinder extends AbstractCatalogFinder {

    public DataStoreFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        String ws = getAttribute(request, "workspace");
        String ds = getAttribute(request, "datastore");
        
        //ensure referenced resources exist
        if ( ws != null && catalog.getWorkspaceByName(ws) == null) {
            throw new RestletException( "No such workspace: " + ws, Status.CLIENT_ERROR_NOT_FOUND );
        }
        if ( ds != null && catalog.getDataStoreByName(ws, ds) == null && !"default".equals(ds)) {
            throw new RestletException( "No such datastore: " + ws + "," + ds, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        if ( ds == null && request.getMethod() == Method.GET ) {
            return new DataStoreListResource(getContext(),request,response,catalog);
        }
        return new DataStoreResource( null, request, response, catalog );
    }

}
