/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.security.config.SSLFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Servlet filter redirecting HTTP requests to HTTPS requests
 *
 * @author mcr
 */
public class GeoServerSSLFilter extends GeoServerSecurityFilter {

    protected Integer sslPort;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        SSLFilterConfig sslConfig = (SSLFilterConfig) config;

        sslPort = sslConfig.getSslPort();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request.isSecure()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        StringBuffer buff = new StringBuffer("https://");
        buff.append(httpRequest.getServerName())
                .append(":")
                .append(sslPort)
                .append(httpRequest.getContextPath())
                .append(httpRequest.getServletPath());

        Map<String, String> kvp = new HashMap<String, String>();
        if (httpRequest.getQueryString() != null) {
            for (String kvpString : httpRequest.getQueryString().split("&")) {
                String[] kvpArray = kvpString.split("=");
                if (kvpArray == null || kvpArray.length != 2) {
                    LOGGER.warning("Unknown query parameter: " + kvpString);
                    continue;
                }
                kvp.put(kvpArray[0], kvpArray[1]);
            }
        }
        String redirectURL =
                ResponseUtils.buildURL(buff.toString(), httpRequest.getPathInfo(), kvp, null);

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("Redirecting " + httpRequest.getRequestURL() + " to " + redirectURL);
        ((HttpServletResponse) response).sendRedirect(redirectURL);
    }
}
