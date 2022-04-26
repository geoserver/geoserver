/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.opengis.feature.Feature;

/** Event associated with OSEO data store changes */
public class OseoEvent {
    private OseoEventType type;
    private String collectionName;

    private Feature collectionBefore;

    private Feature collectionAfter;

    public OseoEventType getType() {
        return type;
    }

    public void setType(OseoEventType type) {
        this.type = type;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public Feature getCollectionBefore() {
        return collectionBefore;
    }

    public void setCollectionBefore(Feature collectionBefore) {
        this.collectionBefore = collectionBefore;
    }

    public Feature getCollectionAfter() {
        return collectionAfter;
    }

    public void setCollectionAfter(Feature collectionAfter) {
        this.collectionAfter = collectionAfter;
    }
}
