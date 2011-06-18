/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
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

public class LayerGroupFinder extends AbstractCatalogFinder {

    public LayerGroupFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String lg = getAttribute(request, "layergroup");
        if ( lg != null && catalog.getLayerGroupByName( lg ) == null ) {
            throw new RestletException( "No such layer group " + lg, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        if ( lg == null && request.getMethod() == Method.GET ) {
            return new LayerGroupListResource( getContext(), request, response, catalog );
        }
        
        return new LayerGroupResource( getContext(), request, response, catalog );
    }

}
