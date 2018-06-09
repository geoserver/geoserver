/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule.impl;

import java.util.Map;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Task Context Implementation
 *
 * @author Niels Charlier
 */
@Component
@Scope("prototype")
public class TaskContextImpl implements TaskContext {

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerTaskUtil taskUtil;

    private Task task;

    private BatchContext batchContext;

    private Map<String, Object> parameterValues;

    public TaskContextImpl(Task task) {
        this.task = task;
    }

    public TaskContextImpl(Task task, BatchContext batchContext) {
        this.task = task;
        this.batchContext = batchContext;
    }

    @Override
    public Task getTask() {
        return task;
    }

    @Override
    public Map<String, Object> getParameterValues() throws TaskException {
        if (parameterValues == null) {
            parameterValues = taskUtil.getParameterValues(task);
        }
        return parameterValues;
    }

    @Override
    public boolean isInterruptMe() {
        return batchContext != null && dao.reload(batchContext.getBatchRun()).isInterruptMe();
    }

    @Override
    public BatchContext getBatchContext() {
        return batchContext;
    }
}
