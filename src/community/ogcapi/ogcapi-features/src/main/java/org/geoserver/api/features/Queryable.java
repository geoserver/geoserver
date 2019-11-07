/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.sql.Date;
import org.locationtech.jts.geom.Geometry;

public class Queryable {

    enum AttributeType {
        string,
        uri,
        enumeration("enum"),
        number,
        integer,
        date,
        dateTime,
        bool("boolean"),
        geometry;

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

        @Override
        public String toString() {
            return getType();
        }
    }

    String id;
    AttributeType type;

    public Queryable(String id, AttributeType type) {
        this.id = id;
        this.type = type;
    }

    public Queryable(String id, Class binding) {
        this.id = id;
        this.type = getAttributeType(binding);
    }

    private AttributeType getAttributeType(Class<?> binding) {
        if (Number.class.isAssignableFrom(binding)) {
            if (Float.class.isAssignableFrom(binding)
                    || Double.class.isAssignableFrom(binding)
                    || BigDecimal.class.isAssignableFrom(binding)) {
                return AttributeType.number;
            } else {
                return AttributeType.integer;
            }
        } else if (Date.class.isAssignableFrom(binding)) {
            return AttributeType.date;
        } else if (java.util.Date.class.isAssignableFrom(binding)) {
            return AttributeType.dateTime;
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return AttributeType.bool;
        } else if (Geometry.class.isAssignableFrom(binding)) {
            return AttributeType.geometry;
        } else {
            // fallback
            return AttributeType.string;
        }
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
