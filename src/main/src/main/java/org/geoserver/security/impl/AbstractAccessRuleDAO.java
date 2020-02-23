/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DataAccessRule.ANY;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.PropertyFileWatcher;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * Abstract class for security dao's whose configuration is stored in a property file.
 *
 * <p>Subclasses must implement {@link #loadRules(Properties)} and {@link #toProperties()} to
 * provide the mapping back and forth to the underlying properly file.
 *
 * @author Justin Deoliveira, OpenGeo
 * @param <R> The access rule class.
 */
public abstract class AbstractAccessRuleDAO<R extends Comparable<?>> {
    private static final Logger LOGGER = Logging.getLogger(AbstractAccessRuleDAO.class);

    /** Parsed rules */
    protected Set<R> rules;

    /** Used to check the file for modifications */
    PropertyFileWatcher watcher;

    /** Stores the time of the last rule list loading */
    protected long lastModified;

    /** The security dir */
    Resource securityDir;

    /** The property file name that stores the raw rule names. */
    String propertyFileName;

    /** Data directory accessor */
    GeoServerDataDirectory dd;

    protected AbstractAccessRuleDAO(GeoServerDataDirectory dd, String propertyFileName)
            throws IOException {
        this.dd = dd;
        this.securityDir = dd.getSecurity();
        this.propertyFileName = propertyFileName;
    }

    protected AbstractAccessRuleDAO(Resource securityDirectory, String propertyFileName) {
        this.securityDir = securityDirectory;
        this.propertyFileName = propertyFileName;
        this.dd = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        // this.dd = org.vfny.geoserver.global.GeoserverDataDirectory.accessor();
    }

    /**
     * Returns the list of rules contained in the property file. The returned rules are sorted
     * against the <code>R</code> natural order
     */
    public List<R> getRules() {
        checkPropertyFile(false);
        return new ArrayList<R>(rules);
    }

    /**
     * Adds/overwrites a rule in the rule set
     *
     * @return true if the set did not contain the rule already, false otherwise
     */
    public boolean addRule(R rule) {
        lastModified = System.currentTimeMillis();
        return rules.add(rule);
    }

    /** Forces a reload of the rules */
    public void reload() {
        checkPropertyFile(true);
    }

    /** Cleans up the contents of the rule set */
    public void clear() {
        rules.clear();
        lastModified = System.currentTimeMillis();
    }

    /** Removes the rule from rule set */
    public boolean removeRule(R rule) {
        lastModified = System.currentTimeMillis();
        return rules.remove(rule);
    }

    /**
     * Returns the last modification date of the rules in this DAO (last time the rules were
     * reloaded from the property file)
     */
    public long getLastModified() {
        return lastModified;
    }

    public boolean isModified() {
        return watcher != null && watcher.isStale();
    }

    /** Writes the rules back to file system */
    public void storeRules() throws IOException {
        OutputStream os = null;
        try {
            // turn back the users into a users map
            Properties p = toProperties();

            // write out to the data dir
            Resource propFile = securityDir.get(propertyFileName);
            os = propFile.out();
            p.store(os, null);
            lastModified = System.currentTimeMillis();
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            else
                throw (IOException)
                        new IOException("Could not write rules to " + propertyFileName)
                                .initCause(e);
        } finally {
            if (os != null) os.close();
        }
    }

    /** Checks the property file is up to date, eventually rebuilds the tree */
    protected void checkPropertyFile(boolean force) {
        try {
            if (rules == null || force) {
                // no security folder, let's work against an empty properties then
                if (securityDir == null || securityDir.getType() == Type.UNDEFINED) {
                    this.rules = new TreeSet<R>();
                } else {
                    // no security config, let's work against an empty properties then
                    Resource layers = securityDir.get(propertyFileName);
                    if (layers.getType() == Type.UNDEFINED) {
                        // try to load a template and copy it over
                        InputStream in =
                                getClass().getResourceAsStream(propertyFileName + ".template");
                        if (in != null) {
                            IOUtils.copy(in, layers.out());
                        }
                    }

                    if (layers.getType() == Type.UNDEFINED) {
                        this.rules = new TreeSet<R>();
                    } else {
                        // ok, something is there, let's load it
                        watcher = new PropertyFileWatcher(layers);
                        loadRules(watcher.getProperties());
                    }
                }
                lastModified = System.currentTimeMillis();
            } else if (isModified()) {
                loadRules(watcher.getProperties());
                lastModified = System.currentTimeMillis();
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to reload data access rules from layers.properties, keeping old rules",
                    e);
        }
    }

    /**
     * Parses the rules contained in the property file
     *
     * @param props The parsed property file.
     */
    protected abstract void loadRules(Properties props);

    /** Turns the rules list into a property bag */
    protected abstract Properties toProperties();

    /**
     * Parses a comma separated list of roles into a set of strings, with special handling for the
     * {@link DataAccessRule#ANY} role
     *
     * @param roleCsv Comma separated list of roles.
     */
    protected Set<String> parseRoles(String roleCsv) {
        // regexp: treat extra spaces as separators, ignore extra commas
        // "a,,b, ,, c" --> ["a","b","c"]
        String[] rolesArray = roleCsv.split("[\\s,]+");
        Set<String> roles = new HashSet<String>(rolesArray.length);
        roles.addAll(Arrays.asList(rolesArray));

        // if any of the roles is * we just remove all of the others
        for (String role : roles) {
            if (ANY.equals(role)) return Collections.singleton("*");
        }

        return roles;
    }
}
