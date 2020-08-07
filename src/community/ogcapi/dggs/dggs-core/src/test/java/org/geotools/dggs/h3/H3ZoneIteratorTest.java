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

import static java.util.function.Function.identity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.uber.h3core.H3Core;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;

public class H3ZoneIteratorTest {

    H3Core h3;

    @Before
    public void createH3() throws IOException {
        this.h3 = H3Core.newInstance();
    }

    private List<Long> collectZones(H3ZoneIterator<Long> iterator) {
        List<Long> zoneIds = new ArrayList<>();
        iterator.forEachRemaining(id -> zoneIds.add(id));
        return zoneIds;
    }

    @Test
    public void testRootZones() {
        H3ZoneIterator<Long> iterator =
                new H3ZoneIterator<>(h3, id -> false, id -> true, identity());
        List<Long> zoneIds = collectZones(iterator);
        assertEquals(122, zoneIds.size());
        assertTrue(zoneIds.containsAll(h3.getRes0Indexes()));
    }

    @Test(expected = NoSuchElementException.class)
    public void testEmpty() {
        H3ZoneIterator<Long> iterator =
                new H3ZoneIterator<>(h3, id -> false, id -> false, identity());
        assertFalse(iterator.hasNext());
        // make it throw a no such element
        iterator.next();
    }

    @Test
    public void testDrillDown() {
        long center = h3.geoToH3(0, 0, 0);
        // gets the res = 2 children of the center cell
        H3ZoneIterator<Long> iterator =
                new H3ZoneIterator<>(
                        h3,
                        id1 -> h3.h3GetResolution(id1) < 2,
                        id -> h3.h3GetResolution(id) == 2,
                        id -> id,
                        Arrays.asList(center));
        List<Long> zones = collectZones(iterator);
        List<Long> l3Children = h3.h3ToChildren(center, 2);
        assertEquals(l3Children.size(), zones.size());
        assertTrue(zones.containsAll(l3Children));
    }
}
