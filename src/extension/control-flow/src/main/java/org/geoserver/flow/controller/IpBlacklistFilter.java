/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.io.File;
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

    private Set<String> blackListedAddresses;

    private final PropertyFileWatcher configFile;
    
    /**
     * Constructor used for testing purposes
     * 
     * @param props
     */
    public IpBlacklistFilter(Properties props) {
        blackListedAddresses = loadConfiguration(props);
        configFile = null;
    }

    /**
     * Default constructor
     */
    public IpBlacklistFilter() {
        try {
            File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
                    "controlflow.properties");
            configFile = new PropertyFileWatcher(file);
            blackListedAddresses = reloadConfiguration();
        } catch (Exception e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Filters ip black list
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        if (isBlackListed(httpRequest)) {
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "This IP has been blocked. Please contact the server administrator");
                    return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isBlackListed(HttpServletRequest httpRequest) throws IOException {
        if(configFile != null && configFile.isStale()){
            synchronized(configFile){
                if(configFile.isStale()){
                    this.blackListedAddresses = reloadConfiguration();
                }
            }
        }
        if(blackListedAddresses.isEmpty()){
            return false;
        }
        String incomingIp = IpFlowController.getRemoteAddr(httpRequest);
        boolean blocked = blackListedAddresses.contains(incomingIp);
        return blocked;
    }

    private Set<String> reloadConfiguration() throws IOException {
        Properties props = configFile.getProperties();
        if(props == null){
            //file doesn't exist
            return Collections.emptySet();
        }
        return loadConfiguration(props);
    }

    private Set<String> loadConfiguration(Properties props) {
        String rawList = props.getProperty("ip.blacklist");
        if(null == rawList){
            return Collections.emptySet();
        }
        Set<String> ipAddresses = new HashSet<String>();
        for(String ip : rawList.split(",")){
            ipAddresses.add(ip.trim());
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
