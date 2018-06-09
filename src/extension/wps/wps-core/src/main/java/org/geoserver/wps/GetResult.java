/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wps.resource.WPSResourceManager;

public class GetResult {

    private WPSResourceManager resourceManager;

    public GetResult(WPSResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public Resource run(GetExecutionResultType request) {
        // otherwise check for a stored response
        Resource output =
                resourceManager.getOutputResource(request.getExecutionId(), request.getOutputId());
        if (output == null || output.getType() == Type.UNDEFINED) {
            throw new WPSException(
                    "Unknown output "
                            + request.getOutputId()
                            + " for execution id "
                            + request.getExecutionId()
                            + ", either the execution was never submitted or too much time "
                            + "elapsed since the process completed");
        }
        return output;
    }
}
