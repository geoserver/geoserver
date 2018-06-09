/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * {@link PriorityProvider} picking the request priority from a configured HTTP header, if found, or
 * returning the default priority otherwise. The header must contain a integer value (the priority
 * itself)
 */
public class HttpHeaderPriorityProvider implements PriorityProvider {

    static final Logger LOGGER = Logging.getLogger(HttpHeaderPriorityProvider.class);

    private final String headerName;
    private final int defaultPriority;

    public HttpHeaderPriorityProvider(String headerName, int defaultPriority) {
        this.headerName = headerName;
        this.defaultPriority = defaultPriority;
    }

    @Override
    public int getPriority(Request request) {
        if (request != null
                && request.getHttpRequest() != null
                && request.getHttpRequest().getHeader(headerName) != null) {
            String priorityString = request.getHttpRequest().getHeader(headerName);
            try {
                int priority = Integer.parseInt(priorityString);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Found priority header "
                                    + headerName
                                    + " in request with value "
                                    + priority);
                }
                return priority;
            } catch (NumberFormatException e) {
                LOGGER.log(
                        Level.INFO,
                        "Priority header found, but did not have a valid integer value",
                        e);
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Did not find priority header "
                                + headerName
                                + " in request, using default priorirty");
            }
        }

        return defaultPriority;
    }

    /**
     * The header name holding the priority value
     *
     * @return A HTTP header name
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * The default priority value, in case the header does not return a priority value
     *
     * @return a default priority value
     */
    public int getDefaultPriority() {
        return defaultPriority;
    }
}
