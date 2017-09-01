/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.Map;

import org.geoserver.opensearch.eo.response.AtomSearchResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * Temporary trick to force GeoServer KVP parsing of description when there is no KVP param at all
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEODispatcherCallback extends AbstractDispatcherCallback {

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        final Map kvp = request.getKvp();
        if("oseo".equals(request.getService()) && kvp.isEmpty()) {
            if("description".equals(request.getRequest())) {
                kvp.put("service", "oseo");
                // the raw kvp is normally not even initialized
                request.setRawKvp(kvp);
            } else if("search".equals(request.getRequest())) {
                kvp.put("service", "oseo");
                kvp.put("httpAccept", AtomSearchResponse.MIME);
            }
            // make sure the raw kvp is not empty, ever (the current code
            // leaves it empty if the request has no search params)
            request.setRawKvp(kvp);
        }
        return service;
    }
}
