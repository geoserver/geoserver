/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import org.geoserver.wps.WPSException;
import org.geotools.text.Text;
import org.opengis.util.ProgressListener;

/**
 * Interface used by scripts to report about current progress and eventually throw service
 * exceptions with the necessary OGC details
 *
 * @author Andrea Aime - GeoSolutions
 */
public class StatusMonitor {
    ProgressListener listener;

    private float total;

    private float work;

    private float base;

    /** Creates a status object wrapping a {@link ProgressListener} */
    public StatusMonitor(ProgressListener listener) {
        super();
        this.listener = listener;
    }

    /** Returns the wrapped listener */
    public ProgressListener getListener() {
        return listener;
    }

    /** Sets the global progress, between 0 and 100 */
    public void setProgress(float progress) {
        listener.progress(progress);
    }

    /** Sets the current task */
    public void setTask(String status) {
        listener.setTask(Text.text(status));
    }

    /**
     * Starts a sub-task that will take "total" percentage. Call "work" to report progress within
     * the sub-task
     */
    public void start(String status, int total) {
        listener.setTask(Text.text(status));
        this.base = listener.getProgress();
        this.total = total;
        this.work = 0;
    }

    /** Reports progress within the current sub-task, with a number between 0 and 100 */
    public void work(int worked) {
        work += worked;
        listener.progress(base + work / total);
    }

    /** Throws a WPS exception, specifying a message */
    public void throwException(String message) {
        throw new WPSException(message);
    }

    /**
     * Throws a WPS exception, specifying a message and the code (see OWS spec for exception
     * reporting details)
     */
    public void throwException(String message, String code) {
        throw new WPSException(code, message);
    }

    /**
     * Throws a WPS exception, specifying a message and the code and locator (see OWS spec for
     * exception reporting details)
     */
    public void throwException(String message, String code, String locator) {
        throw new WPSException(message, code, locator);
    }
}
