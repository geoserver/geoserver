/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.flow.config.DefaultControlFlowConfigurator;
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
    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    static ThreadLocal<List<FlowController>> REQUEST_CONTROLLERS = new ThreadLocal<List<FlowController>>();
    
    static NestedRequestSentinel SENTINEL = new NestedRequestSentinel();

    List<FlowController> controllers = Collections.emptyList();
    long timeout = -1;

    ControlFlowConfigurator configurator;
    
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
        if(SENTINEL.isOutermostRequest() && REQUEST_CONTROLLERS.get() != null) {
            runningRequests.decrementAndGet();
            // call back the same controllers we used when the operation started
            if (REQUEST_CONTROLLERS.get() != null) {
                List<FlowController> fcl = REQUEST_CONTROLLERS.get();
                for (FlowController flowController : fcl) {
                    try {
                        flowController.requestComplete(request);
                    } catch(Exception e) {
                        LOGGER.log(Level.SEVERE, "Flow controller " + fcl + " failed to mark the request as complete", e);
                    }
                }
            }
            // clean up the thread local
            REQUEST_CONTROLLERS.remove();
            
            // provide some info
            if(LOGGER.isLoggable(Level.INFO)) {
                if(controllers.size() > 0) {
                    LOGGER.info("Running requests: " + runningRequests.get() 
                            + ", processing through flow controllers: " + blockedRequests.get());
                } else {
                    LOGGER.info("Control flow installed, but no rules configured in controlflow.properties");
                }
            }
        } 
        SENTINEL.stop();
    }

    public Operation operationDispatched(Request request, Operation operation) {
        checkConfiguration();
        
        // tell the recursion sentinel we're starting a request
        SENTINEL.start();
        if(SENTINEL.isOutermostRequest()) {
            blockedRequests.incrementAndGet();
            try {
                // scan through the existing controllers and set the list in a thread local
                // so that this request will get exactly the same list when the operation finishes
                List<FlowController> controllers = this.controllers;
                if (controllers.size() > 0) {
                    REQUEST_CONTROLLERS.set(controllers);
                    long maxTime = timeout > 0 ? System.currentTimeMillis() + timeout : -1;
                    for (FlowController flowController : controllers) {
                        if(timeout > 0) {
                            long maxWait = maxTime - System.currentTimeMillis();
                            if(!flowController.requestIncoming(request, maxWait)) 
                                throw new HttpErrorCodeException(503, "Requested timeout out while waiting to be executed");
                         } else {
                            flowController.requestIncoming(request, -1);
                        }
                    }
                }
            } finally {
                blockedRequests.decrementAndGet();
                runningRequests.incrementAndGet();
            }
        }
        return operation;
    }

    private void checkConfiguration() {
        // check if we need to rebuild the flow controller list
        if (configurator.isStale()){
            // be careful, as the configuration can be read on demand, it'd not be uncommon that
            // multiple requests come at once when the config file changed
            synchronized (configurator) {
                if (configurator.isStale()){
                    reloadConfiguration();
                }
            }
        }
    }

    /**
     * Reloads the flow controller list and replaces the existing ones
     */
    void reloadConfiguration() {
        try {
            List<FlowController> newControllers = new ArrayList<FlowController>(configurator
                    .buildFlowControllers());
            Collections.sort(newControllers, new ControllerPriorityComparator());
            controllers = newControllers;
            int controllersCount = controllers.size();
            if(controllersCount > 0) {
                LOGGER.info("Control-flow active with " + controllersCount + " flow controllers");
            } else {
                LOGGER.info("Control-flow inactive, there are no configured rules");
            }
            timeout = configurator.getTimeout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurerd during flow controllers reconfiguration");
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // look for a ControlFlowConfigurator in the application context, if none is found, use the
        // default one
        configurator = GeoServerExtensions.bean(ControlFlowConfigurator.class, applicationContext);
        if (configurator == null) {
            configurator = new DefaultControlFlowConfigurator();
        }
        checkConfiguration();
        if(controllers.size() == 0) {
            LOGGER.info("Control-flow inactive, there are no configured rules");
        }
    }

}
