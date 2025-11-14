/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

import java.util.ArrayList;
import java.util.List;

public class Relationships {
    private List<Relationship> relationships = new ArrayList<>();

    public Relationships() {}

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
    }

    public void addRelationship(Relationship relationship) {
        this.relationships.add(relationship);
    }
}
