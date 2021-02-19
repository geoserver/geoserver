/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.sql.Date;
import org.locationtech.jts.geom.Geometry;

/** Types of attributes, used in {@link Queryable} */
public enum AttributeType {
    STRING("string"),
    URI("uri"),
    ENUMERATION("enum"),
    NUMBER("number"),
    INTEGER("integer"),
    DATE("date"),
    DATE_TIME("dateTime"),
    BOOL("boolean"),
    GEOMETRY("geometry");

    String type;

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
                return AttributeType.NUMBER;
            } else {
                return AttributeType.INTEGER;
            }
        } else if (Date.class.isAssignableFrom(binding)) {
            return AttributeType.DATE;
        } else if (java.util.Date.class.isAssignableFrom(binding)) {
            return AttributeType.DATE_TIME;
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return AttributeType.BOOL;
        } else if (Geometry.class.isAssignableFrom(binding)) {
            return AttributeType.GEOMETRY;
        } else {
            // fallback
            return AttributeType.STRING;
        }
    }
}
