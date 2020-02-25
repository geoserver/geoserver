/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.resource.WPSResourceManager;
import org.springframework.context.ApplicationContext;

/**
 * Runs the GetStatus pseudo WPS request (GeoServer uses it to implement the status url)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GetStatus {

    private WPSResourceManager resources;

    private ProcessStatusTracker tracker;

    private ApplicationContext ctx;

    public GetStatus(
            ProcessStatusTracker tracker, WPSResourceManager resources, ApplicationContext ctx) {
        this.tracker = tracker;
        this.resources = resources;
        this.ctx = ctx;
    }

    public Object run(GetExecutionStatusType request) throws WPSException {
        // see if the process is still in-flight
        String executionId = request.getExecutionId();
        ExecutionStatus status = tracker.getStatus(executionId);
        if (status == null) {
            throw new UnknownExecutionIdException(executionId);
        }

        // are we done?
        if (status.getPhase().isExecutionCompleted()) {
            Resource storedResponse = resources.getStoredResponse(executionId);
            if (storedResponse == null || storedResponse.getType() == Type.UNDEFINED) {
                throw new WPSException(
                        "The execution is completed with status "
                                + status.getPhase()
                                + " and yet the response cannot be located on disk, this is an internal failure");
            } else {
                return storedResponse;
            }
        } else if (status.getPhase() == ProcessState.DISMISSING) {
            // in case of dismissal we have to pretend we don't know the execution id
            throw new UnknownExecutionIdException(executionId);
        } else {
            return new StatusResponseBuilder(resources, ctx).buildStatusResponse(status);
        }
    }
}
