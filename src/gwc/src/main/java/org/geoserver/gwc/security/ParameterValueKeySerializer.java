/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import org.geotools.api.parameter.ParameterValue;

/**
 * Extension point for serializing the value inside a {@link ParameterValue} to a stable cache key fragment.
 *
 * <p>Register custom implementations as Spring beans. {@link AccessLimitsKeyBuilder} collects them all at startup and
 * dispatches by value type via {@link Class#isAssignableFrom}. Duplicate registrations for the same value type fail at
 * startup with a descriptive error.
 *
 * @param <T> the type of the value inside the {@link ParameterValue} (e.g. {@code Filter}, {@code Geometry})
 */
public interface ParameterValueKeySerializer<T> {

    /** Value type this serializer handles. Matched via {@link Class#isAssignableFrom}. */
    Class<T> getValueType();

    /**
     * Serializes {@code value} to a stable string fragment for inclusion in the cache key. Must be deterministic:
     * identical inputs -> identical output; inputs producing different tile content -> different output.
     *
     * <p>Default implementation returns {@code value.toString()}, sufficient for types whose {@code toString()} is
     * already stable and unambiguous (e.g. {@link String}, {@link Boolean}, {@link Number}).
     */
    default String toKey(T value) {
        return value.toString();
    }
}
