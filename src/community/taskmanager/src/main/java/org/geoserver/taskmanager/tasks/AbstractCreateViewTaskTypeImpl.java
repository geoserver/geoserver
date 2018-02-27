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

import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTableImpl;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractCreateViewTaskTypeImpl implements TaskType {
    
    public static final String PARAM_DB_NAME = "database";

    public static final String PARAM_VIEW_NAME = "view-name";

    protected final Map<String, ParameterInfo> paramInfo = new LinkedHashMap<String, ParameterInfo>();

    protected static final Logger LOGGER = Logging.getLogger(AbstractCreateViewTaskTypeImpl.class);

    @Autowired
    protected ExtTypes extTypes;
    
    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_DB_NAME, new ParameterInfo(PARAM_DB_NAME, extTypes.dbName, true));
        paramInfo.put(PARAM_VIEW_NAME, new ParameterInfo(PARAM_VIEW_NAME, ParameterType.STRING, true));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(Batch batch, Task task, Map<String, Object> parameterValues,
            Map<Object, Object> tempValues) throws TaskException {
        final DbSource db = (DbSource) parameterValues.get(PARAM_DB_NAME);
        final String viewName = (String) parameterValues.get(PARAM_VIEW_NAME);
        final String tempViewName = SqlUtil.qualified(SqlUtil.schema(viewName),
                "_temp_" + UUID.randomUUID().toString().replace('-', '_'));
        tempValues.put(new DbTableImpl(db, viewName), new DbTableImpl(db, tempViewName));
        
        try (Connection conn = db.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()){
                StringBuilder sb = new StringBuilder("CREATE VIEW ")
                        .append(tempViewName).append(" AS ")
                        .append(buildQueryDefinition(parameterValues, tempValues, 
                                task.getConfiguration().getAttributes()));
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
                    try (Statement stmt = conn.createStatement()){
                        LOGGER.log(Level.FINE, "commiting view: " + viewName);
                        stmt.executeUpdate("DROP VIEW IF EXISTS " + db.getDialect().quote(viewName));

                        String viewNameQuoted = db.getDialect().quote(SqlUtil.notQualified(viewName));
                        stmt.executeUpdate(db.getDialect().sqlRenameView(tempViewName, viewNameQuoted));

                        LOGGER.log(Level.FINE, "committed view: " + viewName);
                    }
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }

            @Override
            public void rollback() throws TaskException {
                try (Connection conn = db.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()){
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
    
    public abstract String buildQueryDefinition(Map<String, Object> parameterValues,
            Map<Object, Object> tempValues, Map<String, Attribute> attributes) ;

    @Override
    public void cleanup(Task task, Map<String, Object> parameterValues)
            throws TaskException {
        final DbSource db = (DbSource) parameterValues.get(PARAM_DB_NAME);
        final String viewName = (String) parameterValues.get(PARAM_VIEW_NAME);
        try (Connection conn = db.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()){
                stmt.executeUpdate("DROP VIEW IF EXISTS " + db.getDialect().quote(viewName));
            }
        } catch (SQLException e) {
            throw new TaskException(e);
        }
    }

}
