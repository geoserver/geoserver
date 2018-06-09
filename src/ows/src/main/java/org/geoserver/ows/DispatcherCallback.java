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
 * Provides call backs for the life cycle of an ows request.
 *
 * <p>Instances of this interface should be registered in a spring application context like:
 *
 * <pre>
 *  &lt;bean id="myCallback" class="org.acme.MyCallback"/&gt;
 * </pre>
 *
 * @author Justin Deoliveira, OpenGEO
 */
public interface DispatcherCallback {

    /**
     * Called immediately after a request has been received and initialized by the dispatcher.
     *
     * <p>This method can modify the request object, or wrap and return it. If null is returned the
     * request passed in is used normally.
     *
     * @param request The request being executed.
     */
    Request init(Request request);

    /**
     * Called after the service for the request has been determined.
     *
     * <p>This method can modify the service object, or wrap and return it. If null is returned the
     * service passed in is used normally.
     *
     * @param request The request.
     * @param service The service descriptor for the service handling the request.
     */
    Service serviceDispatched(Request request, Service service) throws ServiceException;

    /**
     * Called after the operation for the request has been determined.
     *
     * <p>This method can modify the operation object, or wrap and return it. If null is returned
     * the operation passed in is used normally.
     *
     * @param request The request.
     * @param operation The operation for the request.
     */
    Operation operationDispatched(Request request, Operation operation);

    /**
     * Called after the operation for a request has been executed.
     *
     * <p><b>Note:</b>This method should handle the case where <tt>result</tt> is null as this
     * corresponds to an operation which does not return a value.
     *
     * <p>This method can modify the result object, or wrap and return it. If null is returned the
     * result passed in is used normally.
     *
     * @param request The request.
     * @param operation The operation.
     * @param result The result of the operation, may be <code>null</code>.
     */
    Object operationExecuted(Request request, Operation operation, Object result);

    /**
     * Called after the response to a request has been dispatched.
     *
     * <p><b>Note:</b> This method is only called when the operation returns a value.
     *
     * <p>This method can modify the response object, or wrap and return it. If null is returned the
     * response passed in is used normally.
     *
     * @param request The request.
     * @param operation The operation.
     * @param result The result of the operation.
     * @param response The response to the operation.
     */
    Response responseDispatched(
            Request request, Operation operation, Object result, Response response);

    /**
     * Called after the response to the operation has been executed.
     *
     * <p>This method is called regardless if the operation was successful or not. In the event of a
     * request that resulted in an error, the error is available at {@link Request#error}.
     *
     * @param request The request.
     */
    void finished(Request request);
}
