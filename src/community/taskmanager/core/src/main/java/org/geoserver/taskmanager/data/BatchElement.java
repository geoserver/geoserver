/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

import java.io.Serializable;
import java.util.List;

/**
 * Element of a batch.
 *
 * @author Niels Charlier
 */
public interface BatchElement extends SoftRemove, Serializable, Identifiable {

    Batch getBatch();

    void setBatch(Batch batch);

    Task getTask();

    void setTask(Task task);

    Integer getIndex();

    void setIndex(Integer index);

    List<Run> getRuns();
}
