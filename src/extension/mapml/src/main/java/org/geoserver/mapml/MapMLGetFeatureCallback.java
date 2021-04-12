/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;

/**
 * Injected into the OWS request life cycle to obtain context (path, query) for the MapML WFS (only)
 * response format.
 *
 * @author prushforth
 */
public class MapMLGetFeatureCallback extends AbstractDispatcherCallback {

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        Service service = operation.getService();
        if (service == null
                || service.getId() == null
                || !service.getId().equalsIgnoreCase("wfs")) {
            // not a WFS service so we are not interested in it
            return response;
        }
        String responseMimeType = response.getMimeType(result, operation);
        if (!responseMimeType.startsWith(MapMLConstants.MAPML_MIME_TYPE)) {
            return response;
        }

        MapMLGetFeatureOutputFormat mapmlResponse = ((MapMLGetFeatureOutputFormat) response);
        mapmlResponse.setBase(ResponseUtils.baseURL(request.getHttpRequest()));
        mapmlResponse.setPath(request.getPath());
        mapmlResponse.setQuery(request.getRawKvp());
        return response;
    }
}
