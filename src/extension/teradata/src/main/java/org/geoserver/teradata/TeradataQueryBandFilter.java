/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.teradata;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geotools.data.teradata.QueryBand;

public class TeradataQueryBandFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // set some thread local query band info
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            if (req.getRemoteUser() != null) {
                QueryBand.local().put(QueryBand.CLIENT_USER, req.getRemoteUser());
            }
            if (req.getRemoteHost() != null) {
                QueryBand.local().put(QueryBand.CLIENT_HOST, req.getRemoteHost());
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            QueryBand.remove();
        }
    }

    public void destroy() {}
}
