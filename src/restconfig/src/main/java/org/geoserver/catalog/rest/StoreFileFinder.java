/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.HashMap;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.rest.RestletException;
import org.geotools.data.DataStoreFactorySpi;
import org.opengis.coverage.grid.Format;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class StoreFileFinder extends AbstractCatalogFinder {

    
    protected static HashMap<String,String> formatToCoverageStoreFormat = new HashMap();
    static {
        for (Format format : CoverageStoreUtils.formats) {
            formatToCoverageStoreFormat.put(format.getName().toLowerCase(), format.getName());
        }
    }
    
    public StoreFileFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        
        //figure out what kind of store this maps to
        String format = getAttribute(request, "format");
        String datastore = getAttribute(request, "datastore");
        String coveragestore = getAttribute(request, "coveragestore");
        
        if ( datastore != null ) {
            return new DataStoreFileResource(request,response,format,catalog);
        }
        else {
            String coverageFormatName = formatToCoverageStoreFormat.get( format );
            
            if ( coverageFormatName == null ) {
                throw new RestletException( "Unsupported format: " + format + ", available formats are: " 
                        + formatToCoverageStoreFormat.keySet().toString(), Status.CLIENT_ERROR_BAD_REQUEST);
            }
            
            Format coverageFormat = null;
            try {
                coverageFormat = CoverageStoreUtils.acquireFormat( coverageFormatName );
            }
            catch( Exception e ) {
                throw new RestletException( "Coveragestore format unavailable: " + coverageFormatName, Status.SERVER_ERROR_INTERNAL );
            }
            
            return new CoverageStoreFileResource(request,response,coverageFormat,catalog);
        }
        
    }
}
