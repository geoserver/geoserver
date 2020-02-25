/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import javax.servlet.http.HttpServletResponse;

/**
 * A class that handles exceptions caught by {@link APIDispatcher}. Only the first handler able to
 * process the request will handle it. A {@link DefaultAPIExceptionHandler} is provided as a catch
 * all, lowest priority (see {@link org.geoserver.platform.ExtensionPriority} one that simply
 * encodes the exception in JSON
 */
public interface APIExceptionHandler {

    /**
     * Returns true if the handler can handle this exception in the context of the current request
     *
     * @param t The exception
     * @param request The current request
     */
    public boolean canHandle(Throwable t, APIRequestInfo request);

    /**
     * Invoked to handle the exception (to deal with it encoding a response, or do whatever else
     *
     * @param t The exception
     * @param response The HTTP response
     */
    public void handle(Throwable t, HttpServletResponse response);
}
