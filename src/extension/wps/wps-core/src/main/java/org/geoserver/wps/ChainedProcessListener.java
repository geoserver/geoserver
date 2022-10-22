/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geoserver.wps.executor.ProcessManager;

/**
 * An interface to monitor how processes are chained.
 *
 * @author etj (Emanuele Tajariol @ GeoSolutions)
 */
public interface ChainedProcessListener {

    /**
     * Called right before the process is submitted into the {@link ProcessManager}. It's called
     * both for root process and for all internal chained processes.
     */
    void started(String executionId, String processName, boolean chained);

    /** Called when a process returns successfully. */
    void completed(String executionId, String processName);

    /** Called when a process is stopped by dismission. */
    void dismissed(String executionId, String processName);

    /** Called when a process is stopped by an exception. */
    void failed(String executionId, String processName, Exception e);
}
