/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.impl.DbTableImpl;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The copy table task type.
 *
 * @author Niels Charlier
 * @author Timothy De Bock
 */
@Component
public class CopyTableTaskTypeImpl implements TaskType {

    public static final String NAME = "CopyTable";

    public static final String PARAM_SOURCE_DB_NAME = "source-database";

    public static final String PARAM_TARGET_DB_NAME = "target-database";

    public static final String PARAM_TABLE_NAME = "table-name";

    public static final String PARAM_TARGET_TABLE_NAME = "target-table-name";

    public static final String GENERATE_ID_COLUMN_NAME = "generated_id";

    private static final Logger LOGGER = Logging.getLogger(CopyTableTaskTypeImpl.class);

    private static final int BATCH_SIZE = 1000;

    @Autowired protected ExtTypes extTypes;

    private final Map<String, ParameterInfo> paramInfo = new LinkedHashMap<String, ParameterInfo>();

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(
                PARAM_SOURCE_DB_NAME,
                new ParameterInfo(PARAM_SOURCE_DB_NAME, extTypes.dbName, true));
        paramInfo.put(
                PARAM_TARGET_DB_NAME,
                new ParameterInfo(PARAM_TARGET_DB_NAME, extTypes.dbName, true));
        paramInfo.put(
                PARAM_TABLE_NAME,
                new ParameterInfo(PARAM_TABLE_NAME, extTypes.tableName, true)
                        .dependsOn(paramInfo.get(PARAM_SOURCE_DB_NAME)));
        paramInfo.put(
                PARAM_TARGET_TABLE_NAME,
                new ParameterInfo(PARAM_TARGET_TABLE_NAME, extTypes.tableName, false)
                        .dependsOn(paramInfo.get(PARAM_TARGET_DB_NAME)));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        // TODO: check for ctx.isInterruptMe() in loops and cancel task

        final DbSource sourcedb = (DbSource) ctx.getParameterValues().get(PARAM_SOURCE_DB_NAME);
        final DbSource targetdb = (DbSource) ctx.getParameterValues().get(PARAM_TARGET_DB_NAME);
        final DbTable table =
                (DbTable) ctx.getBatchContext().get(ctx.getParameterValues().get(PARAM_TABLE_NAME));

        final DbTable targetTable =
                ctx.getParameterValues().containsKey(PARAM_TARGET_TABLE_NAME)
                        ? (DbTable) ctx.getParameterValues().get(PARAM_TARGET_TABLE_NAME)
                        : new DbTableImpl(targetdb, table.getTableName());
        final String tempTableName =
                SqlUtil.qualified(
                        SqlUtil.schema(targetTable.getTableName()),
                        "_temp_" + UUID.randomUUID().toString().replace('-', '_'));
        ctx.getBatchContext().put(targetTable, new DbTableImpl(targetdb, tempTableName));

