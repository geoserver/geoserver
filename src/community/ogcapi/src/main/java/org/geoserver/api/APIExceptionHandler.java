/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

package org.geoserver.api;

import javax.servlet.http.HttpServletResponse;

public interface APIExceptionHandler {

    public boolean canHandle(Throwable t, RequestInfo request);

    public void handle(Throwable t, HttpServletResponse response);
}
