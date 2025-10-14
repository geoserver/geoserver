/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.enums.EnumConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * XStream converter for enums that returns a default value when the provided value cannot be matched.
 *
 * <p>This is useful to handle enum values that have been removed, or when the enum class has been renamed and we want
 * to provide a default value instead of failing the whole unmarshalling process.
 */
public final class EnumWithDefaultConverter extends EnumConverter {

    private final Enum<?> defaultValue;

    public EnumWithDefaultConverter(Enum<?> defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Class<?> type = context.getRequiredType();
        if (type.getSuperclass() != Enum.class) {
            type = type.getSuperclass();
        }
        String name = reader.getValue();

        try {
            return Enum.valueOf((Class) type, name);
        } catch (IllegalArgumentException ignore) {
            for (Enum<?> c : (Enum<?>[]) (type).getEnumConstants()) {
                if (c.name().equalsIgnoreCase(name)) {
                    return c;
                }
            }
            return defaultValue;
        }
    }
}
