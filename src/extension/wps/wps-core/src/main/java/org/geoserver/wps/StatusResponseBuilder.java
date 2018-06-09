/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.io.IOException;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import org.geoserver.wps.executor.ExecuteResponseBuilder;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.resource.WPSResourceManager;
import org.springframework.context.ApplicationContext;

/**
 * Helper building a {@link ExecuteResponseType} for a given {@link ExecutionStatus}
 *
 * @author Andrea Aime - GeoSolutions
 */
class StatusResponseBuilder {

    private WPSResourceManager resources;

    private ApplicationContext ctx;

    public StatusResponseBuilder(WPSResourceManager resources, ApplicationContext ctx) {
        this.resources = resources;
        this.ctx = ctx;
    }

    ExecuteResponseType buildStatusResponse(ExecutionStatus status) {
        try {
            ExecuteType execute = status.getRequest();
            if (execute == null) {
                execute = resources.getStoredRequestObject(status.getExecutionId());
            }
            if (execute == null) {
                throw new WPSException(
                        "Could not locate the original request for execution id: "
                                + status.getExecutionId());
            } else {
                ExecuteResponseBuilder builder = new ExecuteResponseBuilder(execute, ctx, status);
                return builder.build();
            }
        } catch (IOException e) {
            throw new WPSException("Failed to write status response", e);
        }
    }
}
