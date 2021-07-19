/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

/** Base interface for listeners that can handle event issues by {@link TemplateInfoDAO} */
public interface TemplateDAOListener {

    /**
     * Handle a TemplateInfo delete event.
     *
     * @param deleteEvent the delete event.
     */
    void handleDeleteEvent(TemplateInfoEvent deleteEvent);

    /**
     * Handle a TemplateInfo update event.
     *
     * @param updateEvent
     */
    void handleUpdateEvent(TemplateInfoEvent updateEvent);
}
