/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSException;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.vfny.geoserver.wcs.WcsException;

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
 * @author Andrea Aime - GeoSolutions
 * 
 * TODO: add methods to support process locking and all the deferred cleanup required for asynch processes 
 */
public class WPSResourceManager implements DispatcherCallback,
        ApplicationListener<ApplicationEvent> {
    private static final Logger LOGGER = Logging.getLogger(WPSResourceManager.class);

    ConcurrentHashMap<String, ExecutionResources> resourceCache = new ConcurrentHashMap<String, ExecutionResources>();

    ThreadLocal<String> executionId = new InheritableThreadLocal<String>();

    static final class ExecutionResources {
        /**
         * Temporary resources used to parse inputs or during the process execution
         */
        List<WPSResource> temporary;

        /**
         * Resources representing process outputs, should be kept around for some time for asynch
         * processes
         */
        List<WPSResource> outputs;

        /** Whether the execution is synchronous or asynch */
        boolean synchronouos;

        /** If true there is something accessing the output files and preventing their deletion */
        boolean outputLocked;

        /** Marks the process completion, we start counting down for output deletion */
        long completionTime;

        public ExecutionResources(boolean synchronouos) {
            this.synchronouos = synchronouos;
            this.temporary = new ArrayList<WPSResource>();
            this.outputs = new ArrayList<WPSResource>();
        }
    }

    /**
     * Create a new unique id for the process. All resources linked to the process should use this
     * token to register themselves against the manager
     * 
     * @return
     */
    public String getExecutionId(Boolean synch) {
        String id = executionId.get();
        if (id == null) {
            id = UUID.randomUUID().toString();
            executionId.set(id);
            resourceCache.put(id, new ExecutionResources(synch != null ? synch : true));
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Associating process with execution id: " + id);
            }
        }
        return id;
    }

    /**
     * ProcessManagers should call this method every time they are running the process in a thread
     * other than the request thread, and that is not a child of it either (typical case is running
     * in a thread pool)
     * 
     * @param executionId
     */
    public void setCurrentExecutionId(String executionId) {
        ExecutionResources resources = resourceCache.get(executionId);
        if (resources == null) {
            throw new IllegalStateException("Execution id " + executionId + " is not known");
        }
        this.executionId.set(executionId);
    }

    public void addResource(WPSResource resource) {
        String processId = getExecutionId(null);
        ExecutionResources resources = resourceCache.get(processId);
        if (resources == null) {
            throw new IllegalStateException("The executionId was not set for the current thread!");
        } else {
            resources.temporary.add(resource);
        }
    }
    
    /**
     * Returns a file that will be used to store a process output as a "reference" 
     * 
     * @param executionId
     * @param fileName
     * @return
     */
    public File getOutputFile(String executionId, String fileName) {
        File outputDirectory = new File(getWpsOutputStorage(), executionId);
        if(!outputDirectory.exists()) {
            mkdir(outputDirectory);
        }
        return new File(outputDirectory, fileName);
    }
    
    private void mkdir(File file) {
        if(!file.mkdir()) {
            throw new WPSException("Failed to create the specified directory " + file);
        }
    }
    
    /**
     * Gets the stored response file for the specified execution id
     * @param executionId
     * @return
     */
    public File getStoredResponseFile(String executionId) {
        File file = new File(getWpsOutputStorage(), executionId + ".xml");
        return file;
    }
    
    File getWpsOutputStorage() {
        File wpsStore = null;
        try {
            GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
            Resource wps = loader.get("temp/wps");
            wpsStore = wps.dir(); // find or create
        } catch(Exception e) {
            throw new ServiceException("Could not create the temporary storage directory for WPS");
        }
        if(wpsStore == null || !wpsStore.exists()) {
            throw new ServiceException("Could not create the temporary storage directory for WPS");
        }
        return wpsStore;
    }

    // -----------------------------------------------------------------
    // DispatcherCallback methods
    // -----------------------------------------------------------------

    public void finished(Request request) {
        // if we did not generate any process id, no resources have been added
        if (executionId.get() == null) {
            return;
        }

        // grab the id and unbind the thread local
        String id = executionId.get();
        executionId.remove();

        // cleanup automatically if the process is synchronous
        if (resourceCache.get(id).synchronouos) {
            cleanProcess(id);
            resourceCache.remove(id);
        }
    }
    
    public void finished(String executionId) {
        // cleanup the thread local, in case it has any id in it
        this.executionId.remove();

        // cleanup the temporary resources
        cleanProcess(executionId);
       
        // mark the process as complete
        resourceCache.get(executionId).completionTime = System.currentTimeMillis();
    }

    /**
     * Cleans up all the resources associated to a certain id. It is called automatically
     * when the request ends for synchronous processes, for asynch ones it will be triggered
     * by the process completion
     * 
     * @param id
     */
    void cleanProcess(String id) {
        // delete all resources associated with the process
        ExecutionResources executionResources = resourceCache.get(id);
        for (WPSResource resource : executionResources.temporary) {
            try {
                resource.delete();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING,
                        "Failed to clean up the WPS resource " + resource.getName(), t);
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
            for (String id : resourceCache.keySet()) {
                cleanProcess(id);
            }
            resourceCache.clear();
        }
    }
}
