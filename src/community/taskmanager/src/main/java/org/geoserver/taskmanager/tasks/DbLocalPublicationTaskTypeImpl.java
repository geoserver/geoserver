package org.geoserver.taskmanager.tasks;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.springframework.beans.factory.annotation.Autowired;

public class DbLocalPublicationTaskTypeImpl implements TaskType {
    
    public static final String NAME = "DbLocalPublication";
    
    public static final String PARAM_LAYER = "layer";
    
    public static final String PARAM_DB_NAME = "database";

    public static final String PARAM_TABLE_NAME = "table-name";
    
    protected final Map<String, ParameterInfo> paramInfo = new LinkedHashMap<String, ParameterInfo>();

    @Autowired
    protected ExtTypes extTypes;
    
    @Override
    public String getName() {
        return NAME;
    }

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_LAYER, new ParameterInfo(PARAM_LAYER, extTypes.internalLayer, true));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(Batch batch, Task task, Map<String, Object> parameterValues,
            Map<Object, Object> tempValues) throws TaskException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cleanup(Task task, Map<String, Object> parameterValues) throws TaskException {
        
    }

}
