/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.rest.RestletException;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.opengis.coverage.grid.GridCoverageReader;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class StructuredGridCoverageFinder extends AbstractCatalogFinder {

    public StructuredGridCoverageFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        String ws = getAttribute(request, "workspace");
        String cs = getAttribute(request, "coveragestore");
        String c = getAttribute(request, "coverage");
        String lastSegment = request.getResourceRef().getLastSegment();
        boolean granules = lastSegment.equals("granules") || lastSegment.matches("granules\\..*");
        String granule = getAttribute(request, "granule");
        
        // ensure referenced resources exist
        if(ws == null || cs == null || c == null) {
            throw new RestletException( "Invalid path, workspace, store and coverage name must be specified", Status.CLIENT_ERROR_NOT_FOUND );
        }
        if (catalog.getWorkspaceByName( ws ) == null ) {
            throw new RestletException( "No such workspace: " + ws, Status.CLIENT_ERROR_NOT_FOUND );
        }
        CoverageStoreInfo store = catalog.getCoverageStoreByName(ws, cs);
        if (store == null ) {
            throw new RestletException( "No such coveragestore: " + ws + "," + cs, Status.CLIENT_ERROR_NOT_FOUND );
        }
        CoverageInfo coverage = catalog.getCoverageByCoverageStore(store, c);
        if (coverage == null ) {
            throw new RestletException( "No such coverage: " + c + " in store " + ws + "/" + cs, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        // is it a structured grid coverage reader?
        try {
            GridCoverageReader reader = coverage.getGridCoverageReader(null, null);
            if(!(reader instanceof StructuredGridCoverage2DReader)) {
                throw new RestletException( "Coverage exists, but is no structured grid coverage, no index and inner granules are available", Status.CLIENT_ERROR_NOT_FOUND);
            }
            
            // fine, let's see what kind of resource we are looking for
            if(!granules && granule == null) {
                return new StructuredGridCoverageIndexResource(getContext(), request, response, catalog, coverage);
            } else if(granule == null) {
                if(granules) {
                    return new GranulesResource(getContext(), request, response, catalog, coverage);
                } else {
                    throw new RestletException( "Invalid path", Status.CLIENT_ERROR_NOT_FOUND);
                }
            } else {
                // Check if the quietOnNotFound parameter is set
                boolean quietOnNotFound=quietOnNotFoundEnabled(request);
                GranuleResource granuleResource = new GranuleResource(getContext(), request, response, catalog, coverage, granule);
                try {
                    // Check if the granule is present
                    granuleResource.handleObjectGet();
                } catch (Exception e) {
                    // If it is a RestletException the granule is not present
                    if(e instanceof RestletException && ((RestletException)e).getStatus().equals(Status.CLIENT_ERROR_NOT_FOUND)){
                        // If true, no exception is returned
                        if(quietOnNotFound){
                            return null;
                            
                        }
                        throw (RestletException)e;
                    }else{
                        throw new IOException(e);
                    }
                }
                return granuleResource;
            }
        } catch(IOException e) {
            throw new RestletException( "Failed to load coverage information", Status.SERVER_ERROR_INTERNAL, e);
        }
        
        
    }

}
