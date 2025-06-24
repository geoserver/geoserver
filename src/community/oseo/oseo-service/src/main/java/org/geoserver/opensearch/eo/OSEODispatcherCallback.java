/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.geoserver.opensearch.eo.response.AtomSearchResponse;
import org.geoserver.opensearch.eo.response.GeoJSONSearchResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.springframework.util.StringUtils;

/**
 * Temporary trick to force GeoServer KVP parsing of description when there is no KVP param at all
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEODispatcherCallback extends AbstractDispatcherCallback {

    private static final String PARENT_ID = "parentId";
    private static final String PARENT_IDENTIFIER = "parentIdentifier";

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
                    if (!kvp.containsKey("httpAccept")) kvp.put("httpAccept", AtomSearchResponse.MIME);
                }
                // make sure the raw kvp is not empty, ever (the current code
                // leaves it empty if the request has no search params)
                request.setRawKvp(kvp);
            } else {
                // skip everything that has an empty value, in OpenSearch it should be ignored
                // (clients following the template to the letter will create keys with empty value)
                cleanupRequestParams(request, rawKvp, kvp);
            }

            // backwards compatibility, parentId got renamed to parentIdentifier
            if (rawKvp != null && rawKvp.containsKey(PARENT_ID) && !rawKvp.containsKey(PARENT_IDENTIFIER)) {
                rawKvp.put(PARENT_IDENTIFIER, rawKvp.get(PARENT_ID));
            }
            if (kvp != null && kvp.containsKey(PARENT_ID) && !kvp.containsKey(PARENT_IDENTIFIER)) {
                kvp.put(PARENT_IDENTIFIER, kvp.get(PARENT_ID));
            }
        }
        return service;
    }

    private void cleanupRequestParams(Request request, Map rawKvp, Map kvp) {
        for (String key : new HashSet<>(request.getRawKvp().keySet())) {
            Object value = rawKvp.get(key);
            // Some clients are sending the same search parameter twice
            // once with a value and once as an empty value.
            // Let's handle it gracefully with some cleanup
            if (value instanceof String[] values) {
                List<String> cleaned = new ArrayList<>();

                for (String v : values) {
                    if (StringUtils.hasText(v)) {
                        cleaned.add(v);
                    }
                }

                if (cleaned.isEmpty()) {
                    rawKvp.remove(key);
                    kvp.remove(key);
                } else if (cleaned.size() == 1) {
                    // If only one value remains, simplify to a single String
                    rawKvp.put(key, cleaned.get(0));
                    kvp.put(key, cleaned.get(0));
                } else {
                    rawKvp.put(key, cleaned.toArray(new String[0]));
                    kvp.put(key, cleaned.toArray(new String[0]));
                }
            } else if (!(value instanceof String) || !StringUtils.hasText((String) value)) {
                rawKvp.remove(key);
                kvp.remove(key);
            }
        }
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        // set the output format from httpAccept, to make multiple output formats for a single
        // response work in the OGC dispatcher
        String format = (String) request.getKvp().get("httpAccept");
        boolean searchRequest = "search".equalsIgnoreCase(request.getRequest());
        if (format != null) {
            // leniency for shortcut names
            if ("atom".equals(format) && searchRequest) {
                request.setOutputFormat(AtomSearchResponse.MIME);
            } else if ("json".equals(format) && searchRequest) {
                request.setOutputFormat(GeoJSONSearchResponse.MIME);
            } else {
                request.setOutputFormat(format);
            }
        } else if (searchRequest) {
            // default to atom for backwards compatibility
            request.setOutputFormat(AtomSearchResponse.MIME);
        }

        return operation;
    }
}
