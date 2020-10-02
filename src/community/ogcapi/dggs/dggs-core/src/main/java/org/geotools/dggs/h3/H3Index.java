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

import java.util.BitSet;

/**
 * Support class for the manipulation of H3 indexes. Refer to this page for the interpretation of
 * the binary content of a H3 index: https://h3geo.org/docs/core-library/h3indexing
 */
class H3Index {

    private final long id;

    public H3Index(long id) {
        this.id = id;
    }

    public int getResolution() {
        int shifted = (int) (id >>> (64 - 12));
        return shifted & 0xF;
    }

    public long lowestIdChild(int targetResolution) {
        int resolution = getResolution();
        if (resolution > 14) throw new IllegalArgumentException("This H3 cell has no children");
        if (resolution > 15)
            throw new IllegalArgumentException("Maximum allowed resolution value is 15");
        // switch the resolution up
        long childId =
                (id & (8l << (64 - 8)))
                        | (((long) targetResolution) << (64 - 12))
                        | (id & 0xFFFFFFFFFFFFFL);
        //        long mask = ((long) 0xFFFFE) << (64 - 20);
        //        childId &= mask;
        //        return childId;

        BitSet bitSet = toBitSet(childId);
        int base = 19 + resolution * 3;
        for (int i = base; i < 64; i++) {
            bitSet.set(i, false);
        }

        return fromBitSet(bitSet);
    }

    public long highestIdChild(int targetResolution) {
        int resolution = getResolution();
        if (resolution > 14) throw new IllegalArgumentException("This H3 cell has no children");
        // switch the resolution up
        long childId =
                (id & (8l << (64 - 8)))
                        | (((long) targetResolution) << (64 - 12))
                        | (id & 0xFFFFFFFFFFFFFL);
        return childId;
    }

    public BitSet toBitSet(long value) {
        BitSet bits = new BitSet(64);
        int index = 63;
        while (value != 0L) {
            if (value % 2L != 0) {
                bits.set(index);
            }
            index--;
            value = value >>> 1;
        }
        return bits;
    }

    public long fromBitSet(BitSet bits) {
        long result = 0L;
        for (int i = 63; i >= 0; i--) {
            result += bits.get(i) ? (1L << (63 - i)) : 0L;
        }
        return result;
    }
}
