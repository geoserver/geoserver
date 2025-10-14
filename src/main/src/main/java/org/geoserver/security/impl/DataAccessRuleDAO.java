/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DataAccessRule.ANY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;

/**
 * Allows one to manage the rules used by the per layer security subsystem TODO: consider splitting the persistence of
 * properties into two strategies, and in memory one, and a file system one (this class is so marginal that I did not do
 * so right away, in memory access is mostly handy for testing)
 */
public class DataAccessRuleDAO extends AbstractAccessRuleDAO<DataAccessRule> {

    public static final String KEY_MODE = "mode";
    private static final String KEY_SANDBOX = "filesystemSandbox";
    private static Pattern DOT = Pattern.compile("\\.");

    private static final Logger LOGGER = Logging.getLogger(DataAccessRuleDAO.class);

    /** property file name */
    static final String LAYERS = "layers.properties";

    /** The catalog */
    Catalog rawCatalog;

    /** Default to the highest security mode */
    CatalogMode catalogMode = CatalogMode.HIDE;

    String filesystemSandbox;

    /** Returns the instanced contained in the Spring context for the UI to use */
    public static DataAccessRuleDAO get() {
        return GeoServerExtensions.bean(DataAccessRuleDAO.class);
    }

    /** Builds a new dao */
    public DataAccessRuleDAO(GeoServerDataDirectory dd, Catalog rawCatalog) throws IOException {
        super(dd, LAYERS);
        this.rawCatalog = rawCatalog;
    }

    /** Builds a new dao with a custom security dir. Used mostly for testing purposes */
    DataAccessRuleDAO(Catalog rawCatalog, Resource securityDir) {
        super(securityDir, LAYERS);
        this.rawCatalog = rawCatalog;
    }

    /** The way the catalog should react to unauthorized access */
    public CatalogMode getMode() {
        checkPropertyFile(false);
        return catalogMode;
    }

    /** Parses the rules contained in the property file */
    @Override
    protected void loadRules(Properties props) {
        SortedSet<DataAccessRule> result = new ConcurrentSkipListSet<>();
        CatalogMode catalogMode = CatalogMode.HIDE;
        this.filesystemSandbox = null;
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String ruleKey = (String) entry.getKey();
            String ruleValue = (String) entry.getValue();

            // check for the mode
            if (KEY_MODE.equalsIgnoreCase(ruleKey)) {
                try {
                    catalogMode = CatalogMode.valueOf(ruleValue.toUpperCase());
                } catch (Exception e) {
                    LOGGER.warning("Invalid security mode "
                            + ruleValue
                            + " acceptable values are "
                            + Arrays.asList(CatalogMode.values()));
                }
            } else if (KEY_SANDBOX.equalsIgnoreCase(ruleKey)) {
                filesystemSandbox = ruleValue;
            } else {
                DataAccessRule rule = parseDataAccessRule(ruleKey, ruleValue);
                if (rule != null) {
                    if (result.contains(rule))
                        LOGGER.warning(
                                "Rule " + ruleKey + "." + ruleValue + " overwrites another rule on the same path");
                    result.add(rule);
                }
            }
        }

        // make sure the two basic rules if the set is empty
        if (result.isEmpty()) {
            result.add(new DataAccessRule(DataAccessRule.READ_ALL));
            result.add(new DataAccessRule(DataAccessRule.WRITE_ALL));
        }

