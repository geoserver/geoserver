/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geotools.geometry.jts.WKTReader2;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class UpdateElementHandlerTest {

    @Test
    public void testCheckConsistentGeometryDimensions() throws Exception {
        WKTReader2 wktReader = new WKTReader2();

        Geometry g = wktReader.read("POINT(0 0)");
        assertTrue(UpdateElementHandler.checkConsistentGeometryDimensions(g, Point.class));
        assertTrue(UpdateElementHandler.checkConsistentGeometryDimensions(g, MultiPoint.class));
        assertFalse(UpdateElementHandler.checkConsistentGeometryDimensions(g, LineString.class));
        assertFalse(UpdateElementHandler.checkConsistentGeometryDimensions(g, Polygon.class));

        g = wktReader.read("LINESTRING(0 0, 1 1)");
        assertFalse(UpdateElementHandler.checkConsistentGeometryDimensions(g, Point.class));
        assertTrue(UpdateElementHandler.checkConsistentGeometryDimensions(g, LineString.class));
        assertFalse(UpdateElementHandler.checkConsistentGeometryDimensions(g, Polygon.class));

        g = wktReader.read("POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))");
        assertFalse(UpdateElementHandler.checkConsistentGeometryDimensions(g, Point.class));
        assertFalse(UpdateElementHandler.checkConsistentGeometryDimensions(g, LineString.class));
        assertTrue(UpdateElementHandler.checkConsistentGeometryDimensions(g, Polygon.class));
    }
}
