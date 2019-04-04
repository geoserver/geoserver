/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.threadlocals.ThreadLocalsTransfer;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public class DefaultProcessManager
        implements ProcessManager, ExtensionPriority, ApplicationListener<ApplicationEvent> {

    ConcurrentHashMap<String, Future<Map<String, Object>>> executions =
            new ConcurrentHashMap<String, Future<Map<String, Object>>>();

    ThreadPoolExecutor synchService;

    ThreadPoolExecutor asynchService;

    WPSResourceManager resourceManager;

    public DefaultProcessManager(WPSResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void setMaxAsynchronousProcesses(int maxAsynchronousProcesses) {
        if (asynchService == null) {
            // create a fixed size pool. If we allow a delta between core and max
            // the pool will create new threads only if the queue is full, but the linked queue
            // never is
            asynchService =
                    new ThreadPoolExecutor(
                            maxAsynchronousProcesses,
                            maxAsynchronousProcesses,
                            0L,
                            TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>());
        } else {
            // JDK 11 checks the relation between core and max pool size on each set,
            // need to lower core pool size before changing max
            asynchService.setCorePoolSize(1);
            asynchService.setMaximumPoolSize(maxAsynchronousProcesses);
            asynchService.setCorePoolSize(maxAsynchronousProcesses);
        }
    }

    public void setMaxSynchronousProcesses(int maxSynchronousProcesses) {
        if (synchService == null) {
            // create a fixed size pool. If we allow a delta between core and max
            // the pool will create new threads only if the queue is full, but the linked queue
            // never is
            synchService =
                    new ThreadPoolExecutor(
                            maxSynchronousProcesses,
                            maxSynchronousProcesses,
                            0L,
                            TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>());
        } else {
            // JDK 11 checks the relation between core and max pool size on each set,
            // need to lower core pool size before changing max
            synchService.setCorePoolSize(1);
            synchService.setMaximumPoolSize(maxSynchronousProcesses);
            synchService.setCorePoolSize(maxSynchronousProcesses);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextClosedEvent) {
            synchService.shutdownNow();
            asynchService.shutdownNow();
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
    public Map<String, Object> submitChained(
            String executionId,
            Name processName,
            Map<String, Object> inputs,
            ProgressListener listener)
            throws ProcessException {
        // straight execution, no thread pooling, we're already running in the parent process thread
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(processName, true);
        if (pf == null) {
            throw new WPSException("No such process: " + processName);
        }

        // execute the process in the same thread as the caller
        Process p = pf.create(processName);
        Map<String, Object> result = p.execute(inputs, listener);
        return result;
    }

    @Override
    public void submit(
            String executionId,
            Name processName,
            Map<String, Object> inputs,
            ProgressListener listener,
            boolean background)
            throws ProcessException {
        ProcessCallable callable = new ProcessCallable(processName, inputs, listener);
        Future<Map<String, Object>> future;
        if (background) {
            future = asynchService.submit(callable);
        } else {
            future = synchService.submit(callable);
        }
        executions.put(executionId, future);
    }

    @Override
    public Map<String, Object> getOutput(String executionId, long timeout) throws ProcessException {
        Future<Map<String, Object>> future = executions.get(executionId);
        if (future == null) {
            return null;
        }
        boolean timedOut = false;
        try {
            Map<String, Object> result;
            if (timeout <= 0) {
                result = future.get();
            } else {
                result = future.get(timeout, TimeUnit.MILLISECONDS);
            }
            return result;
        } catch (TimeoutException e) {
            timedOut = true;
            throw new ProcessException(e);
        } catch (Exception e) {
            if (e instanceof ExecutionException && e.getCause() instanceof Exception) {
                e = (Exception) e.getCause();
            }
            if (e instanceof ProcessException) {
                throw (ProcessException) e;
            } else if (e instanceof WPSException) {
                throw (WPSException) e;
            } else {
                throw new ProcessException("Process execution " + executionId + " failed", e);
            }
        } finally {
            if (!timedOut) {
                // we're done
                executions.remove(executionId);
            }
        }
    }

    @Override
    public void cancel(String executionId) {
        Future future = executions.get(executionId);
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }

    class ProcessCallable implements Callable<Map<String, Object>> {

        Name processName;

        Map<String, Object> inputs;

        ThreadLocalsTransfer threadLocalTransfer;

        ProgressListener listener;

        public ProcessCallable(
                Name processName, Map<String, Object> inputs, ProgressListener listener) {
            this.processName = processName;
            this.inputs = inputs;
            this.listener = listener;
            this.threadLocalTransfer = new ThreadLocalsTransfer();
        }

        @Override
        public Map<String, Object> call() throws Exception {
            try {
                // transfer the thread locals to this execution context
                threadLocalTransfer.apply();

                ProcessFactory pf = GeoServerProcessors.createProcessFactory(processName, true);
                if (pf == null) {
                    throw new WPSException("No such process: " + processName);
                }

                // execute the process
                Map<String, Object> result = null;
                Process p = pf.create(processName);
                result = p.execute(inputs, listener);
                return result;
            } finally {
                // clean up the thread locals
                threadLocalTransfer.cleanup();
            }
        }
    }
}
