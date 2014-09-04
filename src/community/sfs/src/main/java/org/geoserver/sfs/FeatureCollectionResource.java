/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

/**
 * The Simple Feature Service data resource
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureCollectionResource extends Resource {
    SimpleFeatureCollection features;

    public FeatureCollectionResource(Context context, Request request, Response response,
            SimpleFeatureCollection features) {
        super(context, request, response);
        this.features = features;
    }
    
    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        handleGet();
    }
    
    @Override
    public void handleGet() {
        Representation representation = new FeaturesJSONFormat().toRepresentation(features);
        getResponse().setEntity(representation);
    }

}
