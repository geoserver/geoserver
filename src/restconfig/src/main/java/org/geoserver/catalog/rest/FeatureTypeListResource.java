/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class FeatureTypeListResource extends AbstractCatalogListResource {

    public FeatureTypeListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, FeatureTypeInfo.class, catalog);
    }

    @Override
    protected List handleListGet() throws Exception {
        String ws = getAttribute( "workspace" ); 
        String ds = getAttribute("datastore");
        
        if ( ds != null ) {
            DataStoreInfo dataStore = catalog.getDataStoreByName(ws, ds);
            return catalog.getFeatureTypesByDataStore(dataStore);    
        }
        
        NamespaceInfo ns = catalog.getNamespaceByPrefix( ws );
        return catalog.getFeatureTypesByNamespace( ns );
    }

}
