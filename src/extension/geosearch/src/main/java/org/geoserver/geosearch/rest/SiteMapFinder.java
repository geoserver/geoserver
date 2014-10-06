/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import org.geoserver.catalog.Catalog;
import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * Finder for {@link SiteMap}
 */
public class SiteMapFinder extends Finder {

    private Catalog catalog;

    private SiteMapFinder(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        if (!Method.GET.equals(request.getMethod())) {
            response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
            return null;
        }
        final Context context = getContext();
        return new SiteMap(context, request, response, catalog);
    }
}
