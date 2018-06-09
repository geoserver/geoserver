/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import net.opengis.wps10.ExecuteResponseType;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.executor.WPSExecutionManager;
import org.geoserver.wps.resource.WPSResourceManager;
import org.springframework.context.ApplicationContext;

/**
 * Cancels the execution of a given process, indentified by executionid
 *
 * @author Andrea Aime - GeoSolutions
 */
public class Dismiss {

    /** The object tracking the status of various processes */
    private ProcessStatusTracker statusTracker;

    /** The resource tracker, we use it to build the responses */
    private WPSResourceManager resources;

    /** Used by the response builder */
    private ApplicationContext ctx;

    /** Used to cancel the progress of a certain process */
    private WPSExecutionManager executionManager;

    public Dismiss(
            WPSExecutionManager executionManager,
            ProcessStatusTracker statusTracker,
            WPSResourceManager resources,
            ApplicationContext ctx) {
        this.executionManager = executionManager;
        this.statusTracker = statusTracker;
        this.resources = resources;
        this.ctx = ctx;
    }

    public ExecuteResponseType run(DismissType request) {
        // See if the process is still in-flight
        String executionId = request.getExecutionId();
        ExecutionStatus status = statusTracker.getStatus(executionId);
        if (status == null) {
            throw new UnknownExecutionIdException(executionId);
        }

        if (status.getPhase() == ProcessState.DISMISSING) {
            // pretend we don't know the process, it has already been cancelled, the spec
            // says that once cancelled we have to act as if the process never existed
            throw new UnknownExecutionIdException(executionId);
        }

        // actually cancel the execution
        executionManager.cancel(executionId);

        // build an appropriate response
        ExecutionStatus cancelledStatus = new ExecutionStatus(status);
        cancelledStatus.setPhase(ProcessState.FAILED);
        cancelledStatus.setException(new WPSException("The process execution has been dismissed"));
        return new StatusResponseBuilder(resources, ctx).buildStatusResponse(cancelledStatus);
    }
}
