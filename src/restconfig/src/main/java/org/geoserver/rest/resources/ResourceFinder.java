/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 *
 * @author Niels Charlier
 *
 */
public class ResourceFinder extends Finder {
    
    private ResourceStore store;
    
    public ResourceFinder(ResourceStore store) {
        this.store = store;
        
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {        
        String path = request.getResourceRef().getRelativeRef().getPath();
        if (".".equals(path)) { //root
            path = "/";
        }
        //format
        request.getAttributes().put("format", RESTUtils.getQueryStringValue(request, "format"));
        
        //operation
        ResourceResource.Operation operation = ResourceResource.Operation.DEFAULT;
        String strOp = RESTUtils.getQueryStringValue(request, "operation");
        if (strOp != null) {
            try {
                operation = ResourceResource.Operation.valueOf(strOp.trim().toUpperCase());
            } catch (IllegalArgumentException e) {}
        }
                
        return new ResourceResource(getContext(), request, response, store.get(path), operation, store);
    }

}
