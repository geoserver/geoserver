/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

/**
 * A list of ways a resource can be accessed according to the data security layer
 *
 * @author Andrea Aime - TOPP
 */
public enum AccessMode {
    /** Pure read access, see but not touch */
    READ("r"),
    /** Write access, that is, see and modify */
    WRITE("w"),
    /** Admin access, read+write and full configure */
    ADMIN("a");

    String alias;

    /**
     * Builds an access mode
     *
     * @param alias a shortcut for the access mode
     */
    AccessMode(String alias) {
        this.alias = alias;
    }

    /**
     * Locates the access mode by its alias
     *
     * @return the access mode, or null if not found
     */
    public static AccessMode getByAlias(String alias) {
        for (AccessMode mode : AccessMode.values()) {
            if (mode.alias.equals(alias)) return mode;
        }
        return null;
    }

    /** Returns the short version of the {@link AccessMode} name */
    public String getAlias() {
        return alias;
    }
}
