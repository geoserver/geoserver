/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class CoverageFinder extends AbstractCatalogFinder {

    public CoverageFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String ws = getAttribute(request, "workspace");
        String cs = getAttribute(request, "coveragestore");
        String c = getAttribute(request, "coverage");

        //ensure referenced resources exist
        if ( ws != null && catalog.getWorkspaceByName( ws ) == null ) {
            throw new RestletException( "No such workspace: " + ws, Status.CLIENT_ERROR_NOT_FOUND );
        }
        if ( cs != null && catalog.getCoverageStoreByName(ws, cs) == null ) {
            throw new RestletException( "No such coveragestore: " + ws + "," + cs, Status.CLIENT_ERROR_NOT_FOUND );
        }

        if (c == null && request.getMethod() == Method.GET) {
            Form form = request.getResourceRef().getQueryAsForm();
            String list = form.getFirstValue("list");
            if ("all".equalsIgnoreCase(list)) {
                return new HarvestedCoveragesResource(getContext(), request, response, catalog);
            } else {
                return new CoverageListResource(getContext(), request, response, catalog);
            }

        }

        if ( c != null ) {
            if ( cs != null && catalog.getCoverageByCoverageStore(catalog.getCoverageStoreByName(ws, cs), c) == null) {
                throw new RestletException( "No such coverage: "+ws+","+cs+","+c, Status.CLIENT_ERROR_NOT_FOUND );
            } else {
                //look up by workspace/namespace
                NamespaceInfo ns = catalog.getNamespaceByPrefix( ws );
                if ( ns == null || catalog.getCoverageByName( ns, c ) == null ) {
                    throw new RestletException( "No such coverage: "+ws+","+c, Status.CLIENT_ERROR_NOT_FOUND );
                }
            }
        }
        return new CoverageResource(null,request,response,catalog);
    }

}
