/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import com.fasterxml.jackson.annotation.JsonValue;

public class StyleAttribute {

    enum AttributeType {
        string,
        uri,
        enumeration("enum"),
        number,
        integer,
        date,
        dateTime,
        bool("boolean");

        String type;

        AttributeType() {}

        AttributeType(String type) {
            this.type = type;
        }

        @JsonValue
        public String getType() {
            if (type != null) {
                return type;
            } else {
                return name();
            }
        }
    }

    String id;
    AttributeType type;

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
