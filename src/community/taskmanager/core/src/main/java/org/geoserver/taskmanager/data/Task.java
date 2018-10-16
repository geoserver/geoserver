/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A task.
 *
 * @author Niels Charlier
 */
public interface Task extends SoftRemove, Serializable, Identifiable {

    public static final String FULL_NAME_DIVISOR = "/";

    String getType();

    void setType(String type);

    Map<String, Parameter> getParameters();

    List<BatchElement> getBatchElements();

    Configuration getConfiguration();

    void setConfiguration(Configuration configuration);

    String getName();

    void setName(String name);

    default String getFullName() {
        return (getConfiguration().getName() == null ? "" : getConfiguration().getName())
                + FULL_NAME_DIVISOR
                + getName();
    }
}
