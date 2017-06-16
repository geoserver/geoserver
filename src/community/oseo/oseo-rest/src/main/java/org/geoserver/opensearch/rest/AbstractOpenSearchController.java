/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;

import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.rest.RestBaseController;

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

}