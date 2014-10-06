/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.File;

import org.geoserver.wps.executor.WPSExecutionManager;

public class GetResult {

    private WPSExecutionManager executionManager;

    public GetResult(WPSExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    public File run(GetExecutionResultType request) {
        // otherwise check for a stored response
        File outputFile = executionManager.getStoredOutput(request.getExecutionId(),
                request.getOutputId());
        if (outputFile == null || !outputFile.exists()) {
            throw new WPSException("Unknown output " + request.getOutputId() + " for execution id "
                    + request.getExecutionId()
                    + ", either the execution was never submitted or too much time "
                    + "elapsed since the process completed");
        }
        return outputFile;
    }

}
