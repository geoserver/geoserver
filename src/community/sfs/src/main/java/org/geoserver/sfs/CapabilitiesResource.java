/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import org.geoserver.catalog.Catalog;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

/**
 * The Simple Feature Service capabilities document
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CapabilitiesResource extends Resource {
    Catalog catalog;

    public CapabilitiesResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response);
        this.catalog = catalog;
    }

    @Override
    public void handleGet() {
        Representation representation = new CapabilitiesJSONFormat().toRepresentation(catalog);
        getResponse().setEntity(representation);
    }

}
