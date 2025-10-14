/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.taskmanager.external.GeometryTable;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.util.logging.Logging;

/**
 * Postgis Dialect.
 *
 * @author Timothy De Bock
 */
public class PostgisDialectImpl extends DefaultDialectImpl {

    private static final Logger LOGGER = Logging.getLogger(PostgisDialectImpl.class);

    @Override
    public String quote(String tableName) {
        return SqlUtil.quote(tableName);
    }

    @Override
    public String createIndex(
            String tableName, Set<String> columnNames, boolean isSpatialIndex, boolean isUniqueIndex) {

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ");
        if (isUniqueIndex) {
            sb.append(" UNIQUE");
        }
        sb.append(" INDEX ");
        // sb.append(indexName);
        sb.append(" ON ");
        sb.append(quote(tableName));

        if (isSpatialIndex) {
            sb.append(" USING gist ");
        }

        sb.append(" (");
        for (String columnName : columnNames) {
            // if the "column" is functional, we don't quote.
            sb.append(columnName.contains("(") ? columnName : quote(columnName));
            sb.append(",");
        }
        sb.setLength(sb.length() - 1);
        sb.append(" )");

        sb.append(";");
        return sb.toString();
    }

    @Override
    public Set<String> getSpatialColumns(Connection sourceConn, String tableName, String defaultSchema) {
        String schema = StringUtils.strip(SqlUtil.schema(tableName), "\"");
        String unqualifiedTableName = StringUtils.strip(SqlUtil.notQualified(tableName), "\"");

        if (schema == null) {
            schema = defaultSchema == null ? "public" : defaultSchema;
        }

        HashSet<String> spatialColumns = new HashSet<>();
        try (Statement stmt = sourceConn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM geometry_columns "
                    + " WHERE geometry_columns.f_table_name='"
                    + unqualifiedTableName
                    + "' and f_table_schema = '"
                    + schema
                    + "' ")) {
                while (rs.next()) {
                    spatialColumns.add(rs.getString("f_geometry_column"));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Could not find the spatial columns:" + e.getMessage());
        }

        return spatialColumns;
    }

    @Override
    public List<Column> getColumns(Connection connection, String tableName, ResultSet rs) throws SQLException {
        // this method generates a more precise type description that the default dialect.

        List<Column> result = new ArrayList<Column>();

        Statement stmt = connection.createStatement();

        ResultSet rsMetadata = stmt.executeQuery("select a.attname, a.attnotnull, format_type(a.atttypid, a.atttypmod) "
                + "from pg_attribute a where attrelid = '"
                + quote(tableName)
                + "'::regclass and attnum > 0"
                + " and not attisdropped order by attnum");

        while (rsMetadata.next()) {
            String name = rsMetadata.getString(1);
            Boolean notNull = rsMetadata.getBoolean(2);
            String type = rsMetadata.getString(3);
            result.add(new Column() {

                @Override
                public String getName() throws SQLException {
                    return name;
                }

                @Override
                public String getTypeEtc() throws SQLException {
                    return type + (notNull ? " NOT NULL" : " NULL");
                }
            });
        }

        return result;
    }

    @Override
    public boolean autoUpdateView() {
        return true;
    }

    @Override
    public String getGeometryType(String type, int srid) {
        return "GEOMETRY('" + type + "', " + srid + ")";
    }

    @Override
    public String getConvertedGeometry(GeometryTable.Type type) {
        if (type == GeometryTable.Type.WKT) {
            return "ST_GeomFromText(?, ?)";
        } else if (type == GeometryTable.Type.WKB) {
            return "ST_GeomFromWKB(?, ?)";
        } else if (type == GeometryTable.Type.WKB_HEX) {
            return "ST_GeomFromWKB(decode(?, 'hex'), ?)";
        } else return super.getConvertedGeometry(type);
    }
}
