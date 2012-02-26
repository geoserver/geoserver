/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
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

import org.geoserver.flow.ControlFlowConfigurator;
import org.geoserver.flow.FlowController;
import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.flow.controller.GlobalFlowController;
import org.geoserver.flow.controller.IpFlowController;
import org.geoserver.flow.controller.SingleIpFlowController;
import org.geoserver.flow.controller.UserFlowController;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Basic property file based {@link ControlFlowConfigurator} implementation
 * 
 * @author Andrea Aime - OpenGeo
 * @author Juan Marin, OpenGeo
 */
public class DefaultControlFlowConfigurator implements ControlFlowConfigurator {
    static final Logger LOGGER = Logging.getLogger(DefaultControlFlowConfigurator.class);

    PropertyFileWatcher configFile;

    long timeout = -1;

    public DefaultControlFlowConfigurator() {
        configFile = new PropertyFileWatcher(new File(
                GeoserverDataDirectory.getGeoserverDataDirectory(), "controlflow.properties"));
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
            String[] keys = key.trim().split("\\s*\\.\\s*");

            int queueSize = 0;
            StringTokenizer tokenizer = new StringTokenizer(value, ",");
            try {
                if (tokenizer.countTokens() == 1) {
                    queueSize = Integer.parseInt(value);
                } else {
                    queueSize = Integer.parseInt(tokenizer.nextToken());
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
                    controller = new SingleIpFlowController(queueSize, tokenizer.nextToken());
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
