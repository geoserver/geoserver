/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geoserver.wps.executor.ProcessManager;

/**
 * An interface to monitor a process execution. The methods will be called by different threads when
 * working on asynchronous processes, so the usage of thread locals to keep state about an execution
 * is discouraged,
 */
public interface ProcessListener {

    /** Called right before the process is submitted into the {@link ProcessManager} */
    void submitted(ProcessEvent event) throws WPSException;

    /**
     * Reports progress of the process. Not to be confused with the Java based process
     * implementation progress tracking, this also includes input parsing and output encoding void
     * progress(ProcessEvent event) throws WPSException;
     */
    void progress(ProcessEvent event) throws WPSException;

    /**
     * Called when the process successfully executed and the output is successfully written out to
     * the caller (or stored on disk, for asynchronous calls)
     */
    void succeeded(ProcessEvent event) throws WPSException;

    /** Called when the process is getting dismissed by the client/administrator */
    void dismissing(ProcessEvent event) throws WPSException;

    /** Notifies dismissal completion */
    void dismissed(ProcessEvent event) throws WPSException;

    /**
     * Called when the process failed to execute. This method should not throw further exceptions.
     */
    void failed(ProcessEvent event);
}
