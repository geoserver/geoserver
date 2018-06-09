/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;

/**
 * A class that allows the configuration of an ip black list, rejecting requests from ip addresses
 * configured in the controlflow.properties file
 *
 * @author Juan Marin, OpenGeo
 */
public class IpBlacklistFilter implements GeoServerFilter {

    static final Logger LOGGER = Logging.getLogger(IpBlacklistFilter.class);
    static final String PROPERTYFILENAME = "controlflow.properties";
    static final String BLPROPERTY = "ip.blacklist";
    static final String WLPROPERTY = "ip.whitelist";
    private Set<String> blackListedAddresses;
    private Set<String> whiteListedAddresses;

    private final PropertyFileWatcher configFile;

    /**
     * Constructor used for testing purposes
     *
     * @param props configuraiton properties
     */
    public IpBlacklistFilter(Properties props) {
        this.blackListedAddresses = loadConfiguration(props, BLPROPERTY);
        this.whiteListedAddresses = loadConfiguration(props, WLPROPERTY);
        configFile = null;
    }

    /** Default constructor */
    public IpBlacklistFilter() {
        try {
            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            Resource resource = loader.get(PROPERTYFILENAME);
            configFile = new PropertyFileWatcher(resource);
            blackListedAddresses = reloadConfiguration(BLPROPERTY);
            whiteListedAddresses = reloadConfiguration(WLPROPERTY);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /** Filters ip black list */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (isBlackListed(httpRequest)) {
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "This IP has been blocked. Please contact the server administrator");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isBlackListed(HttpServletRequest httpRequest) throws IOException {
        if (configFile != null && configFile.isStale()) {
            synchronized (configFile) {
                if (configFile.isStale()) {
                    this.blackListedAddresses = reloadConfiguration(BLPROPERTY);
                    this.whiteListedAddresses = reloadConfiguration(WLPROPERTY);
                }
            }
        }
        if (blackListedAddresses.isEmpty()) {
            return false;
        }
        String incomingIp = IpFlowController.getRemoteAddr(httpRequest);
        boolean blocked = false;
        // Check IP on blackList roles (to block)
        for (String blackListRole : blackListedAddresses) {
            if (incomingIp.matches(blackListRole)) {
                blocked = true;
                break;
            }
        }

        // Check IP (if blocked) on whiteList roles (to unlock)
        if (blocked && !whiteListedAddresses.isEmpty()) {
            for (String whiteListRole : whiteListedAddresses) {
                if (incomingIp.matches(whiteListRole)) {
                    blocked = false;
                    break;
                }
            }
        }
        return blocked;
    }

    private Set<String> reloadConfiguration(String property) throws IOException {
        Properties props = configFile.getProperties();
        if (props == null) {
            // file doesn't exist
            return Collections.emptySet();
        }
        return loadConfiguration(props, property);
    }

    private Set<String> loadConfiguration(Properties props, String property) {
        String rawList = props.getProperty(property);
        if (null == rawList) {
            return Collections.emptySet();
        }
        Set<String> ipAddresses = new HashSet<String>();
        for (String ip : rawList.split(",")) {
            ipAddresses.add(ip.trim().replaceAll("\\*", "(.{0,1}[0-9]+.{0,1}){0,4}"));
        }
        return ipAddresses;
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        // TODO Auto-generated method stub
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }
}
