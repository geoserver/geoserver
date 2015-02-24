/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.geotools.util.logging.Logging;
import org.restlet.Restlet;
import org.restlet.data.Status;

import com.noelios.restlet.ext.servlet.ServletConverter;
import com.noelios.restlet.http.HttpResponse;

/**
 * A custom servlet controller that forces the logging level of errors to SEVERE.
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class GeoServerServletConverter extends ServletConverter {

    static final Logger LOGGER = Logging.getLogger(GeoServerServletConverter.class);

    public GeoServerServletConverter(ServletContext context, Restlet target) {
        super(context, target);
    }

    public GeoServerServletConverter(ServletContext context) {
        super(context);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    /**
     * Overridden to get at the very least a log at severe level
     * 
     * @param response
     */
    @Override
    public void commit(HttpResponse response) {
        try {
            // Add the response headers
            addResponseHeaders(response);

            // Send the response to the client
            response.getHttpCall().sendResponse(response);
        } catch (Exception e) {
            // raise the logging level to SEVERE
            LOGGER.log(Level.SEVERE, "Exception intercepted", e);
            response.getHttpCall().setStatusCode(Status.SERVER_ERROR_INTERNAL.getCode());
            response.getHttpCall().setReasonPhrase("An unexpected exception occured");
        }
    }

}