        try (Connection sourceConn = sourcedb.getDataSource().getConnection()) {
            sourceConn.setAutoCommit(false);
            try (Connection destConn = targetdb.getDataSource().getConnection()) {
                try (Statement stmt = sourceConn.createStatement()) {
                    stmt.setFetchSize(BATCH_SIZE);
                    try (ResultSet rs =
                            stmt.executeQuery(
                                    "SELECT * FROM "
                                            + sourcedb.getDialect().quote(table.getTableName()))) {

                        ResultSetMetaData rsmd = rs.getMetaData();

                        String tempSchema = SqlUtil.schema(tempTableName);
                        String sqlCreateSchemaIfNotExists =
                                tempSchema == null
                                        ? ""
                                        : targetdb.getDialect()
                                                .createSchema(
                                                        destConn,
                                                        targetdb.getDialect().quote(tempSchema));

                        // create the temp table structure
                        StringBuilder sb = new StringBuilder(sqlCreateSchemaIfNotExists);
                        sb.append("CREATE TABLE ")
                                .append(targetdb.getDialect().quote(tempTableName))
                                .append(" ( ");
                        int columnCount = rsmd.getColumnCount();

                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = targetdb.getDialect().quote(rsmd.getColumnLabel(i));
                            String typeName = rsmd.getColumnTypeName(i);
                            sb.append(columnName).append(" ").append(typeName);
                            if (("char".equals(typeName) || "varchar".equals(typeName))
                                    && rsmd.getColumnDisplaySize(i) > 0
                                    && rsmd.getColumnDisplaySize(i) < Integer.MAX_VALUE) {
                                sb.append(" (").append(rsmd.getColumnDisplaySize(i)).append(" ) ");
                            }
                            switch (sourcedb.getDialect().isNullable(rsmd.isNullable(i))) {
                                case ResultSetMetaData.columnNoNulls:
                                    sb.append(" NOT NULL");
                                    break;
                                case ResultSetMetaData.columnNullable:
                                    sb.append(" NULL");
                                    break;
                            }
                            sb.append(", ");
                        }
                        String primaryKey = getPrimaryKey(sourceConn, table.getTableName());
                        boolean hasPrimaryKeyColumn = !primaryKey.isEmpty();
                        if (!hasPrimaryKeyColumn) {
                            // create a Primary key column if none exist.
                            sb.append(GENERATE_ID_COLUMN_NAME + " int PRIMARY KEY, ");
                            columnCount++;
                        }

                        sb.setLength(sb.length() - 2);
                        sb.append(" ); ");

                        // creating indexes
                        Map<String, Set<String>> indexAndColumnMap =
                                getIndexesColumns(sourceConn, table.getTableName());
                        Set<String> uniqueIndexes =
                                getUniqueIndexes(sourceConn, table.getTableName());
                        Set<String> spatialColumns =
                                sourcedb.getDialect()
                                        .getSpatialColumns(
                                                sourceConn,
                                                table.getTableName(),
                                                sourcedb.getSchema());

                        for (String indexName : indexAndColumnMap.keySet()) {
                            Set<String> columnNames = indexAndColumnMap.get(indexName);
                            boolean isSpatialIndex =
                                    columnNames.size() == 1
                                            && spatialColumns.contains(
                                                    columnNames.iterator().next());

                            sb.append(
                                    targetdb.getDialect()
                                            .createIndex(
                                                    tempTableName,
                                                    columnNames,
                                                    isSpatialIndex,
                                                    uniqueIndexes.contains(indexName)));
                        }
                        // we are copying a view and need to create the spatial index.
                        if (indexAndColumnMap.isEmpty() && !spatialColumns.isEmpty()) {
                            sb.append(
                                    targetdb.getDialect()
                                            .createIndex(
                                                    tempTableName, spatialColumns, true, false));
                        }

                        String dump = sb.toString();
                        LOGGER.log(Level.FINE, "creating temporary table: " + dump);

                        try (Statement stmt2 = destConn.createStatement()) {
                            stmt2.executeUpdate(dump);
                        }

                        // copy the data
                        sb =
                                new StringBuilder("INSERT INTO ")
                                        .append(targetdb.getDialect().quote(tempTableName))
                                        .append(" VALUES (");
                        for (int i = 0; i < columnCount; i++) {
                            if (i > 0) {
                                sb.append(",");
                            }
                            sb.append("?");
                        }
                        sb.append(")");

                        LOGGER.log(Level.FINE, "inserting records: " + sb.toString());

                        try (PreparedStatement pstmt = destConn.prepareStatement(sb.toString())) {
                            int batchSize = 0;
                            int primaryKeyValue = 0;
                            while (rs.next()) {
                                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                                    pstmt.setObject(i, rs.getObject(i));
                                }
                                // generate the primary key value
                                if (!hasPrimaryKeyColumn) {
                                    pstmt.setObject(columnCount, primaryKeyValue);
                                }
                                pstmt.addBatch();
                                batchSize++;
                                if (batchSize >= BATCH_SIZE) {
                                    pstmt.executeBatch();
                                    batchSize = 0;
                                }
                                primaryKeyValue++;
                            }
                            if (batchSize > 0) {
                                pstmt.executeBatch();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // clean-up if necessary
            try (Connection conn = targetdb.getDataSource().getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "DROP TABLE IF EXISTS " + targetdb.getDialect().quote(tempTableName));
                }
            } catch (SQLException e2) {
            }

            throw new TaskException(e);
        }

        return new TaskResult() {
            @Override
            public void commit() throws TaskException {
                try (Connection conn = targetdb.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(
                                "DROP TABLE IF EXISTS "
                                        + targetdb.getDialect().quote(targetTable.getTableName()));
                        stmt.executeUpdate(
                                "ALTER TABLE "
                                        + targetdb.getDialect().quote(tempTableName)
                                        + " RENAME TO "
                                        + targetdb.getDialect()
                                                .quote(
                                                        SqlUtil.notQualified(
                                                                targetTable.getTableName())));
                    }

                    ctx.getBatchContext().delete(targetTable);
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }

            @Override
            public void rollback() throws TaskException {
                try (Connection conn = targetdb.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(
                                "DROP TABLE " + targetdb.getDialect().quote(tempTableName) + "");
                    }
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final DbTable table = (DbTable) ctx.getParameterValues().get(PARAM_TABLE_NAME);
        final DbSource targetDb = (DbSource) ctx.getParameterValues().get(PARAM_TARGET_DB_NAME);
        final DbTable targetTable =
                ctx.getParameterValues().containsKey(PARAM_TARGET_TABLE_NAME)
                        ? (DbTable) ctx.getParameterValues().get(PARAM_TARGET_TABLE_NAME)
                        : new DbTableImpl(targetDb, table.getTableName());

        try (Connection conn = targetDb.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                        "DROP TABLE IF EXISTS "
                                + targetDb.getDialect().quote(targetTable.getTableName()));
            }
        } catch (SQLException e) {
            throw new TaskException(e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private static String getTableName(Connection conn, String tableName) throws SQLException {
        String name = StringUtils.strip(SqlUtil.notQualified(tableName));
        if (conn.getMetaData().storesUpperCaseIdentifiers()) {
            name = name.toUpperCase();
        } else if (conn.getMetaData().storesUpperCaseIdentifiers()) {
            name = name.toLowerCase();
        }
        return name;
    }

    private static String getSchema(Connection conn, String tableName) throws SQLException {
        String schema = StringUtils.strip(SqlUtil.schema(tableName), "\"");
        if (conn.getMetaData().storesUpperCaseIdentifiers()) {
            schema = schema.toUpperCase();
        } else if (conn.getMetaData().storesUpperCaseIdentifiers()) {
            schema = schema.toLowerCase();
        }
        return schema;
    }

    private static String getPrimaryKey(Connection conn, String tableName) throws SQLException {
        String schema = getSchema(conn, tableName);
        String name = getTableName(conn, tableName);

        try (ResultSet rsPrimaryKeys = conn.getMetaData().getPrimaryKeys(null, schema, name)) {
            StringBuilder sb = new StringBuilder();
            while (rsPrimaryKeys.next()) {
                sb.append(rsPrimaryKeys.getString("COLUMN_NAME")).append(", ");
            }
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            // if there is no primary key column defined. Check if there is a generated key column
            // available
            if (sb.length() < 2) {
                ResultSet rsColumns = conn.getMetaData().getColumns(null, schema, name, null);
                while (rsColumns.next()) {
                    if (GENERATE_ID_COLUMN_NAME.equalsIgnoreCase(
                            rsColumns.getString("COLUMN_NAME"))) {
                        return GENERATE_ID_COLUMN_NAME;
                    }
                }
            }

            return sb.toString();
        }
    }

    private Set<String> getUniqueIndexes(Connection conn, String tableName) throws SQLException {
        String schema = getSchema(conn, tableName);
        String name = getTableName(conn, tableName);

        Set<String> result = new HashSet<String>();

        try (ResultSet rs = conn.getMetaData().getIndexInfo(null, schema, name, true, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                result.add(indexName);
            }
        }
        return result;
    }

    private Map<String, Set<String>> getIndexesColumns(Connection conn, String tableName)
            throws SQLException {
        String schema = getSchema(conn, tableName);
        String name = getTableName(conn, tableName);

        HashMap<String, Set<String>> result = new HashMap<>();

        try (ResultSet rs = conn.getMetaData().getIndexInfo(null, schema, name, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String dbColumnName = rs.getString("COLUMN_NAME");
                if (!result.containsKey(indexName)) {
                    result.put(indexName, new HashSet<>());
                }
                result.get(indexName).add(dbColumnName);
            }
        }
        return result;
    }
}
