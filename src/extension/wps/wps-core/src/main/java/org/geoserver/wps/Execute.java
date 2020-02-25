/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.logging.Logger;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.ResponseDocumentType;
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

    /** Main method for performing decoding, execution, and response */
    public ExecuteResponseType run(ExecuteType execute) {
        ResponseDocumentType responseDocument = null;
        OutputDefinitionType rawDataOutput = null;
        if (execute.getResponseForm() != null) {
            responseDocument = execute.getResponseForm().getResponseDocument();
            rawDataOutput = execute.getResponseForm().getRawDataOutput();
        }

        if (responseDocument != null && rawDataOutput != null) {
            throw new WPSException(
                    "Invalid request, only one of the raw data output or the "
                            + "response document should be specified in the request");
        }

        ExecuteRequest request = new ExecuteRequest(execute);

        ExecuteResponseType response = executionManager.submit(request, !request.isAsynchronous());
        return response;
    }
}
