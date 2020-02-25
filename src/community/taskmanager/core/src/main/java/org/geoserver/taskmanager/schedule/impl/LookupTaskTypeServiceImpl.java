/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule.impl;

import java.util.List;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.LookupServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the task service.
 *
 * @author Niels Charlier
 */
@Service
public class LookupTaskTypeServiceImpl extends LookupServiceImpl<TaskType> {

    @Autowired
    public void setTaskTypes(List<TaskType> taskTypes) {
        setNamed(taskTypes);
    }
}
