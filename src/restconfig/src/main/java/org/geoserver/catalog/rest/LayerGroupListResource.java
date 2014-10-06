/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerGroupInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class LayerGroupListResource extends AbstractCatalogListResource {

    protected LayerGroupListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, LayerGroupInfo.class, catalog);
    }

    @Override
    protected List handleListGet() throws Exception {
        String ws = getAttribute("workspace");
        LOGGER.fine( "GET all layer groups" + ws != null ? " in workspace " + ws : "");

        //JD:NO_WORKSPACE here is a pretty big hack... figure out how to expose global layer groups
        // through the catalog api in a consistent way
        return ws != null ? catalog.getLayerGroupsByWorkspace(ws) : 
            catalog.getLayerGroupsByWorkspace(CatalogFacade.NO_WORKSPACE);
    }

}
