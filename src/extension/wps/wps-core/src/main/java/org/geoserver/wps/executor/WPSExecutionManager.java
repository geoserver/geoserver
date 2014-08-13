/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wps10.ExecuteResponseType;

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.XmlObjectEncodingResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Manages the process runs for both synchronous and asynchronous processes
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class WPSExecutionManager implements ApplicationContextAware,
        ApplicationListener<ApplicationEvent> {

    private static final Logger LOGGER = Logging.getLogger(WPSExecutionManager.class);

    private ExecutorService storedResponseWriters = Executors.newCachedThreadPool();

    ApplicationContext applicationContext;

    private WPSResourceManager resourceManager;

    private List<ProcessManager> processManagers;

    private Map<String, AsynchronousProcessContext> contexts = new ConcurrentHashMap<String, AsynchronousProcessContext>();

    private int connectionTimeout;

    public WPSExecutionManager(WPSResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    WPSResourceManager getResourceManager() {
        return resourceManager;
    }
    
    /**
     * This call should only be used by process chaining to avoid deadlocking due to execution
     * threads starvation
     * 
     * @param request
     * @return
     */
    Map<String, Object> submitChained(ExecuteRequest request) {
        Name processName = request.getProcessName();
        ProcessManager processManager = getProcessManager(processName);
        String executionId = resourceManager.getExecutionId(true);
        Map<String, Object> inputs = request.getProcessInputs(this);
        return processManager.submitChained(executionId, processName, inputs);
    }
    

    /**
     * Process submission, not blocking. Returns an id that can be used to get the process status
     * and result later.
     * 
     * @param ExecuteType The request to be executed
     * @param inputs The process inputs
     * @return The execution id
     * @throws ProcessException
     */
    public String submit(ExecuteRequest request, boolean synchronous) throws ProcessException {
        Name processName = request.getProcessName();
        ProcessManager processManager = getProcessManager(processName);
        LazyInputMap inputs = request.getProcessInputs(this);
        String executionId = resourceManager.getExecutionId(synchronous);
        final AsynchronousProcessContext context = new AsynchronousProcessContext(request,
                executionId, inputs, processManager, applicationContext);
        contexts.put(executionId, context);
        if(!synchronous) {
            LOGGER.log(Level.INFO, "Submitting new asynch process " + processName.getURI() + " with execution id " + executionId);
        }
        processManager.submit(executionId, processName, inputs, request.isAsynchronous());
        if (request.isAsynchronous()) {
            // ah, we need to store the output at the end, schedule a thread that will
            // do as soon as the process is done executing
            storedResponseWriters.submit(new Runnable() {

                @Override
                public void run() {
                    
                    context.writeResponseFile();
                }
            });
        }

        return executionId;
    }

    /**
     * Returns the status response for an asynch call if the id is known, null otherwise (it means
     * the process is either unknown or its execution already completed, in the latter case calling
     * getStoredResponse(executionId) will provide the stored response, assuming not too much time
     * passed between the end of the execution and the
     */
    public ExecuteResponseType getStatus(String executionId) {
        AsynchronousProcessContext context = contexts.get(executionId);
        if (context == null) {
            return null;
        }

        return context.getStatusResponse();
    }

    /**
     * Returns the stored response file for the specified execution (which has already completed its
     * lifecycle)
     * 
     * @param executionId
     * @return
     */
    public File getStoredResponse(String executionId) {
        return resourceManager.getStoredResponseFile(executionId);
    }
    
    public File getStoredOutput(String executionId, String outputId) {
        return resourceManager.getOutputFile(executionId, outputId);
    }

    /**
     * Cancels a process
     * 
     * @param executionId
     */
    public void cancel(String executionId) {
        AsynchronousProcessContext context = contexts.get(executionId);
        if (context != null) {
            context.processManager.cancel(executionId);
        }
    }

    /**
     * Returns the execute response for synch requests. This call is blocking, the caller will be
     * blocked until the process completes both input parsing and execution. The code will throw an
     * exception is the process is to be executed in stored mode.
     * 
     * @param executionId
     * @return
     */
    public Map<String, Object> getOutput(String executionId, long timeout) throws ProcessException {
        for (ProcessManager pm : getProcessManagers()) {
            Map<String, Object> output = pm.getOutput(executionId, timeout);
            if (output != null) {
                contexts.remove(executionId);
                return output;
            }
        }
        throw new ProcessException("Failed to find output for execution " + executionId);
    }

    List<ProcessManager> getProcessManagers() {
        if (processManagers == null) {
            synchronized (this) {
                if (processManagers == null) {
                    processManagers = GeoServerExtensions.extensions(ProcessManager.class);
                }
            }
        }
        return processManagers;
    }

    ProcessManager getProcessManager(Name processName) {
        for (ProcessManager pm : getProcessManagers()) {
            if (pm.canHandle(processName)) {
                return pm;
            }
        }

        throw new WPSException("Could not find a ProcessManager able to run this process: "
                + processName);
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.applicationContext = context;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            if (storedResponseWriters == null) {
                storedResponseWriters = Executors.newCachedThreadPool();
            } else if (event instanceof ContextClosedEvent) {
                storedResponseWriters.shutdownNow();
            }
        }
    }

    public class AsynchronousProcessContext {

        String executionId;

        LazyInputMap inputs;

        ProcessManager processManager;

        ExecuteRequest request;

        volatile Exception exception;

        Date started;

        private float inputWeight;

        private float outputWeight;

        private float processWeight;

        public AsynchronousProcessContext(ExecuteRequest request, String executionId,
                LazyInputMap inputs, ProcessManager processManager,
                ApplicationContext applicationContext) {
            this.request = request;
            this.executionId = executionId;
            this.inputs = inputs;
            this.processManager = processManager;
            this.started = new Date();
            // there are three fases running a process
            // 1 - retrieve and parse inputs
            // 2 - process
            // 3 - encode outputs
            // Here we have a simple heuristics to guess how long each one is
            this.inputWeight = inputs.longParse() ? 0.33f : 0f;
            this.outputWeight = hasComplexOutputs() ? 0.33f : 0f;
            this.processWeight = 1 - inputWeight - outputWeight;
        }
        
        boolean hasComplexOutputs() {
            ProcessFactory pf = GeoServerProcessors.createProcessFactory(request.getProcessName());
            Map<String, Parameter<?>> resultInfo = pf.getResultInfo(request.getProcessName(), inputs);
            for (Parameter<?> param : resultInfo.values()) {
                List<ProcessParameterIO> ppios = ProcessParameterIO.findAll(param, applicationContext);
                for (ProcessParameterIO ppio : ppios) {
                    if(ppio instanceof ComplexPPIO) {
                        return true;
                    }
                }
            }
            return false;
        }

        ExecutionStatus getOverallStatus() {
            ExecutionStatus inner = processManager.getStatus(executionId);
            // the process already completed?
            if (inner == null || inner.phase == ProcessState.COMPLETED) {
                if (exception != null) {
                    // failed
                    return new ExecutionStatus(request.getProcessName(), executionId, ProcessState.COMPLETED, 100f, null);
                } else {
                    // Still running, it's writing the output. Right now we have no way to track the
                    // output progress, so return 66% complete
                    return new ExecutionStatus(request.getProcessName(), executionId, ProcessState.RUNNING,
                            66f, null);
                }
            } else {
                // still running
                float progress = inputs.getRetrievedInputPercentage() * inputWeight;
                progress += inner.getProgress() * processWeight;
                return new ExecutionStatus(request.getProcessName(), executionId, ProcessState.RUNNING, progress, inner.getTask());
            }
        }

        ExecuteResponseType getStatusResponse() {
            ExecutionStatus overallStatus;
            if (request.isStatusEnabled()) {
                // user requested to get status updates
                overallStatus = getOverallStatus();
            } else {
                // only stored, we won't give updates until the process is completed (the
                // spec demands this, "If status is "false" then the Status element shall not be
                // updated until the process either completes successfully or fails)
                overallStatus = new ExecutionStatus(request.getProcessName(), executionId, ProcessState.QUEUED,
                        0f, null);
            }
            ExecuteResponseBuilder responseBuilder = new ExecuteResponseBuilder(request.getRequest(),
                    applicationContext, started);
            responseBuilder.setExecutionId(executionId);
            responseBuilder.setStatus(overallStatus);
            responseBuilder.setException(exception);
            return responseBuilder.build();
        }

        public void writeResponseFile() {
            try {
                resourceManager.setCurrentExecutionId(executionId);
                ExecuteResponseBuilder responseBuilder = new ExecuteResponseBuilder(
                        request.getRequest(), applicationContext, started);
                responseBuilder.setExecutionId(executionId);
                try {
                    Map<String, Object> outputs = processManager.getOutput(executionId, -1);
                    responseBuilder.setOutputs(outputs);
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "Request " + executionId + " failed during execution", exception);
                    responseBuilder.setException(exception);
                }

                // write to a temp file (as that might take time) and only when done switch to the
                // actual output file
                File output = resourceManager.getStoredResponseFile(executionId);
                try {
                    writeOutResponse(responseBuilder, output);
                } catch (Exception e) {
                    // maybe it was an exception during output encoding, try to write out
                    // the error if possible
                    LOGGER.log(Level.SEVERE, "Request failed during output encoding", e);
                    responseBuilder.setException(e);
                    writeOutResponse(responseBuilder, output);
                }
            } catch (Exception e) {
                // ouch, this is bad, we can just log the output...
                LOGGER.log(Level.SEVERE,
                        "Failed to write out the stored WPS response for executionId "
                                + executionId, e);

            } finally {
                contexts.remove(executionId);
            }
        }

        void writeOutResponse(ExecuteResponseBuilder responseBuilder, File output)
                throws IOException {
            FileOutputStream fos = null;
            File tmpOutput = new File(output.getParent(), "tmp" + output.getName());
            try {
                ExecuteResponseType response = responseBuilder.build();
                XmlObjectEncodingResponse encoder = new XmlObjectEncodingResponse(
                        ExecuteResponseType.class, "ExecuteResponse", WPSConfiguration.class);

                fos = new FileOutputStream(tmpOutput);
                encoder.write(response, fos, null);
                fos.flush();
                fos.close();
                if (!tmpOutput.renameTo(output)) {
                    LOGGER.log(Level.SEVERE, "Failed to rename " + tmpOutput + " to " + output);
                } else {
                    LOGGER.log(Level.FINE, "Asynch process final response written to " + output.getAbsolutePath());
                }
            } finally {
                IOUtils.closeQuietly(fos);
                if (tmpOutput != null) {
                    tmpOutput.delete();
                }
            }
        }

    }

   

}
