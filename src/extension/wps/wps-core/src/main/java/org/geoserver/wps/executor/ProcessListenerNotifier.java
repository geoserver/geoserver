/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.wps.ProcessDismissedException;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.ProcessListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

/**
 * Handles notification to all the {@link ProcessListener} implementations for a given process
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessListenerNotifier {

    static final Logger LOGGER = Logging.getLogger(ProcessListenerNotifier.class);

    ExecutionStatus status;

    List<ProcessListener> listeners;

    WPSProgressListener progressListener;

    LazyInputMap inputs;

    Map<String, Object> outputs;

    ExecuteRequest request;

    public ProcessListenerNotifier(
            ExecutionStatus status,
            ExecuteRequest request,
            LazyInputMap inputs,
            List<ProcessListener> listeners) {
        this.status = status;
        this.request = request;
        this.progressListener = new WPSProgressListener();
        this.inputs = inputs;
        this.listeners = listeners;
        fireProcessSubmitted();
    }

    public void fireProcessSubmitted() {
        ProcessEvent event = new ProcessEvent(status, inputs);
        for (ProcessListener listener : listeners) {
            listener.submitted(event);
        }
    }

    public void fireProgress(float progress, String task) {
        if (progress > status.progress || StringUtils.equals(task, status.task)) {
            if (status.getPhase() == ProcessState.QUEUED) {
                status.setPhase(ProcessState.RUNNING);
            }
            status.setProgress(progress);
            status.setTask(task);

            // Update the estimated completion.
            // By default we estimate the completion as:
            // "time elapsed millis / percentage completed"
            if (progress > 0) {
                long timeElapsedMillis =
                        (new Date().getTime() - status.getCreationTime().getTime());
                int estimatedCompletionMillis =
                        (int)
                                ((timeElapsedMillis / progress) * timeElapsedMillis
                                        + timeElapsedMillis);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(status.getCreationTime());
                calendar.add(Calendar.MILLISECOND, estimatedCompletionMillis);
                status.setEstimatedCompletion(calendar.getTime());
            }

            ProcessEvent event = new ProcessEvent(status, inputs, outputs);
            for (ProcessListener listener : listeners) {
                listener.progress(event);
            }
        }
    }

    public void fireFailed(Throwable e) {
        status.setPhase(ProcessState.FAILED);
        if (e != null) {
            status.setException(e);
        }
        ProcessEvent event = new ProcessEvent(status, inputs, outputs);
        for (ProcessListener listener : listeners) {
            listener.failed(event);
        }
    }

    public void fireSucceded() {
        status.setPhase(ProcessState.SUCCEEDED);
        status.setProgress(100);
        status.setTask(null);
        ProcessEvent event = new ProcessEvent(status, inputs, outputs);
        for (ProcessListener listener : listeners) {
            listener.succeeded(event);
        }
    }

    public void fireCompleted() {
        if (status.getPhase() == ProcessState.RUNNING) {
            fireSucceded();
        } else if (status.getPhase() == ProcessState.DISMISSING) {
            fireDismissed();
        } else {
            fireFailed(null);
        }
    }

    /** Notifies all listeners that the process is being dismissed */
    public void dismiss() {
        this.status.phase = ProcessState.DISMISSING;
        ProcessEvent event = new ProcessEvent(status, inputs, outputs);
        for (ProcessListener listener : listeners) {
            listener.dismissing(event);
        }
    }

    /** Notifies all listeners that the process is being dismissed */
    public void fireDismissed() {
        this.status.phase = ProcessState.FAILED;
        ProcessEvent event = new ProcessEvent(status, inputs, outputs);
        for (ProcessListener listener : listeners) {
            listener.dismissed(event);
        }
    }

    /**
     * Listens to the process progress and allows to cancel it
     *
     * @author Andrea Aime - GeoSolutions
     */
    class WPSProgressListener implements ProgressListener {

        InternationalString task;

        String description;

        Throwable exception;

        @Override
        public InternationalString getTask() {
            return task;
        }

        @Override
        public void setTask(InternationalString task) {
            this.task = task;
            checkDismissed();
            fireProgress(status.progress, task.toString());
        }

        @Override
        public void started() {
            progress(0f);
        }

        @Override
        public void progress(float percent) {
            // force process to just exit immediately
            checkDismissed();
            fireProgress(percent, task != null ? task.toString() : null);
        }

        @Override
        public float getProgress() {
            return status.progress;
        }

        @Override
        public void complete() {
            progress(100);
        }

        @Override
        public void dispose() {
            // nothing to do
        }

        @Override
        public boolean isCanceled() {
            return status.phase == ProcessState.DISMISSING;
        }

        @Override
        public void setCanceled(boolean cancel) {
            dismiss();
        }

        @Override
        public void warningOccurred(String source, String location, String warning) {
            LOGGER.log(
                    Level.WARNING,
                    "Got a warning during process execution "
                            + status.getExecutionId()
                            + ": "
                            + warning);
            // force process to just exit immediately
            checkDismissed();
        }

        @Override
        public void exceptionOccurred(Throwable exception) {
            // do not record the exception if we just forced the process to bail out
            if (status.phase != ProcessState.DISMISSING) {
                this.exception = exception;
                fireFailed(exception);
            }
        }

        public Throwable getException() {
            return exception;
        }
    }

    public WPSProgressListener getProgressListener() {
        return progressListener;
    }

    /** Throws a process cancelled exception if the process has been cancelled */
    public void checkDismissed() {
        if (status.getPhase() == ProcessState.DISMISSING) {
            throw new ProcessDismissedException();
        }
    }
}
