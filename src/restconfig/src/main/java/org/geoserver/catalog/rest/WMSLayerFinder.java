/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class WMSLayerFinder extends AbstractCatalogFinder {

    protected WMSLayerFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String ws = (String) request.getAttributes().get( "workspace" );
        String wms = (String) request.getAttributes().get( "wmsstore" );
        String wl = (String) request.getAttributes().get( "wmslayer");
        
        //ensure referenced resources exist
        if ( ws != null && catalog.getWorkspaceByName( ws ) == null ) {
            throw new RestletException( "No such workspace: " + ws, Status.CLIENT_ERROR_NOT_FOUND );
        }
        if ( wms != null && catalog.getStoreByName(ws, wms, WMSStoreInfo.class) == null ) {
            throw new RestletException( "No such wms store: " + ws + "," + wms, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        if ( wl != null ) {
            // Check if the quietOnNotFound parameter is set
            boolean quietOnNotFound=quietOnNotFoundEnabled(request);            
            if ( wms != null &&
                    catalog.getResourceByStore(catalog.getStoreByName(ws, wms, WMSStoreInfo.class), wl, WMSLayerInfo.class) == null) {          
                // If true, no exception is returned
                if(quietOnNotFound){
                    return null;
                }                  
                throw new RestletException( "No such cascaded wms layer: "+ws+","+wms+","+wl, Status.CLIENT_ERROR_NOT_FOUND );
            } else {
                //look up by workspace/namespace
                NamespaceInfo ns = catalog.getNamespaceByPrefix( ws );
                if ( ns == null || catalog.getResourceByName( ns, wl , WMSLayerInfo.class ) == null ) {
                    // If true, no exception is returned
                    if(quietOnNotFound){
                        return null;
                    }     
                    throw new RestletException( "No such cascaded wms: "+ws+","+wl, Status.CLIENT_ERROR_NOT_FOUND );
                }
            }
        }  else {
            // check the list flag, if == 'available', just return the list 
            // of available cascaded layers
            Form form = request.getResourceRef().getQueryAsForm();
            if ( "available".equalsIgnoreCase( form.getFirstValue( "list" ) ) ) {
                return new AvailableWMSLayerResource(null,request,response,catalog);
            }
            
            if (request.getMethod() == Method.GET ) {
                return new WMSLayerListResource(getContext(),request,response,catalog);
            }
        }
        
        return new WMSLayerResource(null,request,response,catalog);
    }

}
