/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

/**
 * Returns the bounds as a json array
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class BoundsResource extends Resource {

    private ReferencedEnvelope bounds;

    public BoundsResource(Context context, Request request, Response response,
            ReferencedEnvelope bounds) {
        super(context, request, response);
        this.bounds = bounds;
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
        String bbox = "[" + bounds.getMinX() + ", " + bounds.getMinY() + ", " + bounds.getMaxX()
                + ", " + bounds.getMaxY() + "]";
        Representation representation = new StringRepresentation(bbox, MediaType.APPLICATION_JSON);
        getResponse().setEntity(representation);
    }
}
