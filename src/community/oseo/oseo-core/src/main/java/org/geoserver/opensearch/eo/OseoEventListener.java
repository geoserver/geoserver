/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

/** Listener for OSEO Data Store Events */
public interface OseoEventListener {
    /**
     * Fires in response to OSEO Data Store Event
     *
     * @param event OSEO data store event
     */
    void dataStoreChange(OseoEvent event);
}
