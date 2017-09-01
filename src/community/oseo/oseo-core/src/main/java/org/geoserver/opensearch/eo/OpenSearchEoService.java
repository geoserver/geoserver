/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.Collection;

import org.geotools.data.Parameter;

public interface OpenSearchEoService {

    /**
     * Returns the request for the response object to produce either the global or the collection specific response
     * 
     * @param request
     * @return
     * @throws IOException
     */
    public OSEODescription description(OSEODescriptionRequest request) throws IOException;

    /**
     * Searches either collection or products, returned as complex features
     * 
     * @param request
     * @return
     * @throws IOException 
     */
    public SearchResults search(SearchRequest request) throws IOException;

    /**
     * Returns the search parameters applicable to collections
     * 
     * @param service
     * @return
     * @throws IOException
     */
    Collection<Parameter<?>> getCollectionSearchParameters() throws IOException;

    /**
     * Returns the search parameters applicable to products of a certain collection
     * 
     * @param service
     * @param parentId
     * @return
     * @throws IOException
     */
    Collection<Parameter<?>> getProductSearchParameters(String parentId) throws IOException;
}
