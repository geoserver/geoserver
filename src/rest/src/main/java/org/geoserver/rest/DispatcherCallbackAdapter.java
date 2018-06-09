/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** No-op implementation of DispatcherCallback. */
public class DispatcherCallbackAdapter implements DispatcherCallback {

    public void init(HttpServletRequest request, HttpServletResponse response) {}

    public void dispatched(
            HttpServletRequest request, HttpServletResponse response, Object handler) {}

    public void exception(
            HttpServletRequest request, HttpServletResponse response, Exception error) {}

    public void finished(HttpServletRequest request, HttpServletResponse response) {}
}
