/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wps.ProcessDismissedException;
import org.geotools.data.util.DelegateProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

/**
 * A listener wrapper that will forcefully fail a process once the max time expired
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MaxExecutionTimeListener extends DelegateProgressListener {

    static final Logger LOGGER = Logging.getLogger(MaxExecutionTimeListener.class);

    long maxExecutionTime;
    long maxTotalTime;
    long queuedTime;
    long startTime;

    public MaxExecutionTimeListener(
            ProgressListener progress, long maxExecutionTime, long maxTotalTime) {
        super(progress);

        if (maxTotalTime > 0 && maxTotalTime < maxExecutionTime) {
            LOGGER.log(
                    Level.WARNING,
                    "The maximum total queuing and execution time allowed for processes is "
                            + "less than the maximum allowed execution time");
        }

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

    /** Returns true if the execution went beyond the allowed max time */
    public boolean isExpired() {
        boolean maxExecutionTimeExceeded =
                maxExecutionTime > 0
                        && startTime > 0
                        && (System.currentTimeMillis() - startTime) > maxExecutionTime;
        boolean maxTotalTimeExceeded =
                maxTotalTime > 0 && (System.currentTimeMillis() - queuedTime) > maxTotalTime;
        return maxExecutionTimeExceeded || maxTotalTimeExceeded;
    }

    /** The maximum execution time */
    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }

    /** The maximum total time */
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

    public void setTask(InternationalString task) {
        checkNotExpired();
        super.setTask(task);
    }

    public void warningOccurred(String source, String location, String warning) {
        checkNotExpired();
        super.warningOccurred(source, location, warning);
    }
}
