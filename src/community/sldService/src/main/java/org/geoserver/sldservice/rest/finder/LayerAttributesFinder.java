package org.geoserver.sldservice.rest.finder;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.geoserver.rest.RestletException;
import org.geoserver.sldservice.rest.resource.ListAttributesResource;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class LayerAttributesFinder extends AbstractCatalogFinder {

    public LayerAttributesFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String layer = (String) request.getAttributes().get( "layer" );
        
        if ( layer != null && request.getMethod() == Method.GET ) {
            return new ListAttributesResource(getContext(),request,response,catalog);
        }
        
        throw new RestletException( "No such layer: " + layer, Status.CLIENT_ERROR_NOT_FOUND );
    }

}