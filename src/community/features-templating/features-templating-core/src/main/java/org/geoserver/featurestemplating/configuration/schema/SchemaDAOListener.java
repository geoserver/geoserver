/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

/** Base interface for listeners that can handle event issues by {@link SchemaInfoDAO} */
public interface SchemaDAOListener {

    /**
     * Handle a SchemaInfo delete event.
     *
     * @param deleteEvent the delete event.
     */
    void handleDeleteEvent(SchemaInfoEvent deleteEvent);

    /**
     * Handle a SchemaInfo update event.
     *
     * @param updateEvent
     */
    void handleUpdateEvent(SchemaInfoEvent updateEvent);
}
