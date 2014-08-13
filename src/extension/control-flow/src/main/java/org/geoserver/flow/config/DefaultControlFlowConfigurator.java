/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.flow.ControlFlowConfigurator;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.flow.controller.GlobalFlowController;
import org.geoserver.flow.controller.IpFlowController;
import org.geoserver.flow.controller.SingleIpFlowController;
import org.geoserver.flow.controller.UserFlowController;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;

/**
 * Basic property file based {@link ControlFlowConfigurator} implementation
 * 
 * @author Andrea Aime - OpenGeo
 * @author Juan Marin, OpenGeo
 */
public class DefaultControlFlowConfigurator implements ControlFlowConfigurator {
    static final Logger LOGGER = Logging.getLogger(DefaultControlFlowConfigurator.class);
    static final String PROPERTYFILENAME="controlflow.properties";
    PropertyFileWatcher configFile;

    long timeout = -1;

    /** Default watches controlflow.properties */
    public DefaultControlFlowConfigurator() {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource controlflow = loader.get(PROPERTYFILENAME);
        configFile = new PropertyFileWatcher(controlflow);        
    }

    /**
     * Constructor used for testing purposes
     * 
     * @param watcher
     */
    DefaultControlFlowConfigurator(PropertyFileWatcher watcher) {
        this.configFile = watcher;
    }

    public List<FlowController> buildFlowControllers() throws Exception {
        timeout = -1;

        Properties p = configFile.getProperties();
        List<FlowController> newControllers = new ArrayList<FlowController>();
        for (Object okey : p.keySet()) {
            String key = ((String) okey).trim();
            String value = (String) p.get(okey);
            LOGGER.info("Loading control-flow configuration: " + key + "=" + value);

            String[] keys = key.split("\\s*\\.\\s*");

            int queueSize = 0;
            StringTokenizer tokenizer = new StringTokenizer(value, ",");
            try {
            	//ip.blacklist and ip.whitelist properties aren't integer values
            	if(!"ip.blacklist".equals(key) && !"ip.whitelist".equals(key)){
                    if (tokenizer.countTokens() == 1) {
                        queueSize = Integer.parseInt(value);
                    } else {
                        queueSize = Integer.parseInt(tokenizer.nextToken());
                    }
                }else{
                	continue;
                }
            } catch (NumberFormatException e) {
                LOGGER.severe("Rules should be assigned just a queue size, instead " + okey
                        + " is associated to " + value);
                continue;
            }

            FlowController controller = null;
            if ("timeout".equalsIgnoreCase(key)) {
                timeout = queueSize * 1000;
                continue;
            }
            if ("ows.global".equalsIgnoreCase(key)) {
                controller = new GlobalFlowController(queueSize);
            } else if ("ows".equals(keys[0])) {
                // todo: check, if possible, if the service, method and output format actually exist
                if (keys.length >= 4) {
                    controller = new BasicOWSController(keys[1], keys[2], keys[3], queueSize);
                } else if (keys.length == 3) {
                    controller = new BasicOWSController(keys[1], keys[2], queueSize);
                } else if (keys.length == 2) {
                    controller = new BasicOWSController(keys[1], queueSize);
                }
            } else if ("user".equals(keys[0])) {
                controller = new UserFlowController(queueSize);
            } else if ("ip".equals(keys[0])) {
                if (keys.length == 1) {
                    controller = new IpFlowController(queueSize);
                } else if (keys.length > 1) {
                	if(!"blacklist".equals(keys[1]) && !"whitelist".equals(keys[1])){
                		String ip = key.substring("ip.".length());
                		controller = new SingleIpFlowController(queueSize, ip);
                	}
                }
            }
            if (controller == null) {
                LOGGER.severe("Could not parse rule '" + okey + "=" + value);
            } else {
                newControllers.add(controller);
            }
        }

        return newControllers;
    }

    public boolean isStale() {
        return configFile.isStale();
    }

    public long getTimeout() {
        return timeout;
    }

}
