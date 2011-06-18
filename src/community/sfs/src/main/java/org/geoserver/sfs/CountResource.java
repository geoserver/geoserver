/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.sfs;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

/**
 * Returns a number as a plain string
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CountResource extends Resource {

    private int count;

    public CountResource(Context context, Request request, Response response, int count) {
        super(context, request, response);
        this.count = count;
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
        Representation representation = new StringRepresentation(String.valueOf(count),
                MediaType.TEXT_PLAIN);
        getResponse().setEntity(representation);
    }
}
