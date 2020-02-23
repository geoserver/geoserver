/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.animate;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.filters.GeoServerFilter;
import org.geotools.util.logging.Logging;

/**
 * GIF Animated reflecting service request filter.
 *
 * <p>Modifies requests against the WMS animate reflector service endpoints in order to address <a
 * href="https://osgeo-org.atlassian.net/browse/GEOS-6006">GEOS-6006</a>
 *
 * @author Tom Kunicki, Boundless
 */
public class AnimatorFilter implements GeoServerFilter {

    private static final Logger LOGGER = Logging.getLogger(AnimatorFilter.class);

    private static final String ENDPOINT = "animate";
    private static final String REQUEST = "Request";
    private static final String GETMAP = "GetMap";

    /** */
    @Override
    public void init(FilterConfig config) throws ServletException {
        // nothing to do
    }

    /**
     * Removes KVP argument <code>Request=GetMap</code> <i>(case independent)</i> if present for
     * calls against <code>.../animate</code> service endpoints.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param chain currently executing filter chain
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest requestHTTP = (HttpServletRequest) request;
            if (requestNeedsWrapper(requestHTTP)) {
                LOGGER.log(
                        Level.FINER,
                        "Modified request to {0}, removed \"Request\" KVP argument (GEOS-6006)",
                        requestHTTP.getRequestURI());
                request = new RequestWrapper(requestHTTP);
            }
        }
        chain.doFilter(request, response);
    }

    /** */
    @Override
    public void destroy() {
        // nothing to do...
    }

    private boolean requestNeedsWrapper(HttpServletRequest request) {
        if (request.getRequestURI().endsWith(ENDPOINT)) {
            Enumeration<String> names = request.getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                if (REQUEST.equalsIgnoreCase(name)
                        && GETMAP.equalsIgnoreCase(request.getParameter(name))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class RequestWrapper extends HttpServletRequestWrapper {
        private RequestWrapper(HttpServletRequest wrapped) {
            super(wrapped);
        }

        @Override
        public Enumeration getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            Map filtered = new HashMap<String, String[]>();
            for (Map.Entry<String, String[]> entry : original.entrySet()) {
                String key = entry.getKey();
                if (!REQUEST.equalsIgnoreCase(key)) {
                    filtered.put(key, entry.getValue());
                }
            }
            return filtered;
        }

        @Override
        public String[] getParameterValues(String name) {
            if (REQUEST.equalsIgnoreCase(name)) {
                return null;
            }
            return super.getParameterValues(name);
        }

        @Override
        public String getParameter(String name) {
            if (REQUEST.equalsIgnoreCase(name)) {
                return null;
            }
            return super.getParameter(name);
        }
    }
}
