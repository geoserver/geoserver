/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

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
        if("oseo".equals(request.getService()) && "description".equals(request.getRequest()) && request.getKvp().isEmpty()) {
            request.getKvp().put("service", "oseo");
            // the raw kvp is normally not even initialized
            request.setRawKvp(request.getKvp());
        }
        return service;
    }
}
