/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Configures the dispatcher to be cite compliant based on the specified service configuration.
 *
 * <p>TODO: Cite compliance should be a server wide thing. This should be addressed when we ( if we
 * ) refactor server configuration. When that happens this class can be retired.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class CiteComplianceHack implements HandlerInterceptor {

    GeoServer gs;
    Class serviceClass;

    public CiteComplianceHack(GeoServer gs, Class serviceClass) {
        this.gs = gs;
        this.serviceClass = serviceClass;
    }

    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof Dispatcher) {
            Dispatcher dispatcher = (Dispatcher) handler;
            dispatcher.setCiteCompliant(getInfo().isCiteCompliant());
        }

        return true;
    }

    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView)
            throws Exception {
        // do nothing
    }

    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // do nothing
    }

    ServiceInfo getInfo() {
        return gs.getService(serviceClass);
    }
}
