/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dialect specific commands.
 *
 * @author Timothy De Bock
 */
public interface Dialect {

    interface Column {
        String getName() throws SQLException;

        String getTypeEtc() throws SQLException;
    }

    public interface GeometryColumn {

        String getType();

        int getSrid();
    }

    /**
     * Put quotes arround the schema name and the table name.
     *
     * @return the quoted table name
     */
    String quote(String tableName);

    /** @return statement to rename view */
    String sqlRenameView(String currentViewName, String newViewName);

    /**
     * Returns the create index statement with the index name over the columns.
     *
     * @return statement to create index
     */
    String createIndex(String tableName, Set<String> columnNames, boolean isSpatialIndex, boolean isUniqueIndex);

    /** @return set of spatial columns */
    Set<String> getSpatialColumns(Connection connection, String tableName, String defaultSchema);

    /** translate nullable code */
    int isNullable(int nullable);

    /**
     * Create the schema if it does not exist.
     *
     * @return statement to create schema if it doesn't exist
     */
    String createSchema(Connection connection, String schema);

    /**
     * Are views automatically updated when dependent object is renamed?
     *
     * @return true or false
     */
    boolean autoUpdateView();

    /**
     * Return list of column descriptions (name, type, etc)
     *
     * @throws SQLException
     */
    List<Column> getColumns(Connection connection, String tableName, ResultSet rs) throws SQLException;

    /** Return a geometry type */
    String getGeometryType(String type, int srid);

    /**
     * Return expression that translates wkt string to geometry type
     *
     * @param wkt
     * @return
     */
    String getConvertedGeometry(GeometryTable.Type type);

    /**
     * Return spatial columns that are encoded as wkt strings and need to be translated
     *
     * @param geometryTable
     * @param connection
     * @param tableName
     * @return
     * @throws SQLException
     */
    Map<String, GeometryColumn> getRawSpatialColumns(
            GeometryTable geometryTable, Connection connection, String tableName) throws SQLException;
}
