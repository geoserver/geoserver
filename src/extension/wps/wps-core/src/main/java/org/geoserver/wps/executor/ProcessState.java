/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** The various states in which the execution of a process can find itself into */
public enum ProcessState {
    /** Queued for execution, not yet started */
    QUEUED(false),
    /** Execution undergoing */
    RUNNING(false, QUEUED),
    /** Execution completed successfully, (full output encoding included) */
    SUCCEEDED(true, RUNNING),

    /**
     * The process is being cancelled by the original user that requested the process, or the admin
     */
    DISMISSING(false, QUEUED, RUNNING),

    /** The process failed during execution/output encoding */
    FAILED(true, QUEUED, RUNNING, DISMISSING);

    private boolean executionCompleted;

    private Set<ProcessState> predecessors;

    ProcessState(boolean completed, ProcessState... predecessors) {
        this.executionCompleted = completed;
        this.predecessors = new HashSet<>(Arrays.asList(predecessors));
    }

    /**
     * True if this state represents a process whose execution is completed (either succesfully, or
     * not)
     */
    public boolean isExecutionCompleted() {
        return executionCompleted;
    }

    /** Checks if a certain state comes before this one in the status workflow */
    public boolean isValidSuccessor(ProcessState predecessor) {
        return predecessor == this || predecessors.contains(predecessor);
    }
}
