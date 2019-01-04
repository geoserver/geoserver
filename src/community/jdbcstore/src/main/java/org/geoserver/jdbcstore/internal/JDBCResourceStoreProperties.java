/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore.internal;

import org.geoserver.jdbcloader.JDBCLoaderProperties;

/**
 * Configuration information for JDBCResourceStore
 *
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public class JDBCResourceStoreProperties extends JDBCLoaderProperties {

    private static final long serialVersionUID = -3335880912330668027L;

    public JDBCResourceStoreProperties(JDBCResourceStorePropertiesFactoryBean factory) {
        super(factory);
    }

    // jdbcstore specific properties may go here.

    /**
     * Determines behaviour of the rename/move operation: linux-style (delete if exists) or
     * windows-style (fail if exists).
     *
     * @return true iff jdbcstore should delete destination on rename
     */
    public boolean isDeleteDestinationOnRename() {
        return Boolean.valueOf(getProperty("deleteDestinationOnRename", "false"));
    }

    /**
     * Directories that are to be ignored by the JDBCStore: they will not be imported and they will
     * be retrieved from the file system.
     */
    public String[] getIgnoreDirs() {
        return getProperty("ignoreDirs", "").split(",");
    }
}
