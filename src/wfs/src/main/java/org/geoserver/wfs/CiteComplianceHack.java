/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


/**
 *
 *  Configures the dispatcher to be cite compliant based on wfs configuration.
 *  <p>
 *  TODO: Cite compliaance should be a server wide thing. This should be addressed
 *  when we ( if we ) refactor server configuration. When that happens this
 *  class can be retired.
 *  </p>
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class CiteComplianceHack implements HandlerInterceptor {
    
    GeoServer gs;
    
    public CiteComplianceHack(GeoServer gs ) {
        this.gs = gs;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
        Object handler) throws Exception {
        if (handler instanceof Dispatcher) {
            Dispatcher dispatcher = (Dispatcher) handler;
            dispatcher.setCiteCompliant(getInfo().isCiteCompliant());
        }

        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response,
        Object handler, ModelAndView modelAndView) throws Exception {
        // TODO Auto-generated method stub
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
        Object handler, Exception ex) throws Exception {
        //do nothing
    }
    
    WFSInfo getInfo() {
        return gs.getService( WFSInfo.class );
    }
    
}
