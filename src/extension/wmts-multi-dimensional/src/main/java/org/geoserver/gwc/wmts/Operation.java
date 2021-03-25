/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.util.KvpMap;
import org.geowebcache.storage.StorageBroker;

/** WMTS multidimensional specific operations. */
enum Operation {
    DESCRIBE_DOMAINS,
    GET_DOMAIN_VALUES,
    GET_HISTOGRAM,
    GET_FEATURE;

    /**
     * If the requested operation matches a supported operation we return a conveyor for it
     * otherwise we return NULL.
     *
     * @param operationName name of the operation to match, maybe NULL
     * @param request received HTTP request
     * @param response HTTP response that will be send to the client
     * @param storageBroker GWC storage broker, used to instantiate the conveyor
     * @param parameters normalized KVP parameters of the received HTTP request
     * @return NULL if the requested operation is not supported, a conveyor otherwise
     */
    static SimpleConveyor match(
            String operationName,
            HttpServletRequest request,
            HttpServletResponse response,
            StorageBroker storageBroker,
            KvpMap parameters) {
        if (operationName == null || operationName.isEmpty()) {
            // no operation requested, we let invoker handle this a throw the correspondent
            // exception
            return null;
        }
        switch (operationName.toUpperCase()) {
            case "DESCRIBEDOMAINS":
                return new SimpleConveyor(
                        Operation.DESCRIBE_DOMAINS, request, response, storageBroker, parameters);
            case "GETDOMAINVALUES":
                return new SimpleConveyor(
                        Operation.GET_DOMAIN_VALUES, request, response, storageBroker, parameters);
            case "GETHISTOGRAM":
                return new SimpleConveyor(
                        Operation.GET_HISTOGRAM, request, response, storageBroker, parameters);
            case "GETFEATURE":
                return new SimpleConveyor(
                        Operation.GET_FEATURE, request, response, storageBroker, parameters);
            default:
                // operation not supported
                return null;
        }
    }
}
