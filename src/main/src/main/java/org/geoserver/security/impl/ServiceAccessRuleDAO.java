/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DataAccessRule.*;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/**
 * Allows one to manage the rules used by the service layer security subsystem TODO: consider
 * splitting the persistence of properties into two strategies, and in memory one, and a file system
 * one (this class is so marginal that I did not do so right away, in memory access is mostly handy
 * for testing)
 */
public class ServiceAccessRuleDAO extends AbstractAccessRuleDAO<ServiceAccessRule> {
    private static final Logger LOGGER = Logging.getLogger(ServiceAccessRuleDAO.class);

    /** property file name */
    static final String SERVICES = "services.properties";

    /** the catalog */
    Catalog rawCatalog;

    /** Returns the instanced contained in the Spring context for the UI to use */
    public static ServiceAccessRuleDAO get() {
        return GeoServerExtensions.bean(ServiceAccessRuleDAO.class);
    }

    public ServiceAccessRuleDAO(GeoServerDataDirectory dd, Catalog rawCatalog) throws IOException {
        super(dd, SERVICES);
        this.rawCatalog = rawCatalog;
    }

    /** Builds a new dao */
    public ServiceAccessRuleDAO() throws IOException {
        super(GeoServerExtensions.bean(GeoServerDataDirectory.class), SERVICES);
    }

    /** Builds a new dao with a custom security dir. Used mostly for testing purposes */
    ServiceAccessRuleDAO(Catalog rawCatalog, Resource securityDir) {
        super(securityDir, SERVICES);
    }

    /** Parses the rules contained in the property file */
    protected void loadRules(Properties props) {
        TreeSet<ServiceAccessRule> result = new TreeSet<ServiceAccessRule>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String ruleKey = (String) entry.getKey();
            String ruleValue = (String) entry.getValue();

            ServiceAccessRule rule = parseServiceAccessRule(ruleKey, ruleValue);
            if (rule != null) {
                if (result.contains(rule))
                    LOGGER.warning(
                            "Rule "
                                    + ruleKey
                                    + "."
                                    + ruleValue
                                    + " overwrites another rule on the same path");
                result.add(rule);
            }
        }

        // make sure to add the "all access alloed" rule if the set if empty
        if (result.size() == 0) {
            result.add(new ServiceAccessRule(new ServiceAccessRule()));
        }

        rules = result;
    }

    /**
     * Parses a single layer.properties line into a {@link DataAccessRule}, returns false if the
     * rule is not valid
     */
    ServiceAccessRule parseServiceAccessRule(String ruleKey, String ruleValue) {
        final String rule = ruleKey + "=" + ruleValue;

        // parse
        String[] elements = parseElements(ruleKey);
        if (elements.length != 2) {
            LOGGER.warning(
                    "Invalid rule "
                            + rule
                            + ", the expected format is service.method=role1,role2,...");
            return null;
        }
        String service = elements[0];
        String method = elements[1];
        Set<String> roles = parseRoles(ruleValue);

        // check ANY usage sanity
        if (ANY.equals(service)) {
            if (!ANY.equals(method)) {
                LOGGER.warning(
                        "Invalid rule "
                                + rule
                                + ", when namespace "
                                + "is * then also layer must be *. Skipping rule "
                                + rule);
                return null;
            }
        }

        // build the rule
        return new ServiceAccessRule(service, method, roles);
    }

    /** Turns the rules list into a property bag */
    protected Properties toProperties() {
        Properties props = new Properties();
        for (ServiceAccessRule rule : rules) {
            props.put(rule.getKey(), rule.getValue());
        }
        return props;
    }

    /** Parses workspace.layer.mode into an array of strings */
    private String[] parseElements(String path) {
        // regexp: ignore extra spaces, split on dot
        return path.split("\\s*\\.\\s*");
    }

    /** Returns a sorted set of rules associated to the role */
    public SortedSet<ServiceAccessRule> getRulesAssociatedWithRole(String role) {
        SortedSet<ServiceAccessRule> result = new TreeSet<ServiceAccessRule>();
        for (ServiceAccessRule rule : getRules())
            if (rule.getRoles().contains(role)) result.add(rule);
        return result;
    }
}
