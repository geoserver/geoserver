/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import org.opengis.feature.type.Name;

/**
 * Summarizes the execution state of a certain process
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class ExecutionStatus {

    public enum ProcessState {
        QUEUED, RUNNING, COMPLETED, CANCELLED
    };

    /**
     * The process being executed
     */
    Name processName;

    /**
     * The execution id, can be used to retrieve the process results
     */
    String executionId;

    /**
     * Current execution status
     */
    ProcessState phase;

    /**
     * Process execution status (as a percentage between 0 and 100)
     */
    float progress;
    
    public ExecutionStatus(Name processName, String executionId, ProcessState phase, float progress) {
        this.processName = processName;
        this.executionId = executionId;
        this.phase = phase;
        this.progress = progress;
    }

    public Name getProcessName() {
        return processName;
    }

    public String getExecutionId() {
        return executionId;
    }

    public ProcessState getPhase() {
        return phase;
    }

    /**
     * Returns the progress percentage, as a number between 0 and 100
     * @return
     */
    public float getProgress() {
        return progress;
    }

    public void setProcessName(Name processName) {
        this.processName = processName;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public void setPhase(ProcessState phase) {
        this.phase = phase;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

}
