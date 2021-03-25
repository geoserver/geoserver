/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.h3;

import com.uber.h3core.H3Core;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Function;

/**
 * Iterator walking zones, from root to leaves, based on two support functions:
 *
 * <ul>
 *   <li>One to decide if to return a zone
 *   <li>One to decide if to drill down further on it, exploring its children
 * </ul>
 */
class H3ZoneIterator<R> implements Iterator<R> {

    private final H3Core h3;
    Function<Long, Boolean> drill;
    Function<Long, Boolean> accept;
    Function<Long, R> map;
    Stack<Long> candidates = new Stack<>();
    R next = null;

    public H3ZoneIterator(
            H3Core h3,
            Function<Long, Boolean> drill,
            Function<Long, Boolean> accept,
            Function<Long, R> map) {
        this(h3, drill, accept, map, h3.getRes0Indexes());
    }

    public H3ZoneIterator(
            H3Core h3,
            Function<Long, Boolean> drill,
            Function<Long, Boolean> accept,
            Function<Long, R> map,
            Collection<Long> rootZones) {
        this.h3 = h3;
        this.drill = drill;
        this.accept = accept;
        this.map = map;
        this.candidates.addAll(rootZones);
    }

    @Override
    public boolean hasNext() {
        // look for the next item, or drill down through children
        while (next == null && !candidates.isEmpty()) {
            Long test = candidates.pop();
            if (accept.apply(test)) {
                next = map.apply(test);
            }
            if (drill.apply(test)) {
                candidates.addAll(h3.h3ToChildren(test, h3.h3GetResolution(test) + 1));
            }
        }

        return next != null;
    }

    @Override
    public R next() {
        if (!hasNext()) throw new NoSuchElementException();
        R result = next;
        next = null;
        return result;
    }
}
