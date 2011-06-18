/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.sfs;

import org.geoserver.catalog.Catalog;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * Looks up the capabilities object
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CapabilitiesFinder extends Finder {

    Catalog catalog;

    public CapabilitiesFinder(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        return new CapabilitiesResource(getContext(), request, response, catalog);
    }
}
