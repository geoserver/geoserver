/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.sql.Connection;
import java.util.Set;

/**
 * Dialect specific commands.
 *
 * @author Timothy De Bock
 *
 */
public interface Dialect {


    /**
     * Put quotes arround the schema name and the table name.
     *
     * @return the quote table name.
     */
    String quote(String tableName);

    /**
     * 
     * @param currentViewName
     * @param newViewName
     * @return
     */
    String sqlRenameView(String currentViewName, String newViewName);

    /**
     * Returns the create index statement with the index name over the columns.
     * @param tableName
     * @param columnNames
     * @param isSpatialIndex
     * @param isUniqueIndex
     * @return
     */
    String createIndex(String tableName, Set<String> columnNames, boolean isSpatialIndex, boolean isUniqueIndex);

    /**
     * 
     * @param sourceConn
     * @param tableName
     * @return
     */
    Set<String> getSpatialColumns(Connection sourceConn, String tableName);

    int isNullable(int nullable);
}
