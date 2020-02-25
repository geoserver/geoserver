/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

import java.io.Serializable;

/**
 * Parameter of a task.
 *
 * @author Niels Charlier
 */
public interface Parameter extends Serializable, Identifiable {

    String getName();

    void setName(String name);

    String getValue();

    void setValue(String value);

    Task getTask();

    void setTask(Task task);
}
