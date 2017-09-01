/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class CoverageListResource extends AbstractCatalogListResource {

    protected CoverageListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, CoverageInfo.class, catalog);
    }

    @Override
    protected List handleListGet() throws Exception {
        String ws = getAttribute("workspace");
        String cs = getAttribute("coveragestore");
        
        if ( cs != null ) {
            CoverageStoreInfo coverageStore = catalog.getCoverageStoreByName(ws, cs);
            return catalog.getCoveragesByCoverageStore(coverageStore);    
        }
        
        NamespaceInfo ns = catalog.getNamespaceByPrefix( ws );
        return catalog.getCoveragesByNamespace( ns );
    }

}
