/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.util.logging.Logging;

/**
 * Data access object for rest security configuration.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class RESTAccessRuleDAO extends AbstractAccessRuleDAO<String> {
    private final static Logger LOGGER = Logging.getLogger(RESTAccessRuleDAO.class);
    
    /**
     * rule pattern
     */
    static final Pattern PATTERN = 
        Pattern.compile("\\S+;(GET|POST|PUT|DELETE|HEAD)(,(GET|POST|PUT|DELETE|HEAD))*=\\S+(, ?\\S+)*");
    
    protected RESTAccessRuleDAO(GeoServerDataDirectory dd)
            throws IOException {
        super(dd, "rest.properties");
    }

    @Override
    protected void loadRules(Properties props) {
        rules = new LinkedHashSet<String>();
        for (Map.Entry<Object,Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            
            String rule = key+"="+val; 
            if (!PATTERN.matcher(rule).matches()) {
                LOGGER.severe("Ignoring '" + rule + "' not matching " + PATTERN);
                continue;
            }
            rule = rule.replaceAll(";", ":");
            rules.add(rule);
        }
    }
    
    @Override
    protected Properties toProperties() {
        return null;
    }

}
