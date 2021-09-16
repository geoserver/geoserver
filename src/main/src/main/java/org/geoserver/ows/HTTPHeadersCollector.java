/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.ows.util.CaseInsensitiveMap;

/**
 * Collects headers on behalf of {@link ProxifyingURLMangler}, so that they can be used also for
 * asynchronoous executions happening outside of request threads. Given the specific usage, only the
 * first value of headers is collected.
 */
public class HTTPHeadersCollector implements GeoServerFilter {

    public static final ThreadLocal<Map<String, String>> HEADERS = new ThreadLocal<>();

    /**
     * Returns the value for the specified header, if the {@link #HEADERS} thread local is loaded,
     * and contains one, null otherwise.
     */
    public static String getHeader(String header) {
        Map<String, String> headers = HEADERS.get();
        if (headers == null) return null;
        return headers.get(header);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            collectHeaders((HttpServletRequest) request);

            chain.doFilter(request, response);
        } finally {
            HEADERS.remove();
        }
    }

    @SuppressWarnings("unchecked")
    public void collectHeaders(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        Map<String, String> headers = new CaseInsensitiveMap(new HashMap<>());
        while (names.hasMoreElements()) {
            String header = names.nextElement();
            String value = request.getHeader(header);
            headers.put(header, value);
        }
        HEADERS.set(headers);
    }

    @Override
    public void destroy() {}
}
