/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.threadlocals.ThreadLocalsTransfer;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

public class DefaultProcessManager implements ProcessManager, ExtensionPriority, ApplicationListener<ApplicationEvent> {

    ConcurrentHashMap<String, ExecutionStatusEx> executions = new ConcurrentHashMap<String, DefaultProcessManager.ExecutionStatusEx>();

    ThreadPoolExecutor synchService;

    ThreadPoolExecutor asynchService;
    
    WPSResourceManager resourceManager;

    public DefaultProcessManager(WPSResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void setMaxAsynchronousProcesses(int maxAsynchronousProcesses) {
        if(asynchService == null) {
            // create a fixed size pool. If we allow a delta between core and max 
            // the pool will create new threads only if the queue is full, but the linked queue never is
            asynchService = new ThreadPoolExecutor(maxAsynchronousProcesses, maxAsynchronousProcesses, 
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
        } else {
            asynchService.setCorePoolSize(maxAsynchronousProcesses);
            asynchService.setMaximumPoolSize(maxAsynchronousProcesses);
        }
    }

    public void setMaxSynchronousProcesses(int maxSynchronousProcesses) {
        if(synchService == null) {
            // create a fixed size pool. If we allow a delta between core and max 
            // the pool will create new threads only if the queue is full, but the linked queue never is
            synchService = new ThreadPoolExecutor(maxSynchronousProcesses, maxSynchronousProcesses, 
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
        } else {
            synchService.setCorePoolSize(maxSynchronousProcesses);
            synchService.setMaximumPoolSize(maxSynchronousProcesses);
        }
    }
    
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            if (event instanceof ContextClosedEvent) {
                synchService.shutdownNow();
                asynchService.shutdownNow();
            }
        }
    }

    /**
     * We can handle everything, other process managers will have to use a higher priority than us
     */
    @Override
    public boolean canHandle(Name processName) {
        return true;
    }

    @Override
    public Map<String, Object> submitChained(String executionId, Name processName,
            Map<String, Object> inputs) throws ProcessException {
        // straight execution, no thread pooling, we're already running in the parent process thread
        ProcessListener listener = new ProcessListener(new ExecutionStatus(processName, executionId, ProcessState.RUNNING, 0, null));
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(processName);
        if (pf == null) {
            throw new WPSException("No such process: " + processName);
        }

        // execute the process in the same thread as the caller
        Process p = pf.create(processName);
        Map<String, Object> result = p.execute(inputs, listener);
        if (listener.exception != null) {
            throw new ProcessException("Process failed: " + listener.exception.getMessage(),
                    listener.exception);
        }
        return result;
    }

    @Override
    public void submit(String executionId, Name processName, Map<String, Object> inputs,
            boolean background) throws ProcessException {
        ExecutionStatusEx status = new ExecutionStatusEx(processName, executionId);
        ProcessListener listener = new ProcessListener(status);
        status.listener = listener;
        ProcessCallable callable = new ProcessCallable(inputs, status);
        Future<Map<String, Object>> future;
        if(background) {
            future = asynchService.submit(callable);
        } else {
            future = synchService.submit(callable);
        }
        status.future = future;
        executions.put(executionId, status);
    }

    @Override
    public ExecutionStatus getStatus(String executionId) {
        ExecutionStatusEx status = executions.get(executionId);
        if (status != null) {
            return status.getStatus();
        } else {
            return null;
        }
    }

    @Override
    public Map<String, Object> getOutput(String executionId, long timeout) throws ProcessException {
        ExecutionStatusEx status = executions.get(executionId);
        if (status == null) {
            return null;
        }
        try {
            if (timeout <= 0) {
                return status.future.get();
            } else {
                return status.future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            if(e instanceof ExecutionException && e.getCause() instanceof Exception) {
                e = (Exception) e.getCause();
            }
            if (e instanceof ProcessException) {
                throw (ProcessException) e;
            } else {
                throw new ProcessException("Process execution " + executionId + " failed", e);
            }
        } finally {
            // we're done
            executions.remove(executionId);
        }
    }

    @Override
    public void cancel(String executionId) {
        ExecutionStatusEx status = executions.get(executionId);
        if (status != null) {
            status.setPhase(ProcessState.CANCELLED);
            status.future.cancel(true);
            status.listener.setCanceled(true);
        }
    }

    @Override
    public List<ExecutionStatus> getRunningProcesses() {
        List<ExecutionStatus> result = new ArrayList<ExecutionStatus>();
        for (ExecutionStatusEx status : executions.values()) {
            result.add(status.getStatus());
        }
        return result;
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }

    class ProcessCallable implements Callable<Map<String, Object>> {

        Map<String, Object> inputs;

        ExecutionStatusEx status;
        
        ThreadLocalsTransfer threadLocalTransfer;

        public ProcessCallable(Map<String, Object> inputs, ExecutionStatusEx status) {
            this.inputs = inputs;
            this.status = status;
            this.threadLocalTransfer = new ThreadLocalsTransfer();
        }

        @Override
        public Map<String, Object> call() throws Exception {
            try {
                // transfer the thread locals to this execution context
                threadLocalTransfer.apply();
                
                resourceManager.setCurrentExecutionId(status.getExecutionId());
                status.setPhase(ProcessState.RUNNING);
                ProcessListener listener = status.listener;
                Name processName = status.getProcessName();
                ProcessFactory pf = GeoServerProcessors.createProcessFactory(processName);
                if (pf == null) {
                    throw new WPSException("No such process: " + processName);
                }
    
                // execute the process
                Map<String, Object> result = null;
                try {
                    Process p = pf.create(processName);
                    result = p.execute(inputs, listener);
                    if (listener.exception != null) {
                        throw new WPSException("Process failed: " + listener.exception.getMessage(),
                                listener.exception);
                    }
                    return result;
                } finally {
                    // update status unless cancelled
                    if (status.getPhase() == ProcessState.RUNNING) {
                        status.setPhase(ProcessState.COMPLETED);
                    }
                }
            } finally {
                // clean up the thread locals
                threadLocalTransfer.cleanup();
            }
        }

    }

    /**
     * A pimped up execution status
     * 
     * @author Andrea Aime - GeoSolutions
     */
    static class ExecutionStatusEx extends ExecutionStatus {

        Future<Map<String, Object>> future;

        ProcessListener listener;

        public ExecutionStatusEx(Name processName, String executionId) {
            super(processName, executionId, ProcessState.QUEUED, 0, null);
        }

        public ExecutionStatus getStatus() {
            return new ExecutionStatus(processName, executionId, phase, progress, task);
        }
    }

    /**
     * Listens to the process progress and allows to cancel it
     * 
     * @author Andrea Aime - GeoSolutions
     */
    static class ProcessListener implements ProgressListener {

        static final Logger LOGGER = Logging.getLogger(ProcessListener.class);

        ExecutionStatus status;

        InternationalString task;

        String description;

        Throwable exception;

        public ProcessListener(ExecutionStatus status) {
            this.status = status;
        }

        @Override
        public InternationalString getTask() {
            return task;
        }

        @Override
        public void setTask(InternationalString task) {
            this.task = task;
            if(task != null) {
                status.setTask(task.toString());
            }
        }

        @Override
        public String getDescription() {
            return this.description;
        }

        @Override
        public void setDescription(String description) {
            this.description = description;

        }

        @Override
        public void started() {
            status.setPhase(ProcessState.RUNNING);
        }

        @Override
        public void progress(float percent) {
            status.setProgress(percent);
        }

        @Override
        public float getProgress() {
            return status.getProgress();
        }

        @Override
        public void complete() {
            // nothing to do
        }

        @Override
        public void dispose() {
            // nothing to do
        }

        @Override
        public boolean isCanceled() {
            return status.getPhase() == ProcessState.CANCELLED;
        }

        @Override
        public void setCanceled(boolean cancel) {
            if (cancel == true) {
                status.setPhase(ProcessState.CANCELLED);
            }

        }

        @Override
        public void warningOccurred(String source, String location, String warning) {
            LOGGER.log(Level.WARNING,
                    "Got a warning during process execution " + status.getExecutionId() + ": "
                            + warning);
        }

        @Override
        public void exceptionOccurred(Throwable exception) {
            this.exception = exception;
        }

    }

   

}
