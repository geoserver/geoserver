/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.File;

import net.opengis.wps10.ExecuteResponseType;

import org.geoserver.wps.executor.WPSExecutionManager;

public class GetStatus {

    private WPSExecutionManager executionManager;

    public GetStatus(WPSExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    public Object run(GetExecutionStatusType request) {
        // see if the process is still in-flight
        ExecuteResponseType status = executionManager.getStatus(request.getExecutionId());
        if (status != null) {
            return status;
        }
        
        // otherwise check for a stored response
        File storedResponse = executionManager.getStoredResponse(request.getExecutionId());
        if (storedResponse == null || !storedResponse.exists()) {
            throw new WPSException("Unknown execution id " + request.getExecutionId()
                    + ", either the execution was never submitted or too much time "
                    + "elapsed since the process completed");
        }
        return storedResponse;
    }

}
