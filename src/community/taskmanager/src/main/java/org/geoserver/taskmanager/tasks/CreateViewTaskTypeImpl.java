/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
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
        paramInfo.put(PARAM_TABLE_NAME, new ParameterInfo(PARAM_TABLE_NAME, extTypes.tableName(), true)
                .dependsOn(paramInfo.get(PARAM_DB_NAME)));
        paramInfo.put(PARAM_SELECT, new ParameterInfo(PARAM_SELECT, ParameterType.SQL, true));
        paramInfo.put(PARAM_WHERE, new ParameterInfo(PARAM_WHERE, ParameterType.SQL, false));
    }
    
    public String buildQueryDefinition(Map<String, Object> parameterValues,
            Map<Object, Object> tempValues, Map<String, Attribute> attributes) {
        final DbTable table = tempValues.containsKey(parameterValues.get(PARAM_TABLE_NAME)) ?
                (DbTable) tempValues.get(parameterValues.get(PARAM_TABLE_NAME)) :
                (DbTable) parameterValues.get(PARAM_TABLE_NAME);
        final String select = (String) parameterValues.get(PARAM_SELECT);
        final String where = (String) parameterValues.get(PARAM_WHERE);
        StringBuilder sb = new StringBuilder("SELECT ").
            append(select).append(" FROM ").append(table.getTableName());
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
