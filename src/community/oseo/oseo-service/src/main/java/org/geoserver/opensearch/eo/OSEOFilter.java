/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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
        if (request instanceof HttpServletRequest requestHTTP) {
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
                && pathInfo.contains("oseo")
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
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            Map<String, String[]> filtered = new HashMap<>(original);
            filtered.put("service", new String[] {"OSEO"});
            filtered.put("version", new String[] {"1.0.0"});
            filtered.put("request", new String[] {request});
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
