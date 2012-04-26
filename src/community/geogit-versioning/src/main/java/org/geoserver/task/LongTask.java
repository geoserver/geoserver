package org.geoserver.task;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.geotools.util.DefaultProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

public abstract class LongTask<V> implements Callable<V> {

    public static enum Status {
        ABORTED, RUNNING, QUEUED, CANCELLING, CANCELLED, FINISHED
    }

    private CompositeProgressListener progressListener = new CompositeProgressListener();

    private Status status = Status.QUEUED;

    private String title;

    private String description;

    private Exception exception;

    private V result;

    private boolean autoRemoveWhenFinished;

    protected LongTask() {
        this("", "");
    }

    protected LongTask(final String title, final String description) {
        this.title = title;
        this.description = description;
    }

    /**
     * @return the autoRemoveWhenFinished
     */
    public boolean isAutoRemoveWhenFinished() {
        return autoRemoveWhenFinished;
    }

    /**
     * @param autoRemoveWhenFinished
     *            the autoRemoveWhenFinished to set
     */
    public void setAutoRemoveWhenFinished(boolean autoRemoveWhenFinished) {
        this.autoRemoveWhenFinished = autoRemoveWhenFinished;
    }

    public void cancel() {
        if (!isDone()) {
            this.status = Status.CANCELLING;
            progressListener.setCanceled(true);
        }
    }

    public V getResult() {
        return result;
    }

    public void addProgressListener(final ProgressListener listener) {
        progressListener.add(listener);
    }

    public float getProgress() {
        return progressListener.getProgress();
    }

    public String getProgressMessage() {
        return progressListener.getDescription();
    }

    protected void setProgressMessage(String message) {
        progressListener.setDescription(message);
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public Exception getException() {
        return exception;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    public final V call() throws Exception {
        try {
            this.status = Status.RUNNING;
            this.result = callInternal(progressListener);
            if (progressListener.isCanceled()) {
                this.status = Status.CANCELLED;
            } else {
                this.status = Status.FINISHED;
            }
            return this.result;
        } catch (InterruptedException e) {
            progressListener.setCanceled(true);
            return null;
        } catch (Exception e) {
            Logging.getLogger(LongTask.class).log(Level.FINE, "LongTask failed: " + e.getMessage(), e);
            progressListener.complete();
            progressListener.exceptionOccurred(e);
            this.status = Status.ABORTED;
            this.setDescription(e.getMessage());
            this.exception = e;
            throw e;
        }
    }

    protected abstract V callInternal(final ProgressListener progressListener) throws Exception;

    private static class CompositeProgressListener extends DefaultProgressListener implements
            ProgressListener {

        private List<ProgressListener> listeners;

        public CompositeProgressListener() {
            listeners = new CopyOnWriteArrayList<ProgressListener>();
        }

        public void add(ProgressListener listener) {
            listeners.add(listener);
        }

        public void setTask(InternationalString task) {
            super.setTask(task);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).setTask(task);
            }
        }

        public void setDescription(String description) {
            super.setDescription(description);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).setDescription(description);
            }
        }

        public void started() {
            super.started();
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).started();
            }
        }

        public void progress(float percent) {
            // System.err.println("Progress: " + percent + "%");
            super.progress(percent);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).progress(percent);
            }
        }

        public void complete() {
            super.complete();
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).complete();
            }
        }

        public void dispose() {
            super.dispose();
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).dispose();
            }
            listeners.clear();
        }

        public boolean isCanceled() {
            if (super.isCanceled()) {
                return true;
            }
            for (int i = 0; i < listeners.size(); i++) {
                if (listeners.get(i).isCanceled()) {
                    return true;
                }
            }
            return false;
        }

        public void setCanceled(boolean cancel) {
            super.setCanceled(cancel);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).setCanceled(cancel);
            }
        }

        public void warningOccurred(String source, String location, String warning) {
            super.warningOccurred(source, location, warning);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).warningOccurred(source, location, warning);
            }
        }

        public void exceptionOccurred(Throwable exception) {
            super.exceptionOccurred(exception);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).exceptionOccurred(exception);
            }
        }
    }

    /**
     * Returns <tt>true</tt> if this task completed.
     * 
     * Completion may be due to normal termination, an exception, or cancellation -- in all of these
     * cases, this method will return <tt>true</tt>.
     * 
     * @return <tt>true</tt> if this task completed.
     */
    public boolean isDone() {
        return status == Status.ABORTED || status == Status.CANCELLED || status == Status.FINISHED;
    }
}
