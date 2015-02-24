/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest.finder;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.geoserver.sldservice.rest.resource.ListAttributesResource;
import org.restlet.Finder;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class LayerAttributesFinder extends Finder {

    /**
     * reference to the catalog
     */
    protected Catalog catalog;
    
    protected LayerAttributesFinder(Catalog catalog) {
        this.catalog = catalog;
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String layer = (String) request.getAttributes().get("layer");
    
        if (layer != null && request.getMethod() == Method.GET) {
            return new ListAttributesResource(getContext(), request, response,
                    catalog);
        }
    
        throw new RestletException("No such layer: " + layer,
                Status.CLIENT_ERROR_NOT_FOUND);
    }

}