/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import java.util.Map;
import org.geoserver.taskmanager.data.Task;

/**
 * Task Context used during batch run or task clean-up.
 *
 * @author Niels Charlier
 */
public interface TaskContext {

    /** @return the task */
    Task getTask();

    /** @return the batch context, null if this is a clean-up */
    BatchContext getBatchContext();

    /** @return the parameter values, lazy loaded from task and configuration. */
    Map<String, Object> getParameterValues() throws TaskException;

    /**
     * Tasks can call this function to check if the user wants to interrupt the batch and interrupt
     * themselves. If they do, they should still return a TaskResult that implements a roll back of
     * what was already done.
     *
     * @return whether the batch run should be interrupted, false if this is a clean-up
     */
    boolean isInterruptMe();
}
