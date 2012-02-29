/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.filters.GeoServerFilter;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * A class that allows the configuration of an ip black list, rejecting requests from ip addresses configured in the controlflow.properties file
 * 
 * @author Juan Marin, OpenGeo
 * 
 */

public class IpBlacklistFilter implements GeoServerFilter {

    static final Logger LOGGER = Logging.getLogger(IpBlacklistFilter.class);

    private List<String> ipAddresses = new ArrayList<String>();

    private boolean isBlocked;

    /**
     * Constructor used for testing purposes
     * 
     * @param props
     */
    public IpBlacklistFilter(Properties props) {
        configureIpBlackList(props);
    }

    /**
     * Default constructor
     */
    public IpBlacklistFilter() {
        try {
            PropertyFileWatcher configFile = new PropertyFileWatcher(new File(
                    GeoserverDataDirectory.getGeoserverDataDirectory(), "controlflow.properties"));
            Properties props = configFile.getProperties();
            configureIpBlackList(props);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    /**
     * Filters ip black list
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        isBlocked = false;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String incomingIp = httpRequest.getRemoteAddr();
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            for (String ipAddress : ipAddresses) {
                if (ipAddress.equals(incomingIp)) {
                    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "This IP has been blocked. Please contact the server administrator");
                    isBlocked = true;
                    break;
                }
            }
            if (!isBlocked) {
                chain.doFilter(request, response);
            }
        }

    }

    private void configureIpBlackList(Properties p) {
        for (Object okey : p.keySet()) {
            String key = ((String) okey).trim();
            String value = (String) p.get(okey);
            if ("ip.blacklist".equalsIgnoreCase(key)) {
                StringTokenizer tokenizer = new StringTokenizer(value, ",");
                while (tokenizer.hasMoreTokens()) {
                    String ip = tokenizer.nextToken();
                    ipAddresses.add(ip);
                }
            }
        }
    }

    public void init(FilterConfig config) throws ServletException {
        // TODO Auto-generated method stub
    }

    public void destroy() {
        // TODO Auto-generated method stub
    }

}
