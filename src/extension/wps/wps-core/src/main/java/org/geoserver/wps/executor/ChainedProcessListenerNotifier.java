/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.List;
import org.geoserver.wps.ChainedProcessListener;
import org.geoserver.wps.WPSException;

/**
 * A notifier to fire events when processes are submitted and completed. Events are fired for both
 * root process and internal chained processes.
 *
 * @author etj (Emanuele Tajariol @ GeoSolutions)
 */
class ChainedProcessListenerNotifier {

    private List<ChainedProcessListener> listeners;
    private String executionId;
    private String processName;
    private boolean chained;

    public ChainedProcessListenerNotifier(
            String executionId,
            String processName,
            boolean chained,
            List<ChainedProcessListener> listeners) {
        this.listeners = listeners;
        this.executionId = executionId;
        this.processName = processName;
        this.chained = chained;
    }

    /** Called right before the process is submitted into the {@link ProcessManager} */
    public void fireStarted() throws WPSException {
        for (ChainedProcessListener listener : listeners) {
            listener.started(executionId, processName, chained);
        }
    }

    /** Called when a process returns successfully. */
    public void fireCompleted() throws WPSException {
        for (ChainedProcessListener listener : listeners) {
            listener.completed(executionId, processName);
        }
    }

    /** Called when a process is stopped by a dismission. */
    public void fireDismissed() throws WPSException {
        for (ChainedProcessListener listener : listeners) {
            listener.dismissed(executionId, processName);
        }
    }

    /** Called when a process is stopped by an exception. */
    public void fireFailed(Exception e) throws WPSException {
        for (ChainedProcessListener listener : listeners) {
            listener.failed(executionId, processName, e);
        }
    }
}