        this.catalogMode = catalogMode;
        this.rules = result;
    }

    /** Parses a single layer.properties line into a {@link DataAccessRule}, returns false if the rule is not valid */
    DataAccessRule parseDataAccessRule(String ruleKey, String ruleValue) {
        final String rule = ruleKey + "=" + ruleValue;

        // parse
        String[] elements = parseElements(ruleKey);
        // perform basic checks on the elements
        if (elements.length != 3 && elements.length != 2) {
            LOGGER.warning(
                    "Invalid rule "
                            + rule
                            + ", the expected format is workspace.layer.mode=role1,role2,... or globalGroup.mode=role1,role2,...");
            return null;
        }
        String root = elements[0];
        String layerName, modeAlias;
        if (elements.length == 3) {
            layerName = elements[1];
            modeAlias = elements[2];
        } else {
            layerName = null;
            modeAlias = elements[1];
        }

        Set<String> roles = parseRoles(ruleValue);

        // emit warnings for unknown workspaces, layers, but don't skip the rule,
        // people might be editing the catalog structure and will edit the access rule
        // file afterwards
        if (layerName != null) {
            if (!ANY.equals(root) && rawCatalog.getWorkspaceByName(root) == null)
                LOGGER.warning("Namespace/Workspace " + root + " is unknown in rule " + rule);
            if (LOGGER.isLoggable(Level.FINE)
                    && !ANY.equals(layerName)
                    && rawCatalog.getLayerByName(new NameImpl(root, layerName)) == null
                    && rawCatalog.getLayerGroupByName(root, layerName) == null)
                LOGGER.fine("Layer " + root + ":" + layerName + " is unknown in rule: " + rule);
        } else {
            if (!ANY.equals(root) && rawCatalog.getLayerGroupByName(root) == null)
                LOGGER.warning("Global layer group " + root + " is unknown in rule " + rule);
        }

        // check the access mode sanity
        AccessMode mode = AccessMode.getByAlias(modeAlias);
        if (mode == null) {
            LOGGER.warning("Unknown access mode " + modeAlias + " in " + ruleKey + ", skipping rule " + rule);
            return null;
        }

        // check ANY usage sanity
        if (ANY.equals(root)) {
            if (!ANY.equals(layerName)) {
                LOGGER.warning("Invalid rule "
                        + rule
                        + ", when namespace "
                        + "is * then also layer must be *. Skipping rule "
                        + rule);
                return null;
            }
        }

        // check admin access only applied globally to workspace
        if (mode == AccessMode.ADMIN && !ANY.equals(layerName)) {
            // TODO: should this throw an exception instead of ignore rule?
            LOGGER.warning("Invalid rule "
                    + rule
                    + ", admin (a) privileges may only be applied "
                    + "globally to a workspace, layer must be *, skipping rule");
            return null;
        }

        // build the rule
        return new DataAccessRule(root, layerName, mode, roles);
    }

    /** Turns the rules list into a property bag */
    @Override
    protected Properties toProperties() {
        Properties props = new Properties();
        props.put(KEY_MODE, catalogMode.toString());
        if (filesystemSandbox != null) {
            props.put(KEY_SANDBOX, filesystemSandbox);
        }
        for (DataAccessRule rule : rules) {
            StringBuilder sbKey = new StringBuilder(DOT.matcher(rule.getRoot()).replaceAll("\\\\."));
            if (!rule.isGlobalGroupRule()) {
                sbKey.append(".").append(DOT.matcher(rule.getLayer()).replaceAll("\\\\."));
            }
            sbKey.append(".").append(rule.getAccessMode().getAlias());
            props.put(sbKey.toString(), rule.getValue());
        }
        return props;
    }

    /** Parses workspace.layer.mode into an array of strings */
    static String[] parseElements(String path) {
        String[] rawParse = path.trim().split("\\s*\\.\\s*");
        List<String> result = new ArrayList<>();
        String prefix = null;
        for (String raw : rawParse) {
            if (prefix != null) raw = prefix + "." + raw;
            // just assume the escape is invalid char besides \. and check it once only
            if (raw.endsWith("\\")) {
                prefix = raw.substring(0, raw.length() - 1);
            } else {
                result.add(raw);
                prefix = null;
            }
        }

        return result.toArray(new String[result.size()]);
    }

    public void setCatalogMode(CatalogMode catalogMode) {
        this.catalogMode = catalogMode;
    }

    public static CatalogMode getByAlias(String alias) {
        for (CatalogMode mode : CatalogMode.values()) {
            if (mode.name().equals(alias)) {
                return mode;
            }
        }
        return null;
    }

    /** Returns a sorted set of rules associated to the role */
    public SortedSet<DataAccessRule> getRulesAssociatedWithRole(String role) {
        SortedSet<DataAccessRule> result = new TreeSet<>();
        for (DataAccessRule rule : getRules()) if (rule.getRoles().contains(role)) result.add(rule);
        return result;
    }

    /** Returns the file system sandbox configured in <code>layers.properties</code>, if set, or null otherwise. */
    public String getFilesystemSandbox() {
        return filesystemSandbox;
    }

    /**
     * Sets the file system sandbox to be used by the file access manager.
     *
     * @param filesystemSandbox the file system sandbox to be used by the file access manager, or null if the sandbox
     *     should be removed
     */
    public void setFilesystemSandbox(String filesystemSandbox) {
        // sanitize in case a store-like path has ben provided
        if (filesystemSandbox != null && filesystemSandbox.startsWith("file://"))
            filesystemSandbox = filesystemSandbox.substring("file://".length());
        this.filesystemSandbox = filesystemSandbox;
    }
}
