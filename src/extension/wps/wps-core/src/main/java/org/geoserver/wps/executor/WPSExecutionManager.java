/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wps10.ExecuteResponseType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlObjectEncodingResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.threadlocals.ThreadLocalsTransfer;
import org.geoserver.wps.ProcessDismissedException;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.ProcessListener;
import org.geoserver.wps.UnknownExecutionIdException;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.util.SubProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;
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
 */
public class WPSExecutionManager
        implements ApplicationContextAware, ApplicationListener<ApplicationEvent> {

    private static final Logger LOGGER = Logging.getLogger(WPSExecutionManager.class);

    /**
     * The thread pool that will run the threads doing input decoding/process launch/output decoding
     * for asynchronous processes
     */
    private ExecutorService executors = Executors.newCachedThreadPool();

    /** Used to do run-time lookups of extension points */
    ApplicationContext applicationContext;

    /** The resource manager, the source of execution ids */
    private WPSResourceManager resourceManager;

    /** The classes that will actually run the process once the inputs are parsed */
    private List<ProcessManager> processManagers;

    /** Objects listening to the process lifecycles */
    private List<ProcessListener> listeners;

    /** The HTTP connection timeout for remote resources */
    private int connectionTimeout;

    /**
     * The status tracker, that will be periodically informed about processes still running, even if
     * they are not issuing events to the process listener
     */
    private ProcessStatusTracker statusTracker;

    /** The currently running processes */
    private Map<String, ProcessListenerNotifier> localProcesses =
            new ConcurrentHashMap<String, ProcessListenerNotifier>();

    /** The timer informing the status tracker of the currently executing processes */
    private Timer heartbeatTimer;

    /** The delay between one heartbeat and the next */
    private int heartbeatDelay;

    /** Used to retrieve the current WPSInfo */
    private GeoServer geoServer;

    public WPSExecutionManager(
            GeoServer geoServer,
            WPSResourceManager resourceManager,
            ProcessStatusTracker statusTracker) {
        this.resourceManager = resourceManager;
        this.statusTracker = statusTracker;
        this.geoServer = geoServer;
    }

    WPSResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * This call should only be used by process chaining to avoid deadlocking due to execution
     * threads starvation
     *
     * @param request
     * @param listener
     */
    Map<String, Object> submitChained(ExecuteRequest request, ProgressListener listener) {
        Name processName = request.getProcessName();
        ProcessManager processManager = getProcessManager(processName);
        String executionId = resourceManager.getExecutionId(true);
        LazyInputMap inputs = request.getProcessInputs(this);
        int inputsLongSteps = inputs.longStepCount();
        int longSteps = inputsLongSteps + 1;
        float longStepPercentage = 100f / longSteps;
        float inputPercentage = inputsLongSteps * longStepPercentage;
        float executionPercentage = 100 - inputPercentage;
        inputs.setListener(new SubProgressListener(listener, inputPercentage));
        ProgressListener executionListener =
                new SubProgressListener(listener, inputPercentage, executionPercentage);
        return processManager.submitChained(executionId, processName, inputs, executionListener);
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
    public ExecuteResponseType submit(final ExecuteRequest request, boolean synchronous)
            throws ProcessException {

        Name processName = request.getProcessName();
        ProcessManager processManager = getProcessManager(processName);
        String executionId = resourceManager.getExecutionId(synchronous);
        LazyInputMap inputs = request.getProcessInputs(WPSExecutionManager.this);
        request.validateOutputs(inputs);
        ExecutionStatus status =
                new ExecutionStatus(processName, executionId, request.isAsynchronous());
        status.setRequest(request.getRequest());
        long maxExecutionTime = getMaxExecutionTime(synchronous);
        long maxTotalTime = getMaxTotalTime(synchronous);
        Executor executor =
                new Executor(
                        request,
                        processManager,
                        processName,
                        inputs,
                        synchronous,
                        status,
                        resourceManager,
                        maxExecutionTime,
                        maxTotalTime);

        ExecuteResponseType response;
        if (synchronous) {
            response = executor.call();
        } else {
            LOGGER.log(
                    Level.INFO,
                    "Submitting new asynch process "
                            + processName.getURI()
                            + " with execution id "
                            + executionId);
            // building the response while the process is still "queued", will result in
            // ProcessAccepted in the response
            try {
                resourceManager.storeRequestObject(request.getRequest(), executionId);
            } catch (IOException e) {
                throw new WPSException(
                        "Failed to store original WPS request, which "
                                + "will be needed to encode the output",
                        e);
            }
            ExecuteResponseBuilder builder =
                    new ExecuteResponseBuilder(request.getRequest(), applicationContext, status);
            response = builder.build();
            // now actually start the process
            executors.submit(executor);
        }

        return response;
    }

    private long getMaxExecutionTime(boolean synchronous) {
        WPSInfo wps = geoServer.getService(WPSInfo.class);
        if (synchronous) {
            return wps.getMaxSynchronousExecutionTime() * 1000;
        } else {
            return wps.getMaxAsynchronousExecutionTime() * 1000;
        }
    }

    private long getMaxTotalTime(boolean synchronous) {
        WPSInfo wps = geoServer.getService(WPSInfo.class);
        if (synchronous) {
            return wps.getMaxSynchronousTotalTime() * 1000;
        } else {
            return wps.getMaxAsynchronousTotalTime() * 1000;
        }
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

        throw new WPSException(
                "Could not find a ProcessManager able to run this process: " + processName);
    }

    /** Returns the HTTP connection timeout for remote resource fetching */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the HTTP connection timeout for remote resource fetching
     *
     * @param connectionTimeout
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Sets the heartbeat delay for the processes that are running (to make sure we tell the rest of
     * the cluster the process is actually still running, even if it does not update its status)
     *
     * @param i
     */
    public void setHeartbeatDelay(int heartbeatDelay) {
        if (heartbeatDelay != this.heartbeatDelay) {
            this.heartbeatDelay = heartbeatDelay;
            if (heartbeatTimer != null) {
                heartbeatTimer.cancel();
            }
            heartbeatTimer = new Timer();
            heartbeatTimer.schedule(new HeartbeatTask(), heartbeatDelay);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.applicationContext = context;
        this.listeners = GeoServerExtensions.extensions(ProcessListener.class, context);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            if (executors == null) {
                executors = Executors.newCachedThreadPool();
            } else if (event instanceof ContextClosedEvent) {
                executors.shutdownNow();
            }
        }
    }

    /**
     * Linearly runs input decoding, execution and output encoding
     *
     * @author Andrea Aime - GeoSolutions
     */
    private final class Executor implements Callable<ExecuteResponseType> {
        private final ExecuteRequest request;

        private final ProcessManager processManager;

        private ExecutionStatus status;

        LazyInputMap inputs;

        private boolean synchronous;

        private ProcessListenerNotifier notifier;

        private ThreadLocalsTransfer transfer;

        private long maxExecutionTime;

        private long maxTotalTime;

        private Executor(
                ExecuteRequest request,
                ProcessManager processManager,
                Name processName,
                LazyInputMap inputs,
                boolean synchronous,
                ExecutionStatus status,
                WPSResourceManager resources,
                long maxExecutionTime,
                long maxTotalTime) {

            this.request = request;
            this.processManager = processManager;
            this.status = status;
            this.inputs = inputs;
            this.synchronous = synchronous;
            this.maxExecutionTime = maxExecutionTime;
            this.maxTotalTime = maxTotalTime;

            // if we execute asynchronously we'll need to make sure all thread locals are
            // transferred (in particular, the executionId in WPSResourceManager)
            if (status.isAsynchronous()) {
                this.transfer = new ThreadLocalsTransfer();
            }

            // preparing the listener that will report
            notifier = new ProcessListenerNotifier(status, request, inputs, listeners);
        }

        boolean hasComplexOutputs() {
            ProcessFactory pf =
                    GeoServerProcessors.createProcessFactory(request.getProcessName(), false);
            Map<String, Parameter<?>> resultInfo =
                    pf.getResultInfo(request.getProcessName(), inputs);
            for (Parameter<?> param : resultInfo.values()) {
                List<ProcessParameterIO> ppios =
                        ProcessParameterIO.findAll(param, applicationContext);
                for (ProcessParameterIO ppio : ppios) {
                    if (ppio instanceof ComplexPPIO) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public ExecuteResponseType call() {
            if (transfer != null) {
                try {
                    transfer.apply();
                    localProcesses.put(status.getExecutionId(), notifier);
                    return execute();
                } finally {
                    localProcesses.remove(status.getExecutionId());
                    try {
                        transfer.cleanup();
                    } finally {
                        resourceManager.finished(status.getExecutionId());
                    }
                }
            } else {
                return execute();
            }
        }

        private ExecuteResponseType execute() {
            ExecuteResponseType result = null;
            Map<String, Object> outputs = null;

            // prepare the lazy input map to report progress (for simple inputs the parse
            // already happened, but the output response is yet to be encoded, so we give
            // that a bit more in terms of percentage)
            int inputsLongSteps = inputs.longStepCount();
            int longSteps = inputsLongSteps + 1;
            if (hasComplexOutputs()) {
                longSteps++;
            }
            float longStepPercentage = 98f / longSteps;
            // Set the base to 0 in case of no inputs, as there is really nothing to do there,
            // this will make the process call the listener startup notification instead
            // otherwise the executor SubProgressListener won't delegate that method down
            int inputsBase = inputs.size() == 0 ? 0 : 1;
            float inputPercentage = inputsBase + inputsLongSteps * longStepPercentage;
            float outputPercentage = (hasComplexOutputs() ? longStepPercentage : 0) + 1;
            float executionPercentage = 100 - inputPercentage - outputPercentage;
            ProgressListener listener = notifier.getProgressListener();

            listener = new MaxExecutionTimeListener(listener, maxExecutionTime, maxTotalTime);
            try {
                // have the input map give us progress report
                inputs.setListener(new SubProgressListener(listener, 0, inputPercentage));

                // submit
                SubProgressListener executionListener =
                        new SubProgressListener(listener, inputPercentage, executionPercentage);
                notifier.checkDismissed();
                processManager.submit(
                        status.getExecutionId(),
                        status.getProcessName(),
                        inputs,
                        executionListener,
                        status.isAsynchronous());

                // grab the output (and get blocked waiting for it)
                notifier.checkDismissed();
                outputs = processManager.getOutput(status.getExecutionId(), -1);
                if (status.getPhase() == ProcessState.RUNNING) {
                    notifier.fireProgress(
                            inputPercentage + executionPercentage,
                            "Execution completed, preparing to write response");
                }
            } catch (ProcessDismissedException e) {
                // that's fine, move on writing the output
            } catch (Exception e) {
                if (status.getPhase() != ProcessState.DISMISSING) {
                    LOGGER.log(Level.SEVERE, "Process execution failed", e);
                    notifier.fireFailed(e);
                }
            } finally {
                // build result and return
                if (status.getPhase() == ProcessState.RUNNING) {
                    notifier.fireProgress(inputPercentage + executionPercentage, "Writing outputs");
                }

                try {
                    // the build must say we completed, even if writing might take some time
                    ExecutionStatus completedStatus = new ExecutionStatus(status);
                    if (status.getPhase() == ProcessState.RUNNING) {
                        completedStatus.setPhase(ProcessState.SUCCEEDED);
                    } else {
                        completedStatus.setPhase(ProcessState.FAILED);
                    }
                    ExecuteResponseBuilder builder =
                            new ExecuteResponseBuilder(
                                    status.getRequest(), applicationContext, completedStatus);
                    builder.setOutputs(outputs);

                    ProgressListener outputListener =
                            new SubProgressListener(
                                    listener,
                                    inputPercentage + executionPercentage,
                                    outputPercentage);
                    result = builder.build(outputListener);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed writing out the results", e);
                    if (status.getPhase() != ProcessState.DISMISSING) {
                        notifier.fireFailed(e);
                    }
                }

                // if we are being cancelled, clean up everything right now
                if (status.getPhase() != ProcessState.DISMISSING) {
                    if (synchronous) {
                        notifier.fireCompleted();
                    } else {
                        try {
                            // write out the final response to a file that will be kept there
                            // for GetExecutionStatus requests
                            Resource output =
                                    resourceManager.getStoredResponse(status.getExecutionId());
                            try {
                                if (status.getPhase() == ProcessState.RUNNING) {
                                    notifier.fireProgress(
                                            inputPercentage
                                                    + executionPercentage
                                                    + outputPercentage / 2,
                                            "Writing out response");
                                }
                                writeOutResponse(result, output);
                                // just in case it got cancelled while we were writing the output
                                notifier.fireCompleted();
                            } catch (Exception e) {
                                // maybe it was an exception during output encoding, try to write
                                // out the error if possible
                                LOGGER.log(
                                        Level.SEVERE, "Request failed during output encoding", e);
                                status.setException(e);
                                ExecuteResponseBuilder builder =
                                        new ExecuteResponseBuilder(
                                                status.getRequest(), applicationContext, status);
                                builder.setOutputs(null);
                                result = builder.build();
                                writeOutResponse(result, output);
                                notifier.fireCompleted();
                            }
                        } catch (Exception e) {
                            // ouch, this is bad, we can just log the output...
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Failed to write out the stored WPS response for executionId "
                                            + status.getExecutionId(),
                                    e);
                            notifier.fireFailed(e);
                            throw new WPSException("Execution failed while writing the outputs", e);
                        }
                    }
                } else {
                    notifier.fireCompleted();
                }
            }

            return result;
        }

        void writeOutResponse(ExecuteResponseType response, Resource output) throws IOException {
            try (OutputStream os = output.out()) {
                XmlObjectEncodingResponse encoder =
                        new XmlObjectEncodingResponse(
                                ExecuteResponseType.class,
                                "ExecuteResponse",
                                WPSConfiguration.class);

                encoder.write(response, os, null);
                LOGGER.log(Level.FINE, "Asynch process final response written to " + output.path());
            }
        }
    }

    /**
     * Touches the running processes, making sure we don't end up cleaning their resources by
     * mistake while they are still running (this is required for processes that are not reporting
     * progress)
     *
     * @author Andrea Aime - GeoSolutions
     */
    private class HeartbeatTask extends TimerTask {

        @Override
        public void run() {
            for (String executionId : localProcesses.keySet()) {
                statusTracker.touch(executionId);
            }
        }
    }

    /**
     * Cancels the execution of the given process, notifying the process managers if needs be
     *
     * @param executionId
     */
    public void cancel(String executionId) {
        ExecutionStatus status = statusTracker.getStatus(executionId);
        if (status == null) {
            throw new UnknownExecutionIdException(executionId);
        }

        // if the process is running locally, clean it
        if (status.getPhase() == ProcessState.RUNNING) {
            ProcessListenerNotifier notifier = localProcesses.get(executionId);
            if (notifier != null) {
                notifier.dismiss();
            } else {
                status.setPhase(ProcessState.DISMISSING);
                statusTracker.dismissing(new ProcessEvent(status, null, null));
            }

            // did it manage to complete while we were notifying dismiss?
            status = statusTracker.getStatus(executionId);
            if (!status.getPhase().isExecutionCompleted()) {
                return;
            }
        }

        // alredy completed, clean it up
        ProcessEvent event = new ProcessEvent(status, null);
        statusTracker.dismissed(event);
        resourceManager.dismissed(event);
    }
}
