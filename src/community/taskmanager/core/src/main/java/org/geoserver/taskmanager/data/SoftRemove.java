/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

/**
 * Objects that are be soft removed are still stored in database for historical reasons, but are
 * generally not displayed.
 *
 * @author Niels Charlier
 */
public interface SoftRemove {

    void setRemoveStamp(long removeStamp);

    long getRemoveStamp();

    default boolean isActive() {
        return getRemoveStamp() == 0;
    }

    default void setActive(boolean active) {
        setRemoveStamp(active ? 0 : System.currentTimeMillis());
    }
}
