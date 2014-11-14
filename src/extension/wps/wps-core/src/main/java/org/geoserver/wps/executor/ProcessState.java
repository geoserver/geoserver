package org.geoserver.wps.executor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The various states in which the execution of a process can find itself into
 */
public enum ProcessState {
    /**
     * Queued for execution, not yet started
     */
    QUEUED(false),
    /**
     * Execution undergoing
     */
    RUNNING(false, QUEUED),
    /**
     * Execution completed successfully, (full output encoding included)
     */
    SUCCEEDED(true, RUNNING),
    /**
     * The process has been cancelled by the origina user or the admin
     */
    CANCELLED(true, QUEUED, RUNNING),
    /**
     * The process failed during execution/output encoding
     */
    FAILED(true, QUEUED, RUNNING);

    private boolean executionCompleted;

    private Set<ProcessState> predecessors;

    ProcessState(boolean completed, ProcessState... predecessors) {
        this.executionCompleted = completed;
        this.predecessors = new HashSet<>(Arrays.asList(predecessors));
    }

    /**
     * True if this state represents a process whose execution is completed (either succesfully, or
     * not)
     * 
     * @return
     */
    public boolean isExecutionCompleted() {
        return executionCompleted;
    }

    /**
     * Checks if a certain state comes before this one in the status workflow
     * 
     * @param predecessor
     * @return
     */
    public boolean isValidSuccessor(ProcessState predecessor) {
        return predecessors.contains(predecessor);
    }
}