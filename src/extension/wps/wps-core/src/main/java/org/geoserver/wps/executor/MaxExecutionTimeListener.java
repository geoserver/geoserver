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

    long startTime;

    long maxExecutionTime;

    public MaxExecutionTimeListener(ProgressListener progress, long maxExecutionTime) {
        super(progress);
        this.startTime = System.currentTimeMillis();
        this.maxExecutionTime = maxExecutionTime;
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
        return maxExecutionTime > 0 && (System.currentTimeMillis() - startTime) > maxExecutionTime;
    }

    /**
     * The maximum execution time
     * 
     * @return
     */
    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }

    public void started() {
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
