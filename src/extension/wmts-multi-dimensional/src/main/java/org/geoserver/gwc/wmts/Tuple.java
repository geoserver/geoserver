/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.util.Objects;

/** Utility class for return two values. */
public final class Tuple<T, U> {

    public final T first;
    public final U second;

    private Tuple(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public static <R, S> Tuple<R, S> tuple(R first, S second) {
        return new Tuple<>(first, second);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) object;
        return Objects.equals(first, tuple.first) && Objects.equals(second, tuple.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
