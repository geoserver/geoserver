/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/** Interceptor notifying {@link DispatcherCallback} of request processing progress */
public class CallbackInterceptor extends HandlerInterceptorAdapter {

    static final Logger LOGGER = Logging.getLogger(CallbackInterceptor.class);

    List<DispatcherCallback> getCallbacks() {
        return GeoServerExtensions.extensions(DispatcherCallback.class);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        List<DispatcherCallback> callbacks = getCallbacks();
        // for semi-backwards compatibility
        for (DispatcherCallback callback : callbacks) {
            callback.init(request, response);
        }
        // the real thing
        for (DispatcherCallback callback : callbacks) {
            callback.dispatched(request, response, handler);
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        List<DispatcherCallback> callbacks = getCallbacks();

        if (ex != null) {
            for (DispatcherCallback callback : callbacks) {
                callback.exception(request, response, ex);
            }
        }

        // ensure that finish is always called for all requests
        for (DispatcherCallback callback : callbacks) {
            try {
                callback.finished(request, response);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Callback threw exception on finish", e);
            }
        }
    }
}
