/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.geoserver.taskmanager.external.Dialect;
import org.geoserver.taskmanager.util.SqlUtil;

/**
 * Default implementation for the Dialect interface.
 *
 * <p>This should work with most databases, but it also limits the functionality of the task
 * manager.
 *
 * @author Timothy De Bock
 */
public class DefaultDialectImpl implements Dialect {

    @Override
    public String quote(String tableName) {
        return SqlUtil.quote(tableName);
    }

    @Override
    public String sqlRenameView(String currentViewName, String newViewName) {
        return "ALTER VIEW " + currentViewName + " RENAME TO " + newViewName;
    }

    @Override
    public String createIndex(
            String tableName,
            Set<String> columnNames,
            boolean isSpatialIndex,
            boolean isUniqueIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ");
        sb.append(" ON ");
        sb.append(tableName);
        // regular index
        sb.append(" (");
        for (String columnName : columnNames) {
            sb.append(quote(columnName));
            sb.append(",");
        }
        sb.setLength(sb.length() - 1);
        sb.append(" );");
        return sb.toString();
    }

    @Override
    public Set<String> getSpatialColumns(
            Connection connection, String tableName, String defaultSchema) {
        return Collections.emptySet();
    }

    @Override
    public int isNullable(int nullable) {
        return nullable;
    }

    @Override
    public String createSchema(Connection connection, String schema) {
        StringBuilder sb =
                new StringBuilder("CREATE SCHEMA IF NOT EXISTS ").append(schema).append(" ;");
        return sb.toString();
    }

    @Override
    public boolean autoUpdateView() {
        return false;
    }

    @Override
    public List<Column> getColumns(Connection connection, String tableName, ResultSet rs)
            throws SQLException {
        List<Column> result = new ArrayList<>();
        ResultSetMetaData rsmd = rs.getMetaData();

        int columnCount = rsmd.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            int col = i;
            result.add(
                    new Column() {

                        @Override
                        public String getName() throws SQLException {
                            return rsmd.getColumnLabel(col);
                        }

                        @Override
                        public String getTypeEtc() throws SQLException {
                            String typeName = rsmd.getColumnTypeName(col);
                            StringBuffer sb = new StringBuffer(typeName);
                            if (("char".equals(typeName) || "varchar".equals(typeName))
                                    && rsmd.getColumnDisplaySize(col) > 0
                                    && rsmd.getColumnDisplaySize(col) < Integer.MAX_VALUE) {
                                typeName += " (" + rsmd.getColumnDisplaySize(col) + " ) ";
                            }
                            switch (isNullable(rsmd.isNullable(col))) {
                                case ResultSetMetaData.columnNoNulls:
                                    sb.append(" NOT NULL");
                                    break;
                                case ResultSetMetaData.columnNullable:
                                    sb.append(" NULL");
                                    break;
                            }
                            return sb.toString();
                        }
                    });
        }

        return result;
    }
}
