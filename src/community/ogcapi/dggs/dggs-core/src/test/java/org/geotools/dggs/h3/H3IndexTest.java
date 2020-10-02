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

import static org.junit.Assert.assertEquals;

import com.uber.h3core.H3Core;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class H3IndexTest {

    H3Core h3;

    @Before
    public void setup() throws IOException {
        h3 = H3Core.newInstance();
    }

    @Test
    public void testGetResolution() {
        long id = h3.stringToH3("8075fffffffffff");
        int r = h3.h3GetResolution(id);

        while (true) {
            assertEquals(r, new H3Index(id).getResolution());
            r++;
            if (r <= 15) {
                id = h3.h3ToChildren(id, r).get(0);
            } else {
                break;
            }
        }
    }

    @Test
    public void testGetLowestIdChildren() throws Exception {
        long id = h3.stringToH3("8075fffffffffff");
        long lowestId = new H3Index(id).lowestIdChild(15);
        // check using h3 library, navigate down to the lowest children
        long childId = id;
        int r = 1;
        while (r <= 15) {
            childId = h3.h3ToChildren(childId, r).get(0);
            r++;
        }
        assertEquals(
                Long.toBinaryString(childId) + "\n" + Long.toBinaryString(lowestId),
                childId,
                lowestId);
    }
}
