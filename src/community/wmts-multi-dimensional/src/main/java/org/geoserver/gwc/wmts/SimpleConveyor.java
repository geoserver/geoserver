/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.platform.ServiceException;
import org.geowebcache.conveyor.Conveyor;
import org.geowebcache.storage.StorageBroker;

final class SimpleConveyor extends Conveyor {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Operation operation;
    private final KvpMap parameters;

    SimpleConveyor(
            Operation operation,
            HttpServletRequest request,
            HttpServletResponse response,
            StorageBroker storageBroker,
            KvpMap parameters) {
        super((String) parameters.get("layer"), storageBroker, request, response);
        this.request = request;
        this.response = response;
        this.operation = operation;
        this.parameters = parameters;
        super.setRequestHandler(Conveyor.RequestHandler.SERVICE);
    }

    HttpServletResponse getResponse() {
        return response;
    }

    Operation getOperation() {
        return operation;
    }

    Object getParameter(String parameterName, boolean mandatory) {
        Object value = parameters.get(parameterName.toUpperCase());
        if (value == null && mandatory) {
            throw new ServiceException(
                    String.format("Mandatory '%s' parameter is missing.", parameterName));
        }
        return value;
    }
}
