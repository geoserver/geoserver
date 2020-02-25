/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import org.geoserver.security.filter.GeoServerSecurityContextPersistenceFilter;
import org.geotools.util.logging.Logging;

/**
 * Utility filter that will dump a stack trace identifying any session creation outside of the user
 * interface (OGC and REST services are supposed to be stateless, session creation is harmful to
 * scalability)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SessionDebugFilter implements Filter {

    static final Logger LOGGER = Logging.getLogger(SessionDebugWrapper.class);

    public void destroy() {
        // nothing to do
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;
            chain.doFilter(new SessionDebugWrapper(request), res);
        } else {
            chain.doFilter(req, res);
        }
    }

    /**
     * {@link HttpServletRequest} wrapper that will dump a full trace for any session creation
     * attempt
     *
     * @author Andrea Aime - GeoSolutions
     */
    class SessionDebugWrapper extends HttpServletRequestWrapper {

        public SessionDebugWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public HttpSession getSession() {
            return this.getSession(true);
        }

        @Override
        public HttpSession getSession(boolean create) {
            // first off, try to grab an existing session
            HttpSession session = super.getSession(false);

            if (session != null || !create) {
                return session;
            }

            // ok, no session but the caller really wants one,
            // check for the hint passed by the GeoServerSecurityContextPersistenceFilter and
            // signal the issue in the logs

            Boolean allow =
                    (Boolean)
                            getAttribute(
                                    GeoServerSecurityContextPersistenceFilter
                                            .ALLOWSESSIONCREATION_ATTR);

            // are we creating the session in the web ui?
            if (getPathInfo().startsWith("/web") || Boolean.TRUE.equals(allow)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    Exception e = new Exception("Full stack trace for the session creation path");
                    e.fillInStackTrace();
                    LOGGER.log(
                            Level.FINE,
                            "Creating a new http session inside the web UI (normal behavior)",
                            e);
                }
            } else {
                if (LOGGER.isLoggable(Level.INFO)) {
                    Exception e = new Exception("Full stack trace for the session creation path");
                    e.fillInStackTrace();
                    LOGGER.log(
                            Level.INFO,
                            "Creating a new http session outside of the web UI! "
                                    + "(normally not desirable), the path is"
                                    + getPathInfo(),
                            e);
                }
            }

            // return the session
            session = super.getSession(true);
            return session;
        }
    }
}
