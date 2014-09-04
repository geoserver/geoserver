/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.WorkspaceResource.WorkspaceHTMLFormat;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class WorkspaceListResource extends AbstractCatalogListResource {

    public WorkspaceListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, WorkspaceInfo.class, catalog);
    }

    @Override
    protected List handleListGet() throws Exception {
        LOGGER.fine( "GET all workspaces" );
        return catalog.getWorkspaces();
    }
    
    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new WorkspaceHTMLFormat(request,response,this,catalog);
    }
}
