/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.geoserver.filters.GeoServerFilter;

public class OSEOFilter implements GeoServerFilter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest requestHTTP = (HttpServletRequest) request;
            if (requestNeedsWrapper(requestHTTP)) {
                request = new RequestWrapper(requestHTTP);
            }
        }
        chain.doFilter(request, response);
    }

    private boolean requestNeedsWrapper(HttpServletRequest requestHTTP) {
        String pathInfo = requestHTTP.getPathInfo();
        Map<String, String[]> parameterMap = requestHTTP.getParameterMap();
        return pathInfo != null
                && parameterMap != null
                && "GET".equalsIgnoreCase(requestHTTP.getMethod())
                && !(new CaseInsensitiveMap(parameterMap).containsKey("service"))
                && (pathInfo.endsWith("search") || pathInfo.endsWith("description"));
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    private static class RequestWrapper extends HttpServletRequestWrapper {
        private String request;

        private RequestWrapper(HttpServletRequest wrapped) {
            super(wrapped);
            if (wrapped.getPathInfo().endsWith("search")) {
                request = "search";
            } else {
                request = "description";
            }
        }

        @Override
        public Enumeration getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            Map filtered = new HashMap<String, String[]>(original);
            filtered.put("service", "OSEO");
            filtered.put("version", "1.0.0");
            filtered.put("request", request);
            return filtered;
        }

        @Override
        public String[] getParameterValues(String name) {
            if ("service".equalsIgnoreCase(name)) {
                return new String[] {"OSEO"};
            }
            if ("request".equalsIgnoreCase(name)) {
                return new String[] {request};
            }
            return super.getParameterValues(name);
        }

        @Override
        public String getParameter(String name) {
            if ("service".equalsIgnoreCase(name)) {
                return "OSEO";
            }
            if ("request".equalsIgnoreCase(name)) {
                return request;
            }
            return super.getParameter(name);
        }
    }
}
