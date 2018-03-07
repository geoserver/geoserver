/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.geoserver.filters.GeoServerFilter;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple hack to bridge part of the path based approach in WFS 3 to traditional OWS mappings.
 * If this is somehow generalized and brought into the main dispatcher, check OSEOFilter as well (in the OpenSearch 
 * module)
 */
public class WFS3Filter implements GeoServerFilter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest requestHTTP = (HttpServletRequest)request;
            if (requestNeedsWrapper(requestHTTP)) {
                request = new RequestWrapper(requestHTTP);
            }
        }
        chain.doFilter(request, response);

    }

    private boolean requestNeedsWrapper(HttpServletRequest requestHTTP) {
        String path = requestHTTP.getServletPath();
        return path.contains("wfs3");
    }

    @Override
    public void destroy() {
        // nothing to do
    }
    
    private static class RequestWrapper extends HttpServletRequestWrapper {        
        private String request;

        private RequestWrapper(HttpServletRequest wrapped) {
            super(wrapped);
            String pathInfo = wrapped.getPathInfo();
            if(pathInfo.endsWith("api")) {
                request = "api";
            } else if(pathInfo.endsWith("api/conformance")) {
                request = "conformance";
            }
        }

        @Override
        public Enumeration getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public Map<String,String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            Map filtered = new HashMap<>(original);
            filtered.put("service", "WFS");
            filtered.put("version", "3.0.0");
            filtered.put("request", request);
            return filtered;
        }

        @Override
        public String[] getParameterValues(String name) {
            if ("service".equalsIgnoreCase(name)) {
                return new String[] {"WFS"};
            }
            if ("request".equalsIgnoreCase(name)) {
                return new String[] {request};
            }
            return super.getParameterValues(name);
        }

        @Override
        public String getParameter(String name) {
            if ("service".equalsIgnoreCase(name)) {
                return "WFS";
            }
            if ("request".equalsIgnoreCase(name)) {
                return request;
            }
            return super.getParameter(name);
        }
    }

}
