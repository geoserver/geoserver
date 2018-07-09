/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;

import java.awt.Point;
import java.awt.geom.Point2D;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

public class QuickTileCacheTest {
    QuickTileCache cache = new QuickTileCache();

    @Test
    public void testMetaCoordinates() {
        Point orig = new Point(0, 0);
        assertEquals(orig, cache.getMetaTileCoordinates(orig));

        Point t10 = new Point(1, 0);
        assertEquals(orig, cache.getMetaTileCoordinates(t10));

        Point t01 = new Point(1, 0);
        assertEquals(orig, cache.getMetaTileCoordinates(t01));

        Point t33 = new Point(3, 3);
        assertEquals(new Point(3, 3), cache.getMetaTileCoordinates(t33));

        Point tm1m1 = new Point(-1, -1);
        assertEquals(new Point(-3, -3), cache.getMetaTileCoordinates(tm1m1));

        Point tm3m3 = new Point(-3, -3);
        assertEquals(new Point(-3, -3), cache.getMetaTileCoordinates(tm3m3));

        Point tm4m4 = new Point(-4, -4);
        assertEquals(new Point(-6, -6), cache.getMetaTileCoordinates(tm4m4));

        Point t4m4 = new Point(4, -4);
        assertEquals(new Point(3, -6), cache.getMetaTileCoordinates(t4m4));

        Point tm44 = new Point(-4, 4);
        assertEquals(new Point(-6, 3), cache.getMetaTileCoordinates(tm44));
    }

    @Test
    public void testTileCoordinatesNaturalOrigin() {
        Point2D origin = new Point2D.Double(0, 0);
        Envelope env = new Envelope(30, 60, 30, 60);
        Point tc = cache.getTileCoordinates(env, origin);
        assertEquals(new Point(1, 1), tc);

        env = new Envelope(-30, 0, -30, 0);
        tc = cache.getTileCoordinates(env, origin);
        assertEquals(new Point(-1, -1), tc);
    }

    @Test
    public void testInnerTileOffsets() {
        Envelope meta =
                new Envelope(1215736.8585492, 1215744.0245205, 5455471.361398601, 5455478.5273699);
        Envelope box1 =
                new Envelope(1215736.8585492, 1215739.2472063, 5455476.1387128, 5455478.5273699);
        Envelope box2 =
                new Envelope(1215739.2472063, 1215741.6358635, 5455476.1387128, 5455478.5273699);
        assertEquals(new Point(0, 2), cache.getTileOffsetsInMeta(box1, meta));
        assertEquals(new Point(1, 2), cache.getTileOffsetsInMeta(box2, meta));
    }
}
