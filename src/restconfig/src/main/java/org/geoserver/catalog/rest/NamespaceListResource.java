/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.rest.NamespaceResource.NamespaceHTMLFormat;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class NamespaceListResource extends AbstractCatalogListResource {

    protected NamespaceListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, NamespaceInfo.class, catalog);
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new NamespaceHTMLFormat( request, response, this, catalog );
    }
    @Override
    protected List handleListGet() throws Exception {
        LOGGER.fine( "GET all namespaces");
        return catalog.getNamespaces();
    }

}
