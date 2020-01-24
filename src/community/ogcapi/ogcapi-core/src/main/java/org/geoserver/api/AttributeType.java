/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.sql.Date;
import org.locationtech.jts.geom.Geometry;

/** Types of attributes, used in {@link Queryable} */
public enum AttributeType {
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

    public static AttributeType fromClass(Class binding) {
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
}
