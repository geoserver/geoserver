/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * Looks up the describe object
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeFinder extends Finder {

    Catalog catalog;

    public DescribeFinder(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        String layerName = RESTUtils.getAttribute(request, "layer");
        LayerInfo layer = catalog.getLayerByName(layerName);
        
        // any of these conditions mean the layer is not currently 
        // advertised in the capabilities document
        if(layer == null || !layer.isEnabled() || !(layer.getResource() instanceof FeatureTypeInfo)) {
            throw new RestletException( "No such layer: " + layerName, Status.CLIENT_ERROR_NOT_FOUND );
        }
        final FeatureTypeInfo resource = (FeatureTypeInfo) layer.getResource();
        return new DescribeResource(getContext(), request, response, resource);
    }
}
