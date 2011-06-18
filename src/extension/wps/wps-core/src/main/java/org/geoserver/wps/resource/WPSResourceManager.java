/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;

/**
 * A WPS process has to deal with various temporary resources during the execution, be streamed and
 * stored inputs, Sextante temporary files, temporary feature types and so on.
 * 
 * This class manages the lifecycle of these resources, register them here to have their lifecycle
 * properly managed
 * 
 * The design is still very rough, I'm making this up as I go. The class will require modifications
 * to handle asynch process computations as well as resources with a timeout
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class WPSResourceManager implements DispatcherCallback, ApplicationListener {
    private static final Logger LOGGER = Logging.getLogger(WPSResourceManager.class);

    ConcurrentHashMap<UUID, List<WPSResource>> resourceCache = new ConcurrentHashMap<UUID, List<WPSResource>>();

    ThreadLocal<UUID> processId = new ThreadLocal<UUID>();

    /**
     * Create a new unique id for the process. All resources linked to the process should use this
     * token to register themselves against the manager
     * 
     * @return
     */
    public UUID getProcessId() {
        if (processId.get() == null) {
            processId.set(UUID.randomUUID());
        }
        return processId.get();
    }

    public void addResource(WPSResource resource) {
        UUID processId = getProcessId();
        List<WPSResource> processResources = resourceCache.get(processId);
        if (processResources == null) {
            processResources = new ArrayList<WPSResource>();
            resourceCache.put(processId, processResources);
        }
        processResources.add(resource);
    }

    // -----------------------------------------------------------------
    // DispatcherCallback methods
    // -----------------------------------------------------------------

    public void finished(Request request) {
        // if we did not generate any process id, no resources have been added
        if (processId.get() == null) {
            return;
        }
     
        // grab the id and unbind the thread local
        UUID id = processId.get();
        processId.remove();

        // cleaup
        cleanProcess(id);
        resourceCache.remove(id);
    }

    /**
     * Cleans up all the resources associated to a certain id
     * @param id
     */
    void cleanProcess(UUID id) {
        // delete all resources associated with the process 
        for (WPSResource resource : resourceCache.get(id)) {
            try {
                resource.delete();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Failed to clean up the WPS resource "
                        + resource.getName(), t);
            }
        }
    }

    public Request init(Request request) {
        return null;
    }

    public Operation operationDispatched(Request request, Operation operation) {
        return null;
    }

    public Object operationExecuted(Request request, Operation operation, Object result) {
        return null;
    }

    public Response responseDispatched(Request request, Operation operation, Object result,
            Response response) {
        return null;
    }

    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return null;
    }

    // -----------------------------------------------------------------
    // ApplicationListener methods
    // -----------------------------------------------------------------

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextClosedEvent || event instanceof ContextStoppedEvent) {
            // we are shutting down, remove all temp resources!
            for (UUID id : resourceCache.keySet()) {
                cleanProcess(id);
            }
        }
        
        resourceCache.clear();
    }
}
