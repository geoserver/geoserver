/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.HashSet;
import java.util.Map;
import org.geoserver.opensearch.eo.response.AtomSearchResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.springframework.util.StringUtils;

/**
 * Temporary trick to force GeoServer KVP parsing of description when there is no KVP param at all
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEODispatcherCallback extends AbstractDispatcherCallback {

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        final Map kvp = request.getKvp();
        final Map rawKvp = request.getRawKvp();
        if ("oseo".equalsIgnoreCase(request.getService())) {
            if (kvp.isEmpty()) {
                if ("description".equals(request.getRequest())) {
                    kvp.put("service", "oseo");
                    // the raw kvp is normally not even initialized
                    request.setRawKvp(kvp);
                } else if ("search".equals(request.getRequest())) {
                    kvp.put("service", "oseo");
                    kvp.put("httpAccept", AtomSearchResponse.MIME);
                }
                // make sure the raw kvp is not empty, ever (the current code
                // leaves it empty if the request has no search params)
                request.setRawKvp(kvp);
            } else {
                // skip everything that has an empty value, in OpenSearch it should be ignored
                // (clients following the template to the letter will create keys with empty value)
                for (String key : new HashSet<String>(request.getRawKvp().keySet())) {
                    Object value = rawKvp.get(key);
                    if ((!(value instanceof String) || StringUtils.isEmpty((String) value))
                            && !(value instanceof String[])) {
                        rawKvp.remove(key);
                        kvp.remove(key);
                    }
                }
            }
        }
        return service;
    }
}
