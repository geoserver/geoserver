/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.ResponseDocumentType;

import org.geoserver.wps.executor.ExecuteResponseBuilder;
import org.geoserver.wps.executor.ExecuteRequest;
import org.geoserver.wps.executor.WPSExecutionManager;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;

/**
 * Main class used to handle Execute requests
 * 
 * @author Lucas Reed, Refractions Research Inc
 * @author Andrea Aime, OpenGeo
 */
public class Execute {

    static final Logger LOGGER = Logging.getLogger(Execute.class);

    int connectionTimeout;

    WPSInfo wps;

    ApplicationContext context;

    WPSExecutionManager executionManager;

    public Execute(WPSExecutionManager executionManager, ApplicationContext context) {
        this.context = context;
        this.executionManager = executionManager;
    }

    /**
     * Main method for performing decoding, execution, and response
     * 
     * @param object
     * @param output
     * @throws IllegalArgumentException
     */
    public ExecuteResponseType run(ExecuteType execute) {
        ResponseDocumentType responseDocument = null;
        OutputDefinitionType rawDataOutput = null;
        if (execute.getResponseForm() != null) {
            responseDocument = execute.getResponseForm().getResponseDocument();
            rawDataOutput = execute.getResponseForm().getRawDataOutput();
        }

        if (responseDocument != null && rawDataOutput != null) {
            throw new WPSException("Invalid request, only one of the raw data output or the "
                    + "response document should be specified in the request");
        }

        ExecuteRequest request = new ExecuteRequest(execute);

        // TODO: get the startup time from the execution status
        ExecuteResponseBuilder builder = new ExecuteResponseBuilder(execute, context, new Date());
        String executionId = executionManager.submit(request, !request.isAsynchronous());
        builder.setExecutionId(executionId);
        if (!request.isAsynchronous()) {
            try {
                Map<String, Object> outputs = executionManager.getOutput(executionId, -1);
                builder.setOutputs(outputs);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Process execution failed", e);
                builder.setException(e);
            }
        }

        return builder.build();
    }

}
