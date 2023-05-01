/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geotools.util.Range;

/**
 * A Range that implements Comparable to allow the WMTS module to work with ranges since it always
 * expects to deal with comparators in order to sort the dimension values.
 *
 * @param <T>
 */
class ComparableRange<T extends Comparable<T>> extends Range<T>
        implements Comparable<ComparableRange> {

    ComparableRange(Class<T> elementClass, T value) {
        super(elementClass, value);
    }

    ComparableRange(Class<T> elementClass, T minValue, T maxValue) {
        super(elementClass, minValue, maxValue);
    }

    ComparableRange(
            Class<T> elementClass,
            T minValue,
            boolean isMinIncluded,
            T maxValue,
            boolean isMaxIncluded) {
        super(elementClass, minValue, isMinIncluded, maxValue, isMaxIncluded);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(ComparableRange range) {
        int result = getMinValue().compareTo((T) range.getMinValue());
        if (result == 0) result = getMaxValue().compareTo((T) range.getMaxValue());
        return result;
    }
}
