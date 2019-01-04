/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

import java.io.Serializable;
import java.util.Map;

/**
 * A configuration exists of tasks which are somehow related to each other and share attributes
 * which are used as task parameters.
 *
 * @author Niels Charlier
 */
public interface Configuration extends SoftRemove, Serializable, Identifiable {

    boolean isTemplate();

    void setTemplate(boolean template);

    String getWorkspace();

    void setWorkspace(String workspace);

    Map<String, Attribute> getAttributes();

    Map<String, Task> getTasks();

    Map<String, Batch> getBatches();

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String name);

    boolean isValidated();

    void setValidated(boolean initMode);
}
