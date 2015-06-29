/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import org.geoserver.wps.ProcessDismissedException;
import org.geotools.util.DelegateProgressListener;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

/**
 * A listener wrapper that will forcefully fail a process once the max time expired
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class MaxExecutionTimeListener extends DelegateProgressListener {

    long maxExecutionTime;
    long maxTotalTime;
    long queuedTime;
    long startTime;

    public MaxExecutionTimeListener(ProgressListener progress, long maxExecutionTime, long maxTotalTime) {
        super(progress);
        this.maxExecutionTime = maxExecutionTime;
        this.maxTotalTime = maxTotalTime;
        this.queuedTime = System.currentTimeMillis();
        this.startTime = 0;
    }

    @Override
    public boolean isCanceled() {
        if (isExpired()) {
            return true;
        } else {
            return super.isCanceled();
        }
    }

    private void checkNotExpired() {
        if (isExpired()) {
            throw new ProcessDismissedException(this);
        }
    }

    /**
     * Returns true if the execution went beyond the allowed max time
     * 
     * @return
     */
    public boolean isExpired() {
        boolean maxExecutionTimeExceeded = maxExecutionTime > 0 && startTime > 0 && (System.currentTimeMillis() - startTime) > maxExecutionTime;
        boolean maxTotalTimeExceeded = maxTotalTime > 0 && (System.currentTimeMillis() - queuedTime) > maxTotalTime;
        return maxExecutionTimeExceeded || maxTotalTimeExceeded;
    }

    /**
     * The maximum execution time
     * 
     * @return
     */
    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }

    /**
     * The maximum total time
     * 
     * @return
     */
    public long getMaxTotalTime() {
        return maxTotalTime;
    }

    public void started() {
        this.startTime = System.currentTimeMillis();
        checkNotExpired();
        super.started();
    }

    public void complete() {
        checkNotExpired();
        super.complete();
    }

    public String getDescription() {
        checkNotExpired();
        return super.getDescription();
    }

    public InternationalString getTask() {
        checkNotExpired();
        return super.getTask();
    }

    public void progress(float progress) {
        checkNotExpired();
        super.progress(progress);
    }

    public float getProgress() {
        checkNotExpired();
        return super.getProgress();
    }

    public void setDescription(String description) {
        checkNotExpired();
        super.setDescription(description);
    }

    public void setTask(InternationalString task) {
        checkNotExpired();
        super.setTask(task);
    }

    public void warningOccurred(String source, String location, String warning) {
        checkNotExpired();
        super.warningOccurred(source, location, warning);
    }
}
