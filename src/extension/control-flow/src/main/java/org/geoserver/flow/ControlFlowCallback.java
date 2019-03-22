/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.flow.config.DefaultControlFlowConfigurator;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Callback that controls the flow of OWS requests based on user specified rules and makes sure
 * GeoServer does not get overwhelmed by too many concurrent ones. Can also be used to provide
 * different quality of service on different users.
 *
 * @author Andrea Aime - OpenGeo
 */
public class ControlFlowCallback extends AbstractDispatcherCallback
        implements ApplicationContextAware, GeoServerFilter {

    /**
     * Header added to all responses to make it visible how much deplay was applied going thorough
     * the flow controllers
     */
    static final String X_RATELIMIT_DELAY = "X-Control-flow-delay-ms";

    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    /**
     * Container for the original Request object, the controllers and the timeout (to make sure we
     * are playing with the same objects, regardless of what other machinery might do to mock up
     * with the Dispatcher.REQUEST thread local).
     */
    static final class CallbackContext {
        List<FlowController> controllers;

        long timeout;

        Request request;

        int nestingLevel = 1;

        public CallbackContext(Request request, List<FlowController> controllers, long timeout) {
            this.controllers = controllers;
            this.timeout = timeout;
            this.request = request;
        }
    }

    static ThreadLocal<CallbackContext> REQUEST_CONTROLLERS = new ThreadLocal<>();

    static ThreadLocal<Boolean> FAILED_ON_FLOW_CONTROLLERS = new ThreadLocal<>();

    FlowControllerProvider provider;

    AtomicLong blockedRequests = new AtomicLong();

    AtomicLong runningRequests = new AtomicLong();

    public ControlFlowCallback() {
        // this is just to isolate tests from shared state, at runtime there is only one callback.
        REQUEST_CONTROLLERS.remove();
    }

    /** Returns the current number of blocked/queued requests. */
    public long getBlockedRequests() {
        return blockedRequests.get();
    }

    /** Returns the current number of running requests. */
    public long getRunningRequests() {
        return runningRequests.get();
    }

    public Operation operationDispatched(Request request, Operation operation) {
        // if this request is nested, release the previous controllers and grab new ones
        // Nesting happens only with integrated GWC, sometimes the nested request is similar to the
        // outside one, e.g., with transparent integration, other times it's completely different,
        // e.g. native
        // tile services). We cannot afford to have the same controller lock twice, that would cause
        // deadlocks
        if (REQUEST_CONTROLLERS.get() != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Nested request found, not locking on it");
            }
            REQUEST_CONTROLLERS.get().nestingLevel++;
            return operation;
        }

        blockedRequests.incrementAndGet();
        long start = System.currentTimeMillis();
        boolean failedOnFlowControllers = true;
        try {
            // the operation has not been set in the Request yet by the dispatcher, do so now in
            // a clone of the Request
            Request requestWithOperation = null;
            if (request != null) {
                requestWithOperation = new Request(request);
                requestWithOperation.setOperation(operation);
            }

            // grab the controllers for this request
            List<FlowController> controllers = null;
            try {
                controllers = provider.getFlowControllers(requestWithOperation);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "An error occurred setting up the flow controllers to this request",
                        e);
                return operation;
            }
            if (controllers.size() == 0) {
                LOGGER.info("Control-flow inactive, there are no configured rules");
            } else {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(
                            "Request ["
                                    + requestWithOperation
                                    + "] starting, processing through flow controllers");
                }

                long timeout = provider.getTimeout(requestWithOperation);
                CallbackContext context =
                        new CallbackContext(requestWithOperation, controllers, timeout);
                REQUEST_CONTROLLERS.set(context);
                long maxTime = timeout > 0 ? System.currentTimeMillis() + timeout : -1;
                for (FlowController flowController : controllers) {
                    if (timeout > 0) {
                        long maxWait = maxTime - System.currentTimeMillis();
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine(
                                    "Request ["
                                            + requestWithOperation
                                            + "] checking flow controller "
                                            + flowController);
                        }
                        if (!flowController.requestIncoming(requestWithOperation, maxWait)) {
                            throw new HttpErrorCodeException(
                                    503,
                                    "Requested timeout out while waiting to be executed, please lower your request rate");
                        }
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine(
                                    "Request ["
                                            + requestWithOperation
                                            + "] passed flow controller "
                                            + flowController);
                        }
                    } else {
                        flowController.requestIncoming(requestWithOperation, -1);
                    }
                }
            }
            failedOnFlowControllers = false;
        } finally {
            blockedRequests.decrementAndGet();
            if (!failedOnFlowControllers) {
                runningRequests.incrementAndGet();
            }
            FAILED_ON_FLOW_CONTROLLERS.set(failedOnFlowControllers);

            if (REQUEST_CONTROLLERS.get() != null) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(
                            "Request control-flow performed, running requests: "
                                    + getRunningRequests()
                                    + ", blocked requests: "
                                    + getBlockedRequests());
                }
            }
            if (request != null && request.getHttpResponse() != null) {
                // report how much time was spent going though the flow controllers
                long end = System.currentTimeMillis();
                request.getHttpResponse().addHeader(X_RATELIMIT_DELAY, String.valueOf(end - start));
            }
        }
        return operation;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            // register default beans if needed
            registDefaultBeansIfNeeded((ConfigurableApplicationContext) applicationContext);
        } else {
            // we cannot regist default beans, there is nothing else we can do about this
            LOGGER.warning(
                    "Application context not configurable, control-flow default beans will not be registered.");
        }
        provider = GeoServerExtensions.bean(FlowControllerProvider.class, applicationContext);
        // default beans may have not been registered
        if (provider == null) {
            provider = new DefaultFlowControllerProvider(applicationContext);
        }
    }

    /** Register default beans for control flow configurator and flow controller. */
    private void registDefaultBeansIfNeeded(ConfigurableApplicationContext applicationContext) {
        ConfigurableListableBeanFactory factory = applicationContext.getBeanFactory();
        // make sure default beans are only registered once
        synchronized (ControlFlowCallback.class) {
            // first handle the configurator bean
            try {
                applicationContext.getBean(ControlFlowConfigurator.class, applicationContext);
            } catch (NoSuchBeanDefinitionException exception) {
                // we need to use the default configurator
                factory.registerSingleton(
                        "defaultControlFlowConfigurator", new DefaultControlFlowConfigurator());
                LOGGER.fine("Default flow configurator bean dynamically registered.");
            }
            // handle the flow controller provider bean
            try {
                applicationContext.getBean(FlowControllerProvider.class, applicationContext);
            } catch (NoSuchBeanDefinitionException exception) {
                // we need to use the default flow controller provider
                factory.registerSingleton(
                        "defaultFlowControllerProvider",
                        new DefaultFlowControllerProvider(applicationContext));
                LOGGER.fine("Default flow controller provider bean dynamically registered.");
            }
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do

    }

    public void finished(Request request) {
        releaseControllers(false);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // execute normally
            chain.doFilter(request, response);
        } finally {
            // this is a precaution in case finished() is not called by any reason
            releaseControllers(true);
        }
    }

    private void releaseControllers(boolean forceRelease) {
        CallbackContext context = REQUEST_CONTROLLERS.get();
        try {
            // will be called twice in normal requests, make sure we check if there
            // are actually controllers around
            if (context != null) {
                context.nestingLevel--;
                if (context.nestingLevel <= 0 || forceRelease) {
                    if (Boolean.FALSE.equals(FAILED_ON_FLOW_CONTROLLERS.get())) {
                        runningRequests.decrementAndGet();
                    }
                    // call back the same controllers we used when the operation started, releasing
                    // them in inverse order
                    LOGGER.info("releasing flow controllers for [" + context.request + "]");
                    final List<FlowController> controllers = context.controllers;
                    for (int i = controllers.size() - 1; i >= 0; i--) {
                        FlowController flowController = controllers.get(i);
                        try {
                            flowController.requestComplete(context.request);
                        } catch (Throwable t) {
                            // catching throwable here is intended, we cannot afford not to
                            // release controllers, it would eventually lead to deadlock
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Flow controller "
                                            + flowController
                                            + " failed to mark the request as complete",
                                    t);
                        }
                    }
                    // provide some visibility that control flow is running
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info(
                                "Request completed, running requests: "
                                        + getRunningRequests()
                                        + ", blocked requests: "
                                        + getBlockedRequests());
                    }
                }
            }
        } finally {
            // clean up the thread local, all controllers have been released
            if (context != null && (context.nestingLevel <= 0 || forceRelease)) {
                REQUEST_CONTROLLERS.remove();
            }
        }
    }

    @Override
    public void destroy() {
        // nothing to do
    }
}
