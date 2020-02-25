/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskManagerFactoryImpl implements TaskManagerFactory {

    @Override
    public Run createRun() {
        return new RunImpl();
    }

    @Override
    public Task createTask() {
        return new TaskImpl();
    }

    @Override
    public Batch createBatch() {
        return new BatchImpl();
    }

    @Override
    public Configuration createConfiguration() {
        return new ConfigurationImpl();
    }

    @Override
    public Parameter createParameter() {
        return new ParameterImpl();
    }

    @Override
    public Attribute createAttribute() {
        return new AttributeImpl();
    }

    @Override
    public BatchElement createBatchElement() {
        return new BatchElementImpl();
    }

    @Override
    public BatchRun createBatchRun() {
        return new BatchRunImpl();
    }
}
