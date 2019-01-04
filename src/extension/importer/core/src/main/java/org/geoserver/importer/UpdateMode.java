/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

/**
 * UpdateMode describes how an import will behave with respect it's target DataStore.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public enum UpdateMode {

    /**
     * The target DataStore will be created regardless of any existing. If needed, an alternative
     * name will be computed.
     */
    CREATE,

    /**
     * The target DataStore will be removed and replaced with the specified input. This may result
     * in a new schema.
     */
    REPLACE,

    /** All features in the input will be appended to the existing store. */
    APPEND,

    /** Based upon FID of input features, update any existing features. */
    UPDATE
}
