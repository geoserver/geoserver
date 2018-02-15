/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import java.util.Map;

import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.util.Named;

/**
 * A Task Type.
 * 
 * @author Niels Charlier
 *
 */
public interface TaskType extends Named {
    
    /**
     * Return parameter info for this task type.
     * 
     * @return the parameter info
     */
    public Map<String, ParameterInfo> getParameterInfo();
    
    /**
     * Run a task, based on these parameter values.
     * @param batch TODO
     * @param task TODO
     * @param parameterValues
     * 
     * @return the task result
     */
    public TaskResult run(Batch batch, Task task, Map<String, Object> parameterValues,
            Map<Object, Object> tempValues) throws TaskException;
    
    /**
     * Do a clean-up for this task (for example, if this task publishes something, remove it).
     * @param parameterValues the parameter values for this task
     * 
     * @throws TaskException 
     */
    public void cleanup(Task task, Map<String, Object> parameterValues) 
            throws TaskException;

}
