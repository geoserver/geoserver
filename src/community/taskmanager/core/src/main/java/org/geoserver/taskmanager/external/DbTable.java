/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

/**
 * Represents a table in a db.
 *
 * @author Niels Charlier
 */
public interface DbTable {

    DbSource getDbSource();

    String getTableName();
}
