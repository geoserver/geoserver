/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;

/** Provides callbacks for the life cycle of a rest request. */
public interface DispatcherCallback {

    /** Called at the start of a request cycle. */
    void init(HttpServletRequest request, HttpServletResponse response);

    /** Called once a handler has been located. */
    void dispatched(HttpServletRequest request, HttpServletResponse response, Object handler);

    /** Called in the event of an exception occurring during a request. */
    void exception(HttpServletRequest request, HttpServletResponse response, Exception error);

    /**
     * Final callback called once a request has been completed.
     *
     * <p>This method is always called, even in the event of an exception during request processing.
     */
    void finished(HttpServletRequest request, HttpServletResponse response);

    /**
     * Attempts to unwrap the Controller in case the handler is annotation driven, returns the
     * handler otherwise
     */
    static Object getControllerBean(Object handler) {
        if (handler instanceof HandlerMethod) {
            return ((HandlerMethod) handler).getBean();
        }
        return handler;
    }
}
