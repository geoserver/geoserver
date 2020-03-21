/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

import static org.junit.Assert.*;

import org.junit.Test;

public class RingTest {

    /*
     * A.3.5: geometry/polygon - Verify that all rings have identical start and end points
     */
    @Test
    public void testIsValid() {
        Double[] c1 = { 0.0, 0.0 };
        Double[] c2 = { 0.0, 1.0 };
        Double[] c3 = { 1.0, 1.0 };
        Double[] c4 = { 1.0, 0.0 };
        Double[] c5 = { 1.0, 2.0 };

        Double[][] coords1 = { c1, c2, c3, c4, c1 };
        Double[][] coords2 = { c1, c2, c3, c4, c5 };

        Ring validRing = new Ring(coords1);
        Ring invalidRing = new Ring(coords2);

        assertEquals(true, validRing.isValid());
        assertEquals(false, invalidRing.isValid());
    }

    /*
     * A.3.5: geometry/polygon - Verify that the orientation of the first ring is clockwise and that the orientation of all other rings is
     * counterclockwise
     */

//    @Test
//    public void testIsValidOrientation() {
//        fail("Not yet implemented");
//    }
}
