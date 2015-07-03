/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Callback that controls the flow of OWS requests based on user specified rules and makes sure
 * GeoServer does not get overwhelmed by too many concurrent ones. Can also be used to provide
 * different quality of service on different users.
 * 
 * @author Andrea Aime - OpenGeo
 */
public class ControlFlowCallback extends AbstractDispatcherCallback implements
        ApplicationContextAware {

    /**
     * Header added to all responses to make it visible how much deplay was applied going thorough
     * the flow controllers
     */
    static final String X_RATELIMIT_DELAY = "X-Control-flow-delay-ms";

    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    static final class CallbackContext {
        List<FlowController> controllers;

        long timeout;

        public CallbackContext(List<FlowController> controllers, long timeout) {
            this.controllers = controllers;
            this.timeout = timeout;
        }

    }

    static ThreadLocal<CallbackContext> REQUEST_CONTROLLERS = new ThreadLocal<CallbackContext>();

    static NestedRequestSentinel SENTINEL = new NestedRequestSentinel();

    FlowControllerProvider provider;

    AtomicLong blockedRequests = new AtomicLong();

    AtomicLong runningRequests = new AtomicLong();

    /**
     * Returns the current number of blocked/queued requests.
     */
    public long getBlockedRequests() {
        return blockedRequests.get();
    }

    /**
     * Returns the current number of running requests.
     */
    public long getRunningRequests() {
        return runningRequests.get();
    }

    public void finished(Request request) {
        if (SENTINEL.isOutermostRequest() && REQUEST_CONTROLLERS.get() != null) {
            runningRequests.decrementAndGet();
            // call back the same controllers we used when the operation started
            if (REQUEST_CONTROLLERS.get() != null) {
                CallbackContext context = REQUEST_CONTROLLERS.get();
                for (FlowController flowController : context.controllers) {
                    try {
                        flowController.requestComplete(request);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Flow controller " + flowController
                                + " failed to mark the request as complete", e);
                    }
                }

            }
            // clean up the thread local
            REQUEST_CONTROLLERS.remove();

            // provide some visibility that control flow is running
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Running requests: " + getRunningRequests() + ", blocked requests: "
                        + getBlockedRequests());
            }
        }
        SENTINEL.stop();
    }

    public Operation operationDispatched(Request request, Operation operation) {
        // tell the recursion sentinel we're starting a request
        SENTINEL.start();
        if (SENTINEL.isOutermostRequest()) {
            blockedRequests.incrementAndGet();
            long start = System.currentTimeMillis();
            try {
                // the operation has not been set in the Request yet by the dispatcher, do so now in
                // a clone of the Request
                Request requestWithOperation = null;
                if(request != null) {
                    requestWithOperation = new Request(request);
                    requestWithOperation.setOperation(operation);
                }
                
                // grab the controllers for this request
                List<FlowController> controllers = null;
                try {
                    controllers = provider.getFlowControllers(requestWithOperation);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE,
                            "An error occurred setting up the flow controllers to this request", e);
                    return operation;
                }
                if (controllers.size() == 0) {
                    LOGGER.info("Control-flow inactive for , there are no configured rules");
                } else {
                    long timeout = provider.getTimeout(requestWithOperation);
                    CallbackContext context = new CallbackContext(controllers, timeout);
                    REQUEST_CONTROLLERS.set(context);
                    long maxTime = timeout > 0 ? System.currentTimeMillis() + timeout : -1;
                    for (FlowController flowController : controllers) {
                        if (timeout > 0) {
                            long maxWait = maxTime - System.currentTimeMillis();
                            if (!flowController.requestIncoming(requestWithOperation, maxWait)) {
                                throw new HttpErrorCodeException(503,
                                        "Requested timeout out while waiting to be executed, please lower your request rate");
                            }
                        } else {
                            flowController.requestIncoming(requestWithOperation, -1);
                        }
                    }
                }
            } finally {
                blockedRequests.decrementAndGet();
                runningRequests.incrementAndGet();
                if (request != null && request.getHttpResponse() != null) {
                    // report how much time was spent going though the flow controllers
                    long end = System.currentTimeMillis();
                    request.getHttpResponse().addHeader(X_RATELIMIT_DELAY,
                            String.valueOf(end - start));
                }
            }
        }
        return operation;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // look for a ControlFlowConfigurator in the application context, if none is found, use the
        // default one
        provider = GeoServerExtensions.bean(FlowControllerProvider.class, applicationContext);
        if (provider == null) {
            provider = new DefaultFlowControllerProvider(applicationContext);
        }
    }

}
