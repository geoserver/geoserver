/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import javax.annotation.PostConstruct;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.springframework.stereotype.Component;

@Component
public class CreateViewTaskTypeImpl extends AbstractCreateViewTaskTypeImpl {

    public static final String NAME = "CreateView";

    public static final String PARAM_TABLE_NAME = "table-name";

    public static final String PARAM_SELECT = "select-clause";

    public static final String PARAM_WHERE = "where-clause";

    @Override
    @PostConstruct
    public void initParamInfo() {
        super.initParamInfo();
        paramInfo.put(
                PARAM_TABLE_NAME,
                new ParameterInfo(PARAM_TABLE_NAME, extTypes.tableName, true)
                        .dependsOn(paramInfo.get(PARAM_DB_NAME)));
        paramInfo.put(PARAM_SELECT, new ParameterInfo(PARAM_SELECT, ParameterType.SQL, true));
        paramInfo.put(PARAM_WHERE, new ParameterInfo(PARAM_WHERE, ParameterType.SQL, false));
    }

    public String buildQueryDefinition(TaskContext ctx, BatchContext.Dependency dependency)
            throws TaskException {
        final DbTable table =
                (DbTable)
                        ctx.getBatchContext()
                                .get(ctx.getParameterValues().get(PARAM_TABLE_NAME), dependency);
        final String select = (String) ctx.getParameterValues().get(PARAM_SELECT);
        final String where = (String) ctx.getParameterValues().get(PARAM_WHERE);
        StringBuilder sb =
                new StringBuilder("SELECT ")
                        .append(select)
                        .append(" FROM ")
                        .append(table.getTableName());
        if (where != null) {
            sb.append(" WHERE ").append(where);
        }
        return sb.toString();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
