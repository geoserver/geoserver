/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc;

import java.sql.Connection;

/**
 * Common interface used by all the metadata objects in a JDBC DataStore implementation.
 *
 * @author Jose Macchi - Geosolutions
 */
public interface JdbcConnectable {

    Connection getConnection();
}
