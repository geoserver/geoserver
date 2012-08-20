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

        double[][] coords1 = new double[5][2];
        coords1[0] = c1;
        coords1[1] = c2;
        coords1[2] = c3;
        coords1[3] = c4;
        coords1[4] = c1;

        double[][] coords2 = new double[5][2];
        coords2[0] = c1;
        coords2[1] = c2;
        coords2[2] = c3;
        coords2[3] = c4;
        coords2[4] = c5;

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
