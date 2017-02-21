/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.List;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.ows.ServiceExceptionHandler;
import org.geoserver.platform.ServiceException;

/**
 * Returns exceptions as a RSS feed, as suggested in the OpenSearch EO developer guide at
 * http://www.opensearch.org/Documentation/Developer_how_to_guide
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class OSEOExceptionHandler extends ServiceExceptionHandler {

    private GeoServer geoServer;

    public OSEOExceptionHandler(List services, GeoServer geoServer) {
        super(services);
        this.geoServer = geoServer;
    }

    @Override
    public void handleServiceException(ServiceException exception, Request request) {
        throw new UnsupportedOperationException();
    }

}
