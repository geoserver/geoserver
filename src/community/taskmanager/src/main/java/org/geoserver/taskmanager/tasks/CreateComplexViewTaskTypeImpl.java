/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.DbTableImpl;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
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
        paramInfo.put(PARAM_DEFINITION, new ParameterInfo(PARAM_DEFINITION, ParameterType.SQL, true));
    }
    
    public String buildQueryDefinition(Map<String, Object> parameterValues,
            Map<Object, Object> tempValues, Map<String, Attribute> attributes) {
        String definition = (String) parameterValues.get(PARAM_DEFINITION);
        
        Matcher m = PATTERN_PLACEHOLDER.matcher(definition);

        while (m.find()) {
            Attribute attribute = attributes.get(m.group(1));
            if (attribute != null && attribute.getValue() != null) {
                Object o;
                if (tempValues.containsKey(attribute.getValue())) {
                    o = tempValues.get(attribute.getValue());
                } else {
                    //check if it is a table in the temp map
                    final DbSource db = (DbSource) parameterValues.get(PARAM_DB_NAME);
                    final DbTable table = new DbTableImpl(db, attribute.getValue());
                    if (tempValues.containsKey(table)) {
                        o = tempValues.get(table);
                    } else {
                        //just use the plain value
                        o = attribute.getValue();
                    }
                }
                definition = m.replaceFirst(
                        o instanceof DbTable ? ((DbTable) o).getTableName() : o.toString());
                m = PATTERN_PLACEHOLDER.matcher(definition);
            } else {
                //TODO should we trow an error here?
                LOGGER.severe("Attribute not found for placeholder:" + m.group(1));
            }
        }

        return definition;
    }

    @Override
    public String getName() {
        return NAME;
    }

}
