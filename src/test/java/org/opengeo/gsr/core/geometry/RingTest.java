/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;

import static org.junit.Assert.*;

import org.junit.Test;

public class RingTest {

    /*
     * A.3.5: geometry/polygon - Verify that all rings have identical start and end points
     */
    @Test
    public void testIsValid() {
        double[] c1 = { 0, 0 };
        double[] c2 = { 0, 1 };
        double[] c3 = { 1, 1 };
        double[] c4 = { 1, 0 };
        double[] c5 = { 1, 2 };

        double[][] coords1 = { c1, c2, c3, c4, c1 };
        double[][] coords2 = { c1, c2, c3, c4, c5 };

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
