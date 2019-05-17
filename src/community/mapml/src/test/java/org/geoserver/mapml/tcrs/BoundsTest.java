/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import static org.junit.Assert.*;

import org.junit.Test;

/** @author peter.rushforth@canada.ca */
public class BoundsTest {

    public BoundsTest() {}

    @Test
    public void testBounds() {
        Point p1 = new Point(0, 0);
        Point p2 = new Point(500, -7);
        Bounds b = new Bounds(p2, p1);
        assertTrue(b.min.x == 0);
        assertTrue(b.min.y == -7);
        assertTrue(b.max.x == 500);
        assertTrue(b.max.y == 0);
        b = new Bounds(p1, p2);
        assertTrue(b.min.x == 0);
        assertTrue(b.min.y == -7);
        assertTrue(b.max.x == 500);
        assertTrue(b.max.y == 0);
    }

    @Test
    public void testContains() {
        Bounds b = new Bounds(new Point(768, 768), new Point(1024, 1280));
        Bounds inside = new Bounds(new Point(769, 769), new Point(1023, 1279));

        assertTrue(b.contains(inside));

        Bounds touchingAlongRightSide = new Bounds(new Point(1024, 768), new Point(1280, 1280));
        assertFalse(b.contains(touchingAlongRightSide));

        Bounds overlappingToTheRight = new Bounds(new Point(1000, 768), new Point(1280, 1280));
        assertFalse(b.contains(overlappingToTheRight));

        Bounds overlappingAbove = new Bounds(new Point(768, 500), new Point(1024, 1000));
        assertFalse(b.contains(overlappingAbove));

        Bounds touchingAbove = new Bounds(new Point(768, 500), new Point(1024, 768));
        assertFalse(b.contains(touchingAbove));

        Bounds completelyEnclosing = new Bounds(new Point(0, 0), new Point(1280, 2000));
        assertFalse(b.contains(completelyEnclosing));

        // 495&ymin=855&xmax=1633&ymax=1255&zoom=0&projection=CBMTILE
        Bounds observedErrorBounds = new Bounds(new Point(495, 855), new Point(1633, 1255));
        assertFalse(b.contains(observedErrorBounds));

        Bounds touchingButInside = new Bounds(new Point(769, 769), new Point(1024, 1280));
        assertTrue(b.contains(touchingButInside));
    }
}
