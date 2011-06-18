/**
 * 
 */
package org.geoserver.sldservice.rest.finder;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.geoserver.rest.RestletException;
import org.geoserver.sldservice.rest.resource.RasterizerResource;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * @author Alessio
 *
 */
public class RasterizerResourceFinder extends AbstractCatalogFinder {

	public RasterizerResourceFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String layer = (String) request.getAttributes().get( "layer" );
        
        if ( layer != null) {
            return new RasterizerResource(getContext(),request,response,catalog);
        }
        
        throw new RestletException( "No such layer: " + layer, Status.CLIENT_ERROR_NOT_FOUND );
    }
    
}
