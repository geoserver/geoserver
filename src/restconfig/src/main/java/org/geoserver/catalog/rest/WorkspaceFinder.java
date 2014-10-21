/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class WorkspaceFinder extends AbstractCatalogFinder {

    public WorkspaceFinder(Catalog catalog) {
        super(catalog);
        
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String workspace = getAttribute(request, "workspace");
        
        if ( workspace == null && request.getMethod() == Method.GET ) {
            return new WorkspaceListResource( null, request, response, catalog );
        }
        
        if ( workspace != null ) {
            // Check if the quietOnNotFound parameter is set
            boolean quietOnNotFound=quietOnNotFoundEnabled(request);            
            //ensure it exists
            if ( catalog.getWorkspaceByName( workspace ) == null ) {
                // If true, no exception is returned
                if(quietOnNotFound){
                    return null;
                }else {
                    throw new RestletException( "No such workspace: " + workspace, Status.CLIENT_ERROR_NOT_FOUND );
                }
            }
        }
        
        return new WorkspaceResource( null, request, response, catalog );
    }

}
