/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * An empty callback implementation, can be used as a convenient base class when there is a need to
 * implement only a few callback methods
 *
 * @author Andrea Aime - OpenGeo
 */
public class AbstractDispatcherCallback implements DispatcherCallback {

    public void finished(Request request) {
        // nothing to do here
    }

    public Request init(Request request) {
        return request;
    }

    public Operation operationDispatched(Request request, Operation operation) {
        return operation;
    }

    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        return response;
    }

    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }
}
