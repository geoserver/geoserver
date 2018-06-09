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
    GET_HISTOGRAM,
    GET_FEATURE;

    static SimpleConveyor match(
            String operationName,
            HttpServletRequest request,
            HttpServletResponse response,
            StorageBroker storageBroker,
            KvpMap parameters) {
        switch (operationName.toUpperCase()) {
            case "DESCRIBEDOMAINS":
                return new SimpleConveyor(
                        Operation.DESCRIBE_DOMAINS, request, response, storageBroker, parameters);
            case "GETHISTOGRAM":
                return new SimpleConveyor(
                        Operation.GET_HISTOGRAM, request, response, storageBroker, parameters);
            case "GETFEATURE":
                return new SimpleConveyor(
                        Operation.GET_FEATURE, request, response, storageBroker, parameters);
            default:
                return null;
        }
    }
}
