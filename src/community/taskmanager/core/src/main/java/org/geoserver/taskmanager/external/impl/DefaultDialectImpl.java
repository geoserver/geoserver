/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.taskmanager.external.Dialect;
import org.geoserver.taskmanager.external.GeometryTable;
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
                            StringBuffer sb = new StringBuffer();
                            JDBCType type = JDBCType.valueOf(rsmd.getColumnType(col));
                            if (type == JDBCType.OTHER) {
                                sb.append(rsmd.getColumnTypeName(col));
                            } else {
                                sb.append(type.name());
                                if ((type == JDBCType.CHAR || type == JDBCType.VARCHAR)
                                        && rsmd.getColumnDisplaySize(col) > 0
                                        && rsmd.getColumnDisplaySize(col) < Integer.MAX_VALUE) {
                                    sb.append(" (")
                                            .append(rsmd.getColumnDisplaySize(col))
                                            .append(" ) ");
                                }
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

    @Override
    public Map<String, GeometryColumn> getRawSpatialColumns(
            GeometryTable geometryTable, Connection connection, String tableName)
            throws SQLException {
        Map<String, GeometryColumn> result = new HashMap<>();
        String sql =
                "SELECT "
                        + quote(geometryTable.getAttributeNameGeometry())
                        + ", "
                        + quote(geometryTable.getAttributeNameSrid())
                        + ", "
                        + quote(geometryTable.getAttributeNameType())
                        + " FROM "
                        + quote(geometryTable.getNameTable())
                        + " WHERE "
                        + quote(geometryTable.getAttributeNameTable())
                        + " = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet set = stmt.executeQuery()) {
                while (set.next()) {
                    String name = set.getString(1);
                    int srid = set.getInt(2);
                    String type = set.getString(3);
                    result.put(
                            name,
                            new GeometryColumn() {
                                @Override
                                public int getSrid() {
                                    return srid;
                                }

                                @Override
                                public String getType() {
                                    return type;
                                }
                            });
                }
            }
        }
        return result;
    }

    @Override
    public String getGeometryType(String type, int srid) {
        throw new UnsupportedOperationException("geometry translation not supported by target db");
    }

    @Override
    public String getConvertedGeometry(GeometryTable.Type type) {
        throw new UnsupportedOperationException("geometry translation not supported by target db");
    }
}
