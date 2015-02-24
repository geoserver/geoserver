/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.rest.RestletException;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class WMSStoreFinder extends AbstractCatalogFinder {

    public WMSStoreFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        String ws = (String) request.getAttributes().get( "workspace" );
        String wms = (String) request.getAttributes().get( "wmsstore" );
        
        //ensure referenced resources exist
        if ( ws != null && catalog.getWorkspaceByName(ws) == null) {
            throw new RestletException( "No such workspace: " + ws, Status.CLIENT_ERROR_NOT_FOUND );
        }
        if ( wms != null && catalog.getStoreByName(ws, wms, WMSStoreInfo.class) == null) {
            throw new RestletException( "No such wms store: " + ws + "," + wms, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        if ( wms == null && request.getMethod() == Method.GET ) {
            return new WMSStoreListResource(getContext(),request,response,catalog);
        }
        return new WMSStoreResource( null, request, response, catalog );
    }

}
