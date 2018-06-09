/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

import java.io.Serializable;
import java.util.List;

/**
 * A batch runs a number of tasks in a specific order.
 *
 * @author Niels Charlier
 */
public interface Batch extends SoftRemove, Serializable, Identifiable {

    public static final String FULL_NAME_DIVISOR = ":";

    List<BatchElement> getElements();

    String getFrequency();

    void setFrequency(String frequentie);

    String getName();

    void setName(String name);

    void setEnabled(boolean enabled);

    boolean isEnabled();

    String getDescription();

    void setDescription(String description);

    String getWorkspace();

    void setWorkspace(String workspace);

    Configuration getConfiguration();

    void setConfiguration(Configuration configuration);

    List<BatchRun> getBatchRuns();

    default String getFullName() {
        return getConfiguration() == null
                ? getName()
                : ((getConfiguration().getName() == null ? "" : getConfiguration().getName())
                        + FULL_NAME_DIVISOR
                        + getName());
    }

    BatchRun getLatestBatchRun();
}
