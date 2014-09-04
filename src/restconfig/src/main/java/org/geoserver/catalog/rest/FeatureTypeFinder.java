/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class FeatureTypeFinder extends AbstractCatalogFinder {

    protected FeatureTypeFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String ws = getAttribute(request, "workspace");
        String ds = getAttribute(request, "datastore");
        String ft = getAttribute(request, "featuretype");
        
        //ensure referenced resources exist
        if ( ws != null && catalog.getWorkspaceByName( ws ) == null ) {
            throw new RestletException( "No such workspace: " + ws, Status.CLIENT_ERROR_NOT_FOUND );
        }
        if ( ds != null && catalog.getDataStoreByName(ws, ds) == null ) {
            throw new RestletException( "No such datastore: " + ws + "," + ds, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        if ( ft != null ) {
            if ( ds != null &&
                    catalog.getFeatureTypeByDataStore(catalog.getDataStoreByName(ws, ds), ft) == null) {
                throw new RestletException( "No such feature type: "+ws+","+ds+","+ft, Status.CLIENT_ERROR_NOT_FOUND );
            }
            else {
                //look up by workspace/namespace
                NamespaceInfo ns = catalog.getNamespaceByPrefix( ws );
                if ( ns == null || catalog.getFeatureTypeByName( ns, ft ) == null ) {
                    throw new RestletException( "No such feature type: "+ws+","+ft, Status.CLIENT_ERROR_NOT_FOUND );
                }
            }
        }
        else {
            //check the list flag, if == 'available', just return the list 
            // of feature types available
            Form form = request.getResourceRef().getQueryAsForm();
            String list = form.getFirstValue( "list" );
            if ("available".equalsIgnoreCase(list) || "available_with_geom".equalsIgnoreCase(list)) {
                return new AvailableFeatureTypeResource(null,request,response,catalog);
            }
            
            if (request.getMethod() == Method.GET ) {
                return new FeatureTypeListResource(getContext(),request,response,catalog);
            }
        }
        
        return new FeatureTypeResource(null,request,response,catalog);
    }

}
