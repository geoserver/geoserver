/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;

/** Types of attributes, used in {@link Queryable} */
public enum AttributeType {
    STRING("string"),
    URI("uri"),
    ENUMERATION("enum"),
    NUMBER("number"),
    INTEGER("integer"),
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

    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS =
            new ImmutableMap.Builder<Class<?>, Class<?>>()
                    .put(boolean.class, Boolean.class)
                    .put(byte.class, Byte.class)
                    .put(char.class, Character.class)
                    .put(double.class, Double.class)
                    .put(float.class, Float.class)
                    .put(int.class, Integer.class)
                    .put(long.class, Long.class)
                    .put(short.class, Short.class)
                    .put(void.class, Void.class)
                    .build();

    /**
     * Returns an AttributeType for the given class. Always returns a value, in case there is no
     * better match {@link AttributeType#STRING} is returned.
     */
    public static AttributeType fromClass(Class<?> binding) {
        // some funtions fail to declare their return type, assume the most generic
        if (binding == null) return AttributeType.STRING;

        // some functions use primitive return types, go to the wrapper
        if (PRIMITIVES_TO_WRAPPERS.containsKey(binding)) {
            binding = PRIMITIVES_TO_WRAPPERS.get(binding);
        }
        if (Number.class.isAssignableFrom(binding)) {
            if (Float.class.isAssignableFrom(binding)
                    || Double.class.isAssignableFrom(binding)
                    || BigDecimal.class.isAssignableFrom(binding)) {
                return AttributeType.NUMBER;
            } else {
                return AttributeType.INTEGER;
            }
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return AttributeType.BOOL;
        } else if (Geometry.class.isAssignableFrom(binding)
                || org.opengis.geometry.Geometry.class.isAssignableFrom(binding)) {
            return AttributeType.GEOMETRY;
        } else {
            // fallback
            return AttributeType.STRING;
        }
    }
}
