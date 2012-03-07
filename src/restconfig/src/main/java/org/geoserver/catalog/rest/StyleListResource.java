/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.Collection;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class StyleListResource extends AbstractCatalogListResource {

    protected StyleListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, StyleInfo.class, catalog);
        
    }

    @Override
    protected Collection handleListGet() throws Exception {
        String workspace = getAttribute("workspace");
        String layer = getAttribute("layer");
        if ( layer != null ) {
            LOGGER.fine( "GET styles for layer " + layer );
            return catalog.getLayerByName( layer ).getStyles();
        }
        else if (workspace != null) {
            LOGGER.fine( "GET styles for workspace " + workspace );
            return catalog.getStylesByWorkspace(workspace);
        }
        LOGGER.fine( "GET styles" );
        return catalog.getStyles();
    }

}
