/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.wps10.ExecuteType;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.ProcessListenerAdapter;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.resource.ProcessArtifactsStore.ArtifactType;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.util.logging.Logging;
import org.geotools.wps.WPS;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.xml.sax.SAXException;

/**
 * A WPS process has to deal with various temporary resources during the execution, be streamed and
 * stored inputs, Sextante temporary files, temporary feature types and so on.
 *
 * <p>This class manages the lifecycle of these resources, register them here to have their
 * lifecycle properly managed
 *
 * <p>The design is still very rough, I'm making this up as I go. The class will require
 * modifications to handle asynch process computations as well as resources with a timeout
 *
 * @author Andrea Aime - GeoSolutions
 *     <p>TODO: we need to have the process statuses to avoid deleting stuff that is being worked on
 *     by another machine
 */
public class WPSResourceManager extends ProcessListenerAdapter
        implements DispatcherCallback,
                ApplicationListener<ApplicationEvent>,
                ApplicationContextAware {
    private static final Logger LOGGER = Logging.getLogger(WPSResourceManager.class);

    ConcurrentHashMap<String, ExecutionResources> resourceCache =
            new ConcurrentHashMap<String, ExecutionResources>();

    ThreadLocal<String> executionId = new InheritableThreadLocal<String>();

    private ProcessArtifactsStore artifactsStore;

    static final class ExecutionResources {
        /** Temporary resources used to parse inputs or during the process execution */
        List<WPSResource> temporary;

        /** Whether the execution is synchronous or asynch */
        boolean synchronouos;

        /** If true there is something accessing the output files and preventing their deletion */
        boolean outputLocked;

        /** Marks the process completion, we start counting down for output deletion */
        long completionTime;

        public ExecutionResources(boolean synchronouos) {
            this.synchronouos = synchronouos;
            this.temporary = new ArrayList<WPSResource>();
        }
    }

    private String getExecutionId(String executionId) {
        if (executionId == null) {
            executionId = getExecutionId((Boolean) null);
        }

        return executionId;
    }

    /**
     * Create a new unique id for the process. All resources linked to the process should use this
     * token to register themselves against the manager
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
     */
    void setCurrentExecutionId(String executionId) {
        ExecutionResources resources = resourceCache.get(executionId);
        if (resources == null) {
            throw new IllegalStateException("Execution id " + executionId + " is not known");
        }
        this.executionId.set(executionId);
    }

    /** Returns the executionId bound to this thread, if any */
    String getCurrentExecutionId() {
        return this.executionId.get();
    }

    /** Clears the current execution id thread local */
    void clearExecutionId() {
        this.executionId.set(null);
    }

    public void addResource(WPSResource resource) {
        String processId = getExecutionId((Boolean) null);
        ExecutionResources resources = resourceCache.get(processId);
        if (resources == null) {
            throw new IllegalStateException("The executionId was not set for the current thread!");
        } else {
            resources.temporary.add(resource);
        }
    }

    /**
     * Returns a resource that will be used to store a process output as a "reference"
     *
     * @param executionId - can be null
     */
    public Resource getOutputResource(String executionId, String fileName) {
        executionId = getExecutionId(executionId);
        Resource resource = artifactsStore.getArtifact(executionId, ArtifactType.Output, fileName);
        // no need to track this one, it will be cleaned up when
        return resource;
    }

    /**
     * Returns the url to fetch a output resource using the GetExecutionResult call
     *
     * @param name The file name
     * @param mimeType the
     */
    public String getOutputResourceUrl(String name, String mimeType) {
        return getOutputResourceUrl(null, name, null, mimeType);
    }

    /**
     * Returns the url to fetch a output resource using the GetExecutionResult call
     *
     * @param executionId - optional, if you don't have it the resource manager will use its thread
     *     local version
     * @param baseUrl - optional, if you don't have it the resource manager will pick one from
     *     Dispatcher.REQUEST
     */
    public String getOutputResourceUrl(
            String executionId, String name, String baseUrl, String mimeType) {
        // create the link
        Map<String, String> kvp = new LinkedHashMap<String, String>();
        kvp.put("service", "WPS");
        kvp.put("version", "1.0.0");
        kvp.put("request", "GetExecutionResult");
        kvp.put("executionId", getExecutionId(executionId));
        kvp.put("outputId", name);
        kvp.put("mimetype", mimeType);
        if (baseUrl == null) {
            Operation op = Dispatcher.REQUEST.get().getOperation();
            ExecuteType execute = (ExecuteType) op.getParameters()[0];
            baseUrl = execute.getBaseUrl();
        }
        String url = ResponseUtils.buildURL(baseUrl, "ows", kvp, URLType.SERVICE);

        return url;
    }

    /**
     * Returns a resource that will be used to store some temporary file for processing sake, and
     * will mark it for deletion when the process ends
     */
    public Resource getTemporaryResource(String extension) throws IOException {

        String executionId = getExecutionId((Boolean) null);
        Resource resource =
                artifactsStore.getArtifact(
                        executionId,
                        ArtifactType.Temporary,
                        UUID.randomUUID().toString() + extension);
        addResource(new WPSResourceResource(resource));
        return resource;
    }

    /** Gets the stored response file for the specified execution id */
    public Resource getStoredResponse(String executionId) {
        return artifactsStore.getArtifact(executionId, ArtifactType.Response, null);
    }

    /**
     * Gets the stored request file for the specified execution id. It will be available only if the
     * process is executing asynchronously
     */
    public Resource getStoredRequest(String executionId) {
        return artifactsStore.getArtifact(executionId, ArtifactType.Request, null);
    }

    /** Gets the stored request as a parsed object */
    public ExecuteType getStoredRequestObject(String executionId) throws IOException {
        Resource resource = getStoredRequest(executionId);
        if (resource == null || resource.getType() == Type.UNDEFINED) {
            return null;
        } else {
            try (InputStream in = resource.in()) {
                WPSConfiguration config = new WPSConfiguration();
                Parser parser = new Parser(config);
                return (ExecuteType) parser.parse(in);
            } catch (SAXException | ParserConfigurationException e) {
                throw new WPSException("Could not parse the stored WPS request", e);
            }
        }
    }

    /** Stores the request in a binary resource for efficient later retrieval */
    public void storeRequestObject(ExecuteType execute, String executionId) throws IOException {
        Resource resource = getStoredRequest(executionId);
        try (OutputStream out = resource.out()) {
            WPSConfiguration config = new WPSConfiguration();
            Encoder encoder = new Encoder(config);
            encoder.encode(execute, WPS.Execute, out);
        }
    }

    // -----------------------------------------------------------------
    // DispatcherCallback methods
    // -----------------------------------------------------------------

    public void finished(Request request) {
        // if we did not generate any process id, no resources have been added
        if (executionId.get() == null) {
            return;
        }

        // grab the id and un-bind the thread local
        String id = executionId.get();
        executionId.remove();

        // cleanup automatically if the process is synchronous
        if (resourceCache.get(id).synchronouos) {
            cleanProcess(id, false);
            resourceCache.remove(id);
        }
    }

    public void finished(String executionId) {
        // cleanup the thread local, in case it has any id in it
        this.executionId.remove();

        // cleanup the temporary resources
        cleanProcess(executionId, false);

        // mark the process as complete
        resourceCache.get(executionId).completionTime = System.currentTimeMillis();
    }

    /**
     * Cleans up all the resources associated to a certain id. It is called automatically when the
     * request ends for synchronous processes, for asynch ones it will be triggered by the process
     * completion
     */
    public void cleanProcess(String id, boolean cancelled) {
        // delete all resources associated with the process
        ExecutionResources executionResources = resourceCache.get(id);
        for (WPSResource resource : executionResources.temporary) {
            try {
                resource.delete();
            } catch (Throwable t) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to clean up the WPS resource " + resource.getName(),
                        t);
            }
        }

        // in case of cancellation, remove also the results
        if (cancelled) {
            try {
                artifactsStore.clearArtifacts(id);
            } catch (IOException e) {
                throw new WPSException("Failed to clear the process artifacts");
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

    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
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
                cleanProcess(id, false);
            }
            resourceCache.clear();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // see if we have an artifacts store in the app context, otherwise use the default one
        ProcessArtifactsStore store = GeoServerExtensions.bean(ProcessArtifactsStore.class);
        if (store == null) {
            store = new DefaultProcessArtifactsStore();
        }
        this.artifactsStore = store;
    }

    public ProcessArtifactsStore getArtifactsStore() {
        return artifactsStore;
    }

    public void cleanExpiredResources(long expirationThreshold, ProcessStatusTracker tracker) {
        for (Resource r : artifactsStore.listExecutionResourcess()) {
            ExecutionStatus status = tracker.getStatus(r.name());
            // remove only the things that are not running
            if (status == null || status.getPhase().isExecutionCompleted()) {
                cleanupResource(r, expirationThreshold);
            }
        }
    }

    private boolean cleanupResource(Resource resource, long expirationThreshold) {
        boolean result = true;
        Type resourceType = resource.getType();
        if (resourceType == Type.RESOURCE && resource.lastmodified() < expirationThreshold) {
            result = resource.delete();
        } else if (resourceType == Type.DIRECTORY) {
            long directoryModified = resource.lastmodified();
            for (Resource child : resource.list()) {
                result &= cleanupResource(child, expirationThreshold);
            }
            // Cleanup the directory too if all the children have been cleanup
            if (result && directoryModified < expirationThreshold) {
                result &= resource.delete();
            }
        }

        return result;
    }

    @Override
    public void dismissed(ProcessEvent event) throws WPSException {
        String executionId = event.getStatus().getExecutionId();
        cleanProcess(executionId, true);
    }
}
