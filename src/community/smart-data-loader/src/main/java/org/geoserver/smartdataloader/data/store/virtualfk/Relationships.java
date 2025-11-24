/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

import java.util.ArrayList;
import java.util.List;

/** Container for the list of virtual relationships configured for a store. */
public class Relationships {
    private List<Relationship> relationships = new ArrayList<>();

    public Relationships() {}

    /** Returns the mutable list of configured relationships (never {@code null}). */
    public List<Relationship> getRelationships() {
        return relationships;
    }

    /** Replaces the current collection of relationships (null is treated as empty). */
    public void setRelationships(List<Relationship> relationships) {
        this.relationships = (relationships == null) ? new ArrayList<>() : relationships;
    }

    /**
     * Adds a relationship definition to the current collection.
     *
     * @param relationship relationship to add (ignored when {@code null})
     */
    public void addRelationship(Relationship relationship) {
        if (relationship != null) {
            this.relationships.add(relationship);
        }
    }
}
