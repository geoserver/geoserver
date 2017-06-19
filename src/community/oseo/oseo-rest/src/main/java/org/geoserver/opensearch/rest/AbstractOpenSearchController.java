/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;

import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.springframework.http.HttpStatus;

/**
 * Base class for OpenSearch related REST controllers
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractOpenSearchController extends RestBaseController {

    protected OpenSearchAccessProvider accessProvider;

    public AbstractOpenSearchController(OpenSearchAccessProvider accessProvider) {
        this.accessProvider = accessProvider;
    }

    protected OpenSearchAccess getOpenSearchAccess() throws IOException {
        return accessProvider.getOpenSearchAccess();
    }
    
    protected void validateMin(Integer value, int min, String name) {
        if(value != null && value < min) {
            throw new RestException("Invalid parameter " + name + ", should be at least " + min, HttpStatus.BAD_REQUEST);
        }
    }
    
    protected void validateMax(Integer value, int max, String name) {
        if(value != null && value > max) {
            throw new RestException("Invalid parameter " + name + ", should be at most " + max, HttpStatus.BAD_REQUEST);
        }
    }


}