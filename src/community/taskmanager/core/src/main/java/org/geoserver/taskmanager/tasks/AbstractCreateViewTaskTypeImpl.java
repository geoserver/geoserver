/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.impl.DbTableImpl;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractCreateViewTaskTypeImpl implements TaskType {

    public static final String PARAM_DB_NAME = "database";

    public static final String PARAM_VIEW_NAME = "view-name";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    protected static final Logger LOGGER = Logging.getLogger(AbstractCreateViewTaskTypeImpl.class);

    @Autowired protected ExtTypes extTypes;

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_DB_NAME, new ParameterInfo(PARAM_DB_NAME, extTypes.dbName, true));
        paramInfo.put(
                PARAM_VIEW_NAME, new ParameterInfo(PARAM_VIEW_NAME, ParameterType.STRING, true));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
        final String viewName = (String) ctx.getParameterValues().get(PARAM_VIEW_NAME);
        final String tempViewName =
                SqlUtil.qualified(
                        SqlUtil.schema(viewName),
                        "_temp_" + UUID.randomUUID().toString().replace('-', '_'));
        ctx.getBatchContext().put(new DbTableImpl(db, viewName), new DbTableImpl(db, tempViewName));

        final String definition =
                buildQueryDefinition(
                        ctx,
                        db.getDialect().autoUpdateView()
                                ? null
                                : new BatchContext.Dependency() {
                                    @Override
                                    public void revert() throws TaskException {
                                        final String definition = buildQueryDefinition(ctx, null);
                                        try (Connection conn = db.getDataSource().getConnection()) {
                                            try (Statement stmt = conn.createStatement()) {
                                                StringBuilder sb =
                                                        new StringBuilder("DROP VIEW ")
                                                                .append(viewName)
                                                                .append("; CREATE VIEW ")
                                                                .append(viewName)
                                                                .append(" AS ")
                                                                .append(definition);
                                                LOGGER.log(
                                                        Level.FINE,
                                                        "replacing temporary View: "
                                                                + sb.toString());
                                                stmt.executeUpdate(sb.toString());
                                            }
                                        } catch (SQLException e) {
                                            throw new TaskException(e);
                                        }
                                    }
                                });

        try (Connection conn = db.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {

                String sqlCreateSchemaIfNotExists =
                        db.getDialect().createSchema(conn, SqlUtil.schema(tempViewName));

                StringBuilder sb = new StringBuilder(sqlCreateSchemaIfNotExists);
                sb.append("CREATE VIEW ").append(tempViewName).append(" AS ").append(definition);
                LOGGER.log(Level.FINE, "creating temporary View: " + sb.toString());
                stmt.executeUpdate(sb.toString());
            }
        } catch (SQLException e) {
            throw new TaskException(e);
        }

        return new TaskResult() {
            @Override
            public void commit() throws TaskException {
                try (Connection conn = db.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        LOGGER.log(Level.FINE, "committing view: " + viewName);
                        String viewNameQuoted = db.getDialect().quote(viewName);
                        stmt.executeUpdate("DROP VIEW IF EXISTS " + viewNameQuoted);

                        stmt.executeUpdate(
                                db.getDialect()
                                        .sqlRenameView(
                                                tempViewName,
                                                db.getDialect()
                                                        .quote(SqlUtil.notQualified(viewName))));

                        ctx.getBatchContext().delete(new DbTableImpl(db, viewName));

                        LOGGER.log(Level.FINE, "committed view: " + viewName);
                    }
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }

            @Override
            public void rollback() throws TaskException {
                try (Connection conn = db.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        LOGGER.log(Level.FINE, "rolling back view: " + viewName);
                        stmt.executeUpdate("DROP VIEW " + tempViewName);
                        LOGGER.log(Level.FINE, "rolled back view: " + viewName);
                    }
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }
        };
    }

    public abstract String buildQueryDefinition(TaskContext ctx, BatchContext.Dependency dependency)
            throws TaskException;

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
        final String viewName = (String) ctx.getParameterValues().get(PARAM_VIEW_NAME);
        try (Connection conn = db.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP VIEW IF EXISTS " + db.getDialect().quote(viewName));
            }
        } catch (SQLException e) {
            throw new TaskException(e);
        }
    }
}
