/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.IOException;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.csw.store.RepositoryItem;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.platform.ServiceException;

/**
 * Runs the GetRepositoryItem request
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetRepositoryItem {

    CSWInfo csw;

    CatalogStore store;

    public GetRepositoryItem(CSWInfo csw, CatalogStore store) {
        this.csw = csw;
        this.store = store;
    }

    /** Returns the requested RepositoryItem */
    public RepositoryItem run(GetRepositoryItemType request) {
        try {
            RepositoryItem item = store.getRepositoryItem(request.getId());
            if (item == null) {
                // by spec we have to return a 404
                throw new HttpErrorCodeException(
                        404, "No repository item found for id " + request.getId());
            }
            return item;
        } catch (IOException e) {
            throw new ServiceException("Failed to load the repository item", e);
        }
    }
}
