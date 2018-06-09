/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.junit.Assert.assertEquals;

import java.util.Stack;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * Makes sure that when nested requests occur, the events are balanced and use the right request,
 * one for the nested callbacks, and a separate one for the outer callback
 */
final class BalancedRequestTester implements DispatcherCallback {

    Stack<Request> requestStack = new Stack<>();

    @Override
    public Request init(Request request) {
        requestStack.push(request);
        return request;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        assertEquals(request, requestStack.peek());
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        assertEquals(request, requestStack.peek());
        return operation;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        assertEquals(request, requestStack.peek());
        return result;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        assertEquals(request, requestStack.peek());
        return response;
    }

    @Override
    public void finished(Request request) {
        assertEquals(request, requestStack.pop());
    }
}
