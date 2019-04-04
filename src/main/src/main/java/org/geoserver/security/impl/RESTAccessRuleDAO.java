/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * Data access object for rest security configuration.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RESTAccessRuleDAO extends AbstractAccessRuleDAO<String> {
    private static final Logger LOGGER = Logging.getLogger(RESTAccessRuleDAO.class);

    public static RESTAccessRuleDAO get() {
        return GeoServerExtensions.bean(RESTAccessRuleDAO.class);
    }

    /** rule pattern */
    static final Pattern PATTERN =
            Pattern.compile(
                    "\\S+;(GET|POST|PUT|DELETE|HEAD)(,(GET|POST|PUT|DELETE|HEAD))*=\\S+(, ?\\S+)*");

    protected RESTAccessRuleDAO(GeoServerDataDirectory dd) throws IOException {
        super(dd, "rest.properties");
    }

    @Override
    protected void loadRules(Properties props) {
        rules = new LinkedHashSet<String>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();

            String rule = key + "=" + val;
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
        Properties props = new Properties();
        for (String rule : rules) {
            rule = rule.replaceAll(":", ";");
            if (!PATTERN.matcher(rule).matches()) {
                LOGGER.severe("Invalid '" + rule + "' not matching " + PATTERN);
                continue;
            }
            String[] parts = rule.split("=");
            props.setProperty(parts[0], parts[1]);
        }
        return props;
    }
}
