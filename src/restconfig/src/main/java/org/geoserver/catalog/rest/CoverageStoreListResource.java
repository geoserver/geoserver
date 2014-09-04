/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.rest.CoverageStoreResource.CoverageStoreHTMLFormat;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class CoverageStoreListResource extends AbstractCatalogListResource {

    public CoverageStoreListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, CoverageStoreInfo.class, catalog);
        
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new CoverageStoreHTMLFormat( request, response, this, catalog );
    }
    
    @Override
    protected List handleListGet() throws Exception {
        String ws = getAttribute( "workspace");
        return catalog.getCoverageStoresByWorkspace( ws );
    }

}
