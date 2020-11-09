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
package org.geotools.dggs.rhealpix;

import static org.geotools.dggs.rhealpix.RHealPixUtils.setCellId;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
class RHealPixZoneIterator<R> implements Iterator<R> {

    private final RHealPixDGGSInstance rpix;
    Function<RHealPixZone, Boolean> drill;
    Function<RHealPixZone, Boolean> accept;
    Function<RHealPixZone, R> map;
    Stack<String> candidates = new Stack<>();
    R next = null;

    public RHealPixZoneIterator(
            RHealPixDGGSInstance rpix,
            Function<RHealPixZone, Boolean> drill,
            Function<RHealPixZone, Boolean> accept,
            Function<RHealPixZone, R> map) {
        this(
                rpix,
                drill,
                accept,
                map,
                (List<String>) rpix.runtime.runSafe(si -> si.getValue("dggs.cells0", List.class)));
    }

    public RHealPixZoneIterator(
            RHealPixDGGSInstance rpix,
            Function<RHealPixZone, Boolean> drill,
            Function<RHealPixZone, Boolean> accept,
            Function<RHealPixZone, R> map,
            Collection<String> rootZones) {
        this.rpix = rpix;
        this.drill = drill;
        this.accept = accept;
        this.map = map;
        this.candidates.addAll(rootZones);
    }

    @Override
    public boolean hasNext() {
        // look for the next item, or drill down through children
        while (next == null && !candidates.isEmpty()) {
            String test = candidates.pop();
            RHealPixZone zone = rpix.getZone(test);
            if (accept.apply(zone)) {
                next = map.apply(zone);
            } else {
                if (drill.apply(zone)) {
                    candidates.addAll(
                            0,
                            rpix.runtime.runSafe(
                                    si -> {
                                        setCellId(si, "id", test);
                                        si.exec("c = Cell(dggs, id)");
                                        return si.getValue("list(c.subcells())", List.class);
                                    }));
                }
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
