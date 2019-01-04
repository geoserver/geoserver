/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.impl.DbTableImpl;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.springframework.stereotype.Component;

@Component
public class CreateComplexViewTaskTypeImpl extends AbstractCreateViewTaskTypeImpl {

    public static final String NAME = "CreateComplexView";

    public static final String PARAM_DEFINITION = "definition";

    private static final Pattern PATTERN_PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    @PostConstruct
    public void initParamInfo() {
        super.initParamInfo();
        paramInfo.put(
                PARAM_DEFINITION, new ParameterInfo(PARAM_DEFINITION, ParameterType.SQL, true));
    }

    public String buildQueryDefinition(TaskContext ctx, BatchContext.Dependency dependency)
            throws TaskException {
        String definition = (String) ctx.getParameterValues().get(PARAM_DEFINITION);

        Matcher m = PATTERN_PLACEHOLDER.matcher(definition);

        while (m.find()) {
            Attribute attribute = ctx.getTask().getConfiguration().getAttributes().get(m.group(1));
            if (attribute != null && attribute.getValue() != null) {
                String value = attribute.getValue();
                Object o = ctx.getBatchContext().get(value, dependency);
                if (o == value) {
                    // check if it is a table in the batch context
                    final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
                    final DbTable table = new DbTableImpl(db, value);
                    Object t = ctx.getBatchContext().get(table, dependency);
                    if (t != table) {
                        o = t;
                    }
                }
                definition =
                        m.replaceFirst(
                                o instanceof DbTable ? ((DbTable) o).getTableName() : o.toString());
                m = PATTERN_PLACEHOLDER.matcher(definition);
            } else {
                // TODO: should already happen in validation
                throw new TaskException("Attribute not found for placeholder:" + m.group(1));
            }
        }

        return definition;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
