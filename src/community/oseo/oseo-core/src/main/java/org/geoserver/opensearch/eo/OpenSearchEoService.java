/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;

import org.geotools.feature.FeatureCollection;

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
     */
    public FeatureCollection search(SearchRequest request);
}
