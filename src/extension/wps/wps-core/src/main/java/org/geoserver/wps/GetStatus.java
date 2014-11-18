/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.IOException;

import net.opengis.wps10.ExecuteType;

import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wps.executor.ExecuteResponseBuilder;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.resource.WPSResourceManager;
import org.springframework.context.ApplicationContext;

public class GetStatus {

    private WPSResourceManager resources;

    private ProcessStatusTracker tracker;

    private ApplicationContext ctx;

    public GetStatus(ProcessStatusTracker tracker, WPSResourceManager resources, ApplicationContext ctx) {
        this.tracker = tracker;
        this.resources = resources;
        this.ctx = ctx;
    }

    public Object run(GetExecutionStatusType request) throws WPSException {
        // see if the process is still in-flight
        String executionId = request.getExecutionId();
        ExecutionStatus status = tracker.getStatus(executionId);
        if(status == null) {
            throw new WPSException("Unknown execution id " + executionId
                    + ", either the execution was never submitted or too much time "
                    + "elapsed since the process completed");
        }
        
        // are we done?
        if(status.getPhase().isExecutionCompleted()) {
            Resource storedResponse = resources.getStoredResponse(executionId);
            if (storedResponse == null || storedResponse.getType() == Type.UNDEFINED) {
                throw new WPSException("The execution is completed with status " + status.getPhase() 
                        + " and yet the response cannot be located on disk, this is an internal failure");
            } else {
                return storedResponse;
            }
        } else {
            try {
                ExecuteType execute = status.getRequest();
                if (execute == null) {
                    execute = resources.getStoredRequestObject(executionId);
                }
                if (execute == null) {
                    throw new WPSException(
                            "Could not locate the original request for execution id: "
                                    + executionId);
                } else {
                    ExecuteResponseBuilder builder = new ExecuteResponseBuilder(execute, ctx,
                            status);
                    return builder.build();
                }
            } catch (IOException e) {
                throw new WPSException("Failed to write status response", e);
            }
        }
        
        
    }

}
