/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

public class TestDispatcherCallback implements DispatcherCallback {
    public enum Status {
        INIT,
        SERVICE_DISPATCHED,
        OPERATION_DISPATCHED,
        OPERATION_EXECUTED,
        RESPONSE_DISPATCHED,
        FINISHED
    }

    public ThreadLocal<Status> dispatcherStatus = new ThreadLocal<Status>();

    @Override
    public Request init(Request request) {
        dispatcherStatus.set(Status.INIT);
        return request;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        dispatcherStatus.set(Status.SERVICE_DISPATCHED);
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        dispatcherStatus.set(Status.OPERATION_DISPATCHED);
        return operation;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        dispatcherStatus.set(Status.OPERATION_EXECUTED);
        return result;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        dispatcherStatus.set(Status.RESPONSE_DISPATCHED);
        return response;
    }

    @Override
    public void finished(Request request) {
        dispatcherStatus.set(Status.FINISHED);
    }
}
