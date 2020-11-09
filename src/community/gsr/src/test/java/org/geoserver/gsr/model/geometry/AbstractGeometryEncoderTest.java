/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.geometry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.gsr.translate.geometry.GeometryEncoder;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class AbstractGeometryEncoderTest {

    WKTReader reader = new WKTReader();

    @Test
    public void testRingOrientation() throws ParseException {
        // a polygon with flipped ring orientations, shell is CCW and hole is CW
        Geometry geometry =
                reader.read(
                        "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0), (1 1, 1 2, 2 2, 2" + " 1, 1 1))");

        // to ESRI format
        org.geoserver.gsr.model.geometry.Geometry esriGeometry =
                new GeometryEncoder().toRepresentation(geometry, null);
        assertThat(esriGeometry, CoreMatchers.instanceOf(Polygon.class));

        Polygon polygon = (Polygon) esriGeometry;
        Number[][][] rings = polygon.getRings();
        // shell
        assertArrayEquals(new Number[] {0d, 0d}, rings[0][0]);
        assertArrayEquals(new Number[] {0d, 10d}, rings[0][1]);
        assertArrayEquals(new Number[] {10d, 10d}, rings[0][2]);
        assertArrayEquals(new Number[] {10d, 0d}, rings[0][3]);
        assertArrayEquals(new Number[] {0d, 0d}, rings[0][4]);
        // hole
        assertArrayEquals(new Number[] {1d, 1d}, rings[1][0]);
        assertArrayEquals(new Number[] {2d, 1d}, rings[1][1]);
        assertArrayEquals(new Number[] {2d, 2d}, rings[1][2]);
        assertArrayEquals(new Number[] {1d, 2d}, rings[1][3]);
        assertArrayEquals(new Number[] {1d, 1d}, rings[1][4]);
    }

    @Test
    public void testRoundTripPolyHoles() throws ParseException {
        Geometry geometry =
                reader.read(
                        "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0), (1 1, 2 1, 2 2, 1"
                                + " 2, "
                                + "1 1), (4 4, 5 4, 5 5, 5 5, 4 4))");

        // to ESRI format
        org.geoserver.gsr.model.geometry.Geometry esriGeometry =
                new GeometryEncoder().toRepresentation(geometry, null);
        assertThat(esriGeometry, CoreMatchers.instanceOf(Polygon.class));

        // and back (will not have the same layout due to ring orientations)
        Geometry back = GeometryEncoder.toJts(esriGeometry);
        assertTrue(geometry.equalsTopo(back));
    }

    @Test
    public void testNestedHoles() throws ParseException {
        Geometry geometry =
                reader.read(
                        "MULTIPOLYGON(((0 0, 10 0, 10 10, 0 10, 0 0), (1 1, 9 1, 9 9, 1"
                                + " 9, "
                                + "1 1)), ((2 2, 8 2, 8 8, 2 8, 2 2), (3 3, 7 3, 7 7, 3 7, 3 3)))");

        // to ESRI format
        org.geoserver.gsr.model.geometry.Geometry esriGeometry =
                new GeometryEncoder().toRepresentation(geometry, null);
        assertThat(esriGeometry, CoreMatchers.instanceOf(Polygon.class));

        // and back (will not have the same layout due to ring orientations)
        Geometry back = GeometryEncoder.toJts(esriGeometry);
        assertTrue(geometry.equalsTopo(back));
    }
}
