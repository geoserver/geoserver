/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

/**
 * Factory for DAO objects.
 *
 * @author Niels Charlier
 */
public interface TaskManagerFactory {

    Task createTask();

    Batch createBatch();

    Run createRun();

    Configuration createConfiguration();

    Parameter createParameter();

    Attribute createAttribute();

    BatchElement createBatchElement();

    BatchRun createBatchRun();
}
