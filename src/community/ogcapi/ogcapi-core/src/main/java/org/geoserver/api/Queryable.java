/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

/** A queryable describes an attribute that can be queried by the filter extension */
public class Queryable {

    String id;
    AttributeType type;

    public Queryable(String id, AttributeType type) {
        this.id = id;
        this.type = type;
    }

    public Queryable(String id, Class binding) {
        this.id = id;
        this.type = AttributeType.fromClass(binding);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }
}
