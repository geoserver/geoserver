/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/** Configures the dispatcher to log XML Post requests with configurable size */
public class XmlPostRequestLogBufferSize implements HandlerInterceptor {

    GeoServer gs;

    public XmlPostRequestLogBufferSize(GeoServer gs) {
        this.gs = gs;
    }

    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof Dispatcher) {
            Dispatcher dispatcher = (Dispatcher) handler;
            Integer xmlLogBufferSize =
                    getInfo().getGeoServer().getGlobal().getXmlPostRequestLogBufferSize();
            if (xmlLogBufferSize != null) {
                dispatcher.setXMLPostRequestLogBufferSize(xmlLogBufferSize);
            }
        }

        return true;
    }

    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView)
            throws Exception {
        // TODO Auto-generated method stub
    }

    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // do nothing
    }

    WFSInfo getInfo() {
        return gs.getService(WFSInfo.class);
    }
}
